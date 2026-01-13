package com.hust.musicdm.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val downloadUrl: String,
    val albumArt: String?,
    val duration: Long,
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    val downloadedAt: Long? = null
)