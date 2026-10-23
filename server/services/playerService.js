const mongoose = require('mongoose');

/**
 * Unified service to fetch and map players from multiple collections.
 * Handles: 
 * - Serving directly from in-memory PlayerCache (instant and resilient)
 * - Waiting for DB connection as fallback
 * - Aggregating from all league auction collections dynamically
 */
async function fetchAllPlayers(league = 'ipl') {
    const targetLeague = ['ipl', 'wpl', 'sa20'].includes(String(league).toLowerCase()) ? String(league).toLowerCase() : 'ipl';
    const PlayerCache = require('../utils/PlayerCache');
    const { getCollectionsForLeague, normalizePlayer } = require('../utils/playerNormalizer');

    // Primary: Serve from in-memory PlayerCache
    if (PlayerCache.isLoaded && PlayerCache.playersMap.size > 0) {
        try {
            const collections = getCollectionsForLeague(targetLeague).filter(c => c !== 'presigned_players');
            const allPlayers = [];
            for (const collName of collections) {
                const key = `${targetLeague}_${collName}`;
                const ids = PlayerCache.pools[key] || [];
                for (const id of ids) {
                    const player = PlayerCache.playersMap.get(String(id));
                    if (player) allPlayers.push({ ...player });
                }
            }
            if (allPlayers.length > 0) {
                console.log(`[PLAYER_SERVICE] Served ${allPlayers.length} players from PlayerCache for ${targetLeague}`);
                return allPlayers;
            }
        } catch (cacheErr) {
            console.warn('[PLAYER_SERVICE] PlayerCache read failed, falling back to DB:', cacheErr.message);
        }
    }

    // Fallback: Direct DB query
    try {
        if (mongoose.connection.readyState !== 1) {
            console.log("[PLAYER_SERVICE] Waiting for MongoDB connection...");
            await new Promise((resolve) => {
                const timer = setInterval(() => {
                    if (mongoose.connection.readyState === 1) {
                        clearInterval(timer);
                        resolve();
                    }
                }, 100);
            });
        }

        const collections = getCollectionsForLeague(targetLeague);
        let allPlayers = [];
        const db = mongoose.connection.client.db(targetLeague === 'sa20' ? 'SA20' : targetLeague);

        for (const collName of collections) {
            let docs = [];
            try {
                docs = await db.collection(collName).find({}).toArray();
            } catch (e) {
                console.log(`[PLAYER_SERVICE] Collection ${collName} not found in ${targetLeague} DB`);
                continue;
            }

            const mapped = docs.map(doc => normalizePlayer(doc, collName));
            allPlayers = allPlayers.concat(mapped);
        }

        return allPlayers;
    } catch (err) {
        console.error("[PLAYER_SERVICE] Error fetching players:", err);
        return [];
    }
}

module.exports = {
    fetchAllPlayers
};
