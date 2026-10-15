package com.playauction.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// Entities
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Cached snapshot of a room's full state JSON.
 *
 * [stateJson] is the raw Gson-serialised [com.playauction.app.data.model.RoomState]
 * string. Storing it as a blob avoids complex embedded object schemas for a
 * cache table that is only used for offline/reconnect hydration.
 *
 * [timestamp] is the epoch-millis when the cache was last written, used to
 * implement cache invalidation (e.g. stale after 60 seconds).
 */
@Entity(tableName = "cached_rooms")
data class CachedRoom(
    @PrimaryKey
    val roomCode: String,
    val stateJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Persistent record of an auction that has concluded.
 *
 * [teamsJson] is the Gson-serialised `List<Team>` so the results screen can
 * be shown without a network call.
 *
 * [finishedAt] epoch-millis is used for sorting / display.
 */
@Entity(tableName = "auction_history")
data class AuctionHistory(
    @PrimaryKey
    val roomCode: String,
    val league: String,
    val finishedAt: Long,
    val teamsJson: String
)

/**
 * A single row table that holds the current device session.
 *
 * Using [userId] as the primary key means only one session row ever exists;
 * inserting a new session with `REPLACE` strategy cleanly overwrites the old one.
 *
 * [expiresAt] epoch-millis — 0 means "no expiry stored".
 */
@Entity(tableName = "local_session")
data class LocalSession(
    @PrimaryKey
    val userId: String,
    val playerName: String,
    val token: String,
    val expiresAt: Long = 0L
)

// ─────────────────────────────────────────────────────────────────────────────
// DAOs
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CachedRoomDao {

    /** Upsert a room snapshot. Replaces any existing entry with the same [roomCode]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(room: CachedRoom)

    /** Returns the cached snapshot for [roomCode], or `null` if none exists. */
    @Query("SELECT * FROM cached_rooms WHERE roomCode = :roomCode LIMIT 1")
    suspend fun getByCode(roomCode: String): CachedRoom?

    /** Delete cached snapshots older than [cutoffTimestamp] epoch-millis. */
    @Query("DELETE FROM cached_rooms WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    /** Remove a specific cached room (e.g. after a confirmed final result). */
    @Delete
    suspend fun delete(room: CachedRoom)

    /** Nuke the entire cache — useful for logout. */
    @Query("DELETE FROM cached_rooms")
    suspend fun deleteAll()
}

@Dao
interface AuctionHistoryDao {

    /** Upsert a finished-auction record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: AuctionHistory)

    /**
     * Observe all history records ordered newest-first.
     * Returns a [Flow] so the history screen updates reactively when new
     * auctions finish.
     */
    @Query("SELECT * FROM auction_history ORDER BY finishedAt DESC")
    fun observeAll(): Flow<List<AuctionHistory>>

    /** One-shot fetch of all records (for export / share). */
    @Query("SELECT * FROM auction_history ORDER BY finishedAt DESC")
    suspend fun getAll(): List<AuctionHistory>

    /** Fetch a single history record by room code. */
    @Query("SELECT * FROM auction_history WHERE roomCode = :roomCode LIMIT 1")
    suspend fun getByCode(roomCode: String): AuctionHistory?

    /** Delete a single history entry. */
    @Delete
    suspend fun delete(history: AuctionHistory)

    /** Remove all history (user-triggered data wipe). */
    @Query("DELETE FROM auction_history")
    suspend fun deleteAll()
}

@Dao
interface LocalSessionDao {

    /** Insert or replace the current session. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: LocalSession)

    /**
     * Returns the most recently stored session, or `null` if none exists.
     * There should only ever be one row but `LIMIT 1` guards against edge cases.
     */
    @Query("SELECT * FROM local_session LIMIT 1")
    suspend fun get(): LocalSession?

    /** Remove the stored session on logout. */
    @Query("DELETE FROM local_session")
    suspend fun clear()
}

// ─────────────────────────────────────────────────────────────────────────────
// Database
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Single Room database for the PlayAuction app.
 *
 * Version history:
 *  - 1 → initial schema (CachedRoom, AuctionHistory, LocalSession)
 *
 * `exportSchema = true` ensures a schema JSON is written to
 * `app/schemas/` so migrations can be diffed in CI.
 */
@Database(
    entities = [
        CachedRoom::class,
        AuctionHistory::class,
        LocalSession::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cachedRoomDao(): CachedRoomDao
    abstract fun auctionHistoryDao(): AuctionHistoryDao
    abstract fun localSessionDao(): LocalSessionDao
}

// ─────────────────────────────────────────────────────────────────────────────
// Hilt Module
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "play_auction.db"
        )
            // fallbackToDestructiveMigration is intentional for the alpha phase;
            // replace with proper Migration objects before public release.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCachedRoomDao(db: AppDatabase): CachedRoomDao = db.cachedRoomDao()

    @Provides
    fun provideAuctionHistoryDao(db: AppDatabase): AuctionHistoryDao = db.auctionHistoryDao()

    @Provides
    fun provideLocalSessionDao(db: AppDatabase): LocalSessionDao = db.localSessionDao()
}
