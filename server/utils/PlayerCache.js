/**
 * PlayerCache.js
 * High-performance, memory-resident player database.
 * Loaded once at startup to avoid repeated DB pulls or O(N) scans.
 *
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

        // If primary database had 0 players, load from fallback registry
        if (this.playersMap.size === 0) {
            this.log('[PlayerCache] Primary database has 0 player records. Loading from reference database...');
            await this._loadFromUri(fallbackUri, true);

            // Auto-seed the user's primary database if connected so it's permanently populated
            if (this.playersMap.size > 0 && mongoUri) {
                try {
                    const userConn = mongoose.createConnection(mongoUri, {
                        dbName: 'ipl',
                        serverSelectionTimeoutMS: 10000
                    });
                    await userConn.asPromise();
                    const iplDataCount = await userConn.db.collection('ipl_data').countDocuments();
                    if (iplDataCount === 0) {
                        this.log('[PlayerCache] Auto-seeding user database ipl_data collection with authentic players...');
                        const allDocs = Array.from(this.playersMap.values());
                        if (allDocs.length > 0) {
                            await userConn.db.collection('ipl_data').insertMany(allDocs);
                            this.log(`[PlayerCache] Successfully seeded ${allDocs.length} players into user database!`);
                        }
                    }
                    await userConn.close();
                } catch (seedErr) {
                    this.log(`[PlayerCache] Note on auto-seeding user database: ${seedErr.message}`);
                }
            }
        }

        this.isLoaded = true;
        this.log(`[PlayerCache] Finished: loaded ${this.playersMap.size} players across all databases.`);
    }

    getPlayer(id) {
        return this.playersMap.get(typeof id === 'string' ? id : String(id));
    }

    /**
     * getPlayerWithData
     * Synchronous lookup for a player by ID.
     */
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
            console.log(`[PlayerCache] Live update applied to memory for player: ${id}`);
        }
    }

    addPlayer(player, poolName) {
        const id = String(player._id || player.id || player.playerId);
        const { normalizePlayer } = require('./playerNormalizer');
        const normalized = normalizePlayer(player, poolName || 'ipl_data');
        this.playersMap.set(id, normalized);
        
        const targetPool = poolName || 'ipl_data';
        if (!this.pools[targetPool]) this.pools[targetPool] = [];
        if (!this.pools[targetPool].includes(id)) {
            this.pools[targetPool].push(id);
        }
        if (normalized.basePrice < this.lowestBasePrice) {
            this.lowestBasePrice = normalized.basePrice;
        }
        console.log(`[PlayerCache] Live add applied to memory for player: ${id} in pool: ${targetPool}`);
    }

    deletePlayer(id, poolName) {
        const idStr = String(id);
        this.playersMap.delete(idStr);
        if (poolName && this.pools[poolName]) {
            this.pools[poolName] = this.pools[poolName].filter(pid => String(pid) !== idStr);
        } else {
            // Remove from all pools
            for (const pName in this.pools) {
                this.pools[pName] = this.pools[pName].filter(pid => String(pid) !== idStr);
            }
        }
        console.log(`[PlayerCache] Live delete applied to memory for player: ${idStr}`);
    }

    getAllPoolsOrder() {
        return COLLECTIONS;
    }
}

module.exports = new PlayerCache();
