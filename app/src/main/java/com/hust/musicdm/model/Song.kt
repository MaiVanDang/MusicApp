package com.hust.musicdm.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long = 0,
    val title: String? = null,
    val artist: String? = null,
    val streamUrl: String = "",
    val downloadUrl: String = "",
    val albumArt: String? = null,
    val duration: Long = 0,
    val isDownloaded: Boolean = false,
    val localPath: String? = null
): Parcelable