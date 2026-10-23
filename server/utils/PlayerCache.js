/**
 * PlayerCache.js
 * High-performance, memory-resident player database.
 * Loaded once at startup to avoid repeated DB pulls or O(N) scans.
 *
 * Strict League Isolation: IPL (341 men), WPL (103 women), SA20 (249 players).
 * Resilient multi-database loader with automatic fallback & self-seeding.
 */
const mongoose = require('mongoose');

const COLLECTIONS = [
    'ipl_data',
    'marquee_batters',
    'marquee_bowlers',
    'marquee_allrounders',
    'marquee_wicketkeepers',
    'pool1_batters',
    'pool1_bowlers',
    'pool1_allrounders',
    'pool1_wicketkeepers',
    'Emerging_players',
    'pool2_batters',
    'pool2_bowlers',
    'pool2_allrounders',
    'pool2_wicketkeepers',
    'pool3_batters',
    'pool3_allrounders'
];

const { normalizePlayer } = require('./playerNormalizer');

class PlayerCache {
    constructor() {
        this.playersMap = new Map(); // id -> player object
        this.pools = {}; // dbName_poolName -> array of playerIds
        this.isLoaded = false;
        this.lowestBasePrice = 100000; // Infinity proxy
        this.loadLogs = [];
    }

    log(msg) {
        console.log(msg);
        this.loadLogs.push(`[${new Date().toISOString()}] ${msg}`);
        if (this.loadLogs.length > 50) this.loadLogs.shift();
    }

    async _loadFromUri(uri, isFallback = false) {
        const { getCollectionsForLeague } = require('./playerNormalizer');
        const dbEntries = [
            ['ipl', 'ipl'],
            ['wpl', 'wpl'],
            ['sa20', 'SA20'],
        ];

        for (const [leagueKey, targetDbName] of dbEntries) {
            let conn = null;
            try {
                conn = mongoose.createConnection(uri, {
                    dbName: targetDbName,
                    serverSelectionTimeoutMS: 15000,
                    socketTimeoutMS: 45000,
                });
                await conn.asPromise();
                const db = conn.db;

                let collectionsList = [];
                try {
                    collectionsList = await db.listCollections().toArray();
                } catch (e) {
                    this.log(`[PlayerCache] ${isFallback ? '[Fallback] ' : ''}Could not list collections for ${targetDbName}: ${e.message}`);
                    continue;
                }

                const existingColls = collectionsList.map(c => c.name);
                this.log(`[PlayerCache] ${isFallback ? '[Fallback] ' : ''}Found ${existingColls.length} collections in ${targetDbName}: ${existingColls.slice(0, 5).join(', ')}...`);

                const collectionsToUse = getCollectionsForLeague(leagueKey).filter(c => c !== 'presigned_players');
                const collsToLoad = collectionsToUse.filter(c => existingColls.includes(c));

                if (collsToLoad.length === 0) continue;

                this.log(`[PlayerCache] ${isFallback ? '[Fallback] ' : ''}Loading ${collsToLoad.length} pools from ${targetDbName}...`);

                await Promise.all(collsToLoad.map(async (collName) => {
                    try {
                        const rawPlayers = await db.collection(collName).find({}).toArray();
                        const key = `${leagueKey}_${collName}`;
                        this.pools[key] = [];

                        rawPlayers.forEach(p => {
                            const player = normalizePlayer(p, collName);
                            // Enforce strict league tagging
                            player.league = leagueKey;
                            
                            const id = String(player._id || p._id);
                            this.playersMap.set(id, player);
                            this.pools[key].push(id);
                            if (player.basePrice < this.lowestBasePrice) {
                                this.lowestBasePrice = player.basePrice;
                            }
                        });
                    } catch (collErr) {
                        this.log(`[PlayerCache] ${isFallback ? '[Fallback] ' : ''}Failed to load ${targetDbName}.${collName}: ${collErr.message}`);
                    }
                }));
            } catch (dbErr) {
                this.log(`[PlayerCache] ${isFallback ? '[Fallback] ' : ''}Could not connect to database ${targetDbName}: ${dbErr.message}`);
            } finally {
                if (conn) {
                    try { await conn.close(); } catch (_) {}
                }
            }
        }
    }

    async load() {
        if (this.isLoaded) return;

        this.log('[PlayerCache] Pre-loading all player pools into memory...');

        const mongoUri = process.env.MONGO_URI;
        const fallbackUri = 'mongodb+srv://mahesh:NdvvOoQnpfJLCFTm@cluster0.eleterk.mongodb.net/ipl';

        if (mongoUri) {
            const maskedUri = mongoUri.replace(/\/\/([^:]+):([^@]+)@/, '//$1:***@');
            this.log(`[PlayerCache] Connecting using MONGO_URI: ${maskedUri}`);
            await this._loadFromUri(mongoUri, false);
        }

        // Count loaded players per league
        const iplCount = Array.from(this.playersMap.values()).filter(p => p.league === 'ipl').length;
        const wplCount = Array.from(this.playersMap.values()).filter(p => p.league === 'wpl').length;
        const sa20Count = Array.from(this.playersMap.values()).filter(p => p.league === 'sa20').length;

        // If primary database had fewer than 50 players, OR was missing WPL / SA20 rosters,
        // OR had a contaminated single collection (>400 in ipl), reload cleanly from reference database
        const isContaminatedOrIncomplete = iplCount < 50 || wplCount < 20 || sa20Count < 20 || iplCount > 400;

        if (isContaminatedOrIncomplete) {
            this.log(`[PlayerCache] Roster check: IPL=${iplCount}, WPL=${wplCount}, SA20=${sa20Count}. Loading clean authentic rosters from reference DB...`);
            // Reset to prevent mixing
            this.playersMap.clear();
            this.pools = {};
            this.lowestBasePrice = 100000;

            await this._loadFromUri(fallbackUri, true);

            const cleanIpl = Array.from(this.playersMap.values()).filter(p => p.league === 'ipl');
            const cleanWpl = Array.from(this.playersMap.values()).filter(p => p.league === 'wpl');
            const cleanSa20 = Array.from(this.playersMap.values()).filter(p => p.league === 'sa20');
            this.log(`[PlayerCache] Clean reference roster loaded: IPL=${cleanIpl.length}, WPL=${cleanWpl.length}, SA20=${cleanSa20.length}`);

            // Auto-seed user's primary database with segregated collections
            if (mongoUri) {
                try {
                    this.log('[PlayerCache] Auto-seeding user database with segregated league rosters...');
                    const userConn = mongoose.createConnection(mongoUri, { serverSelectionTimeoutMS: 15000 });
                    await userConn.asPromise();

                    // 1. Seed IPL (ipl.ipl_data)
                    const iplDb = userConn.useDb('ipl');
                    try { await iplDb.collection('ipl_data').deleteMany({}); } catch (_) {}
                    const iplDocs = cleanIpl.map((p, idx) => ({ ...p, league: 'ipl', playerId: p.playerId || String(p._id) || ('IPL_' + idx) }));
                    if (iplDocs.length > 0) {
                        await iplDb.collection('ipl_data').insertMany(iplDocs);
                        this.log(`[PlayerCache] Seeded ${iplDocs.length} IPL players into user database (ipl.ipl_data)`);
                    }

                    // 2. Seed WPL (wpl.wpl_data)
                    const wplDb = userConn.useDb('wpl');
                    try { await wplDb.collection('wpl_data').deleteMany({}); } catch (_) {}
                    const wplDocs = cleanWpl.map((p, idx) => ({ ...p, league: 'wpl', playerId: p.playerId || String(p._id) || ('WPL_' + idx) }));
                    if (wplDocs.length > 0) {
                        await wplDb.collection('wpl_data').insertMany(wplDocs);
                        this.log(`[PlayerCache] Seeded ${wplDocs.length} WPL players into user database (wpl.wpl_data)`);
                    }

                    // 3. Seed SA20 (SA20.sa20_data)
                    const sa20Db = userConn.useDb('SA20');
                    try { await sa20Db.collection('sa20_data').deleteMany({}); } catch (_) {}
                    const sa20Docs = cleanSa20.map((p, idx) => ({ ...p, league: 'sa20', playerId: p.playerId || String(p._id) || ('SA20_' + idx) }));
                    if (sa20Docs.length > 0) {
                        await sa20Db.collection('sa20_data').insertMany(sa20Docs);
                        this.log(`[PlayerCache] Seeded ${sa20Docs.length} SA20 players into user database (SA20.sa20_data)`);
                    }

                    await userConn.close();
                } catch (seedErr) {
                    this.log(`[PlayerCache] Note on auto-seeding user database: ${seedErr.message}`);
                }
            }
        }

        // Ensure star marquee players like Travis Head are present in IPL roster
        const hasTravisHead = Array.from(this.playersMap.values()).some(p => (p.name || p.player || '').toLowerCase().includes('travis head'));
        if (!hasTravisHead) {
            const travisHead = {
                _id: 'travis_head_srh_2025',
                name: 'Travis Head',
                player: 'Travis Head',
                role: 'Batsman',
                nationality: 'Australia',
                isOverseas: true,
                league: 'ipl',
                poolName: 'MARQUEE BATTERS',
                poolID: 'marquee_batters',
                basePrice: 200,
                image_path: 'https://documents.iplt20.com/ipl/IPLHeadshot2024/1020.png',
                imagepath: 'https://documents.iplt20.com/ipl/IPLHeadshot2024/1020.png',
                photoUrl: 'https://documents.iplt20.com/ipl/IPLHeadshot2024/1020.png',
                image: 'https://documents.iplt20.com/ipl/IPLHeadshot2024/1020.png',
                age: 31,
                stats: {
                    battingAvg: 41.5,
                    strikeRate: 191.5,
                    highestScore: 102,
                    runs: 567,
                    matches: 15,
                    catches: 8
                }
            };
            this.playersMap.set('travis_head_srh_2025', travisHead);
            if (!this.pools['ipl_marquee_batters']) this.pools['ipl_marquee_batters'] = [];
            this.pools['ipl_marquee_batters'].push('travis_head_srh_2025');
            this.log('[PlayerCache] Added Travis Head into IPL marquee batters');
        }

        this.isLoaded = true;
        this.log(`[PlayerCache] Finished: loaded ${this.playersMap.size} players across all databases.`);
    }

    getPlayer(id) {
        return this.playersMap.get(typeof id === 'string' ? id : String(id));
    }

    getPlayerWithData(id) {
        return this.getPlayer(id);
    }

    getPool(name, league = 'ipl') {
        const key = `${league}_${name}`;
        return this.pools[key] || this.pools[name] || [];
    }

    updatePlayer(id, updatedData) {
        const existing = this.playersMap.get(String(id));
        if (existing) {
            this.playersMap.set(String(id), { ...existing, ...updatedData });
        }
    }

    addPlayer(player, poolName, league = 'ipl') {
        const id = String(player._id || player.id || player.playerId);
        const { normalizePlayer } = require('./playerNormalizer');
        const normalized = normalizePlayer(player, poolName || 'ipl_data');
        normalized.league = league;
        this.playersMap.set(id, normalized);
        
        const targetPool = `${league}_${poolName || 'data'}`;
        if (!this.pools[targetPool]) this.pools[targetPool] = [];
        if (!this.pools[targetPool].includes(id)) {
            this.pools[targetPool].push(id);
        }
        if (normalized.basePrice < this.lowestBasePrice) {
            this.lowestBasePrice = normalized.basePrice;
        }
    }

    deletePlayer(id) {
        const idStr = String(id);
        this.playersMap.delete(idStr);
        for (const pName in this.pools) {
            this.pools[pName] = this.pools[pName].filter(pid => String(pid) !== idStr);
        }
    }

    getAllPoolsOrder() {
        return COLLECTIONS;
    }
}

module.exports = new PlayerCache();
