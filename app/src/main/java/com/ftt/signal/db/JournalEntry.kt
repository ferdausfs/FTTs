package com.ftt.signal.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pair: String,
    val dir: String,           // BUY / SELL
    val conf: Int,
    val grade: String,
    val entryPrice: String?,
    val exitPrice: String?,
    val pips: Float?,
    val result: String,        // WIN / LOSS / PENDING
    val session: String,
    val expiryMinutes: Int,
    val expiryAt: Long,        // epoch ms
    val note: String = "",
    val autoResolved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
