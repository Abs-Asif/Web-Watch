package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchers")
data class Watcher(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val intervalMinutes: Int = 15,
    val isActive: Boolean = true,
    val lastCheckedTime: Long? = null,
    val lastHtml: String? = null,
    val previousHtml: String? = null,
    val lastStatus: String = "PENDING", // PENDING, SUCCESS, CHANGED, ERROR
    val lastErrorMessage: String? = null,
    val changeCount: Int = 0,
    val lastAdditions: Int = 0,
    val lastDeletions: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val stripScripts: Boolean = true,
    val formatHtml: Boolean = true
)

@Entity(tableName = "snapshot_history")
data class SnapshotHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val watcherId: Long,
    val watcherName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // INITIAL, CHANGED, UNCHANGED, ERROR
    val additions: Int = 0,
    val deletions: Int = 0,
    val diffSummary: String? = null,
    val oldHtml: String? = null,
    val newHtml: String? = null,
    val errorMessage: String? = null
)
