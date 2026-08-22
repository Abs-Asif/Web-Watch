package com.example.data.dao

import androidx.room.*
import com.example.data.model.SnapshotHistory
import com.example.data.model.Watcher
import kotlinx.coroutines.flow.Flow

@Dao
interface WatcherDao {
    @Query("SELECT * FROM watchers ORDER BY createdAt DESC")
    fun getAllWatchers(): Flow<List<Watcher>>

    @Query("SELECT * FROM watchers WHERE isActive = 1")
    suspend fun getActiveWatchers(): List<Watcher>

    @Query("SELECT * FROM watchers WHERE id = :id")
    suspend fun getWatcherById(id: Long): Watcher?

    @Query("SELECT * FROM watchers WHERE id = :id")
    fun getWatcherByIdFlow(id: Long): Flow<Watcher?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatcher(watcher: Watcher): Long

    @Update
    suspend fun updateWatcher(watcher: Watcher)

    @Delete
    suspend fun deleteWatcher(watcher: Watcher)

    @Query("DELETE FROM watchers WHERE id = :id")
    suspend fun deleteWatcherById(id: Long)

    @Query("UPDATE watchers SET lastStatus = 'SUCCESS' WHERE id = :id AND lastStatus = 'CHANGED'")
    suspend fun markChangesReviewed(id: Long)
}

@Dao
interface SnapshotHistoryDao {
    @Query("SELECT * FROM snapshot_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistory(): Flow<List<SnapshotHistory>>

    @Query("SELECT * FROM snapshot_history WHERE watcherId = :watcherId ORDER BY timestamp DESC")
    fun getHistoryForWatcher(watcherId: Long): Flow<List<SnapshotHistory>>

    @Query("SELECT * FROM snapshot_history WHERE id = :id")
    suspend fun getSnapshotById(id: Long): SnapshotHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotHistory): Long

    @Query("DELETE FROM snapshot_history WHERE watcherId = :watcherId")
    suspend fun deleteHistoryForWatcher(watcherId: Long)

    @Query("DELETE FROM snapshot_history")
    suspend fun clearAllHistory()
}
