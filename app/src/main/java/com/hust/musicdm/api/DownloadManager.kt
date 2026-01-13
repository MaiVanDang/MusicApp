package com.hust.musicdm.api

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream

class DownloadManager(private val context: Context) {

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        data class Success(val filePath: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    fun downloadSong(
        songId: Long,
        downloadUrl: String,
        fileName: String
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        try {
            val response = RetrofitClient.apiService.downloadFile(downloadUrl)

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val musicDir = File(context.filesDir, "music")
                    if (!musicDir.exists()) {
                        musicDir.mkdirs()
                    }

                    val file = File(musicDir, "${songId}_$fileName")
                    val totalBytes = body.contentLength()
                    var bytesDownloaded = 0L
                    val buffer = ByteArray(4096)

                    body.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead

                                if (totalBytes > 0) {
                                    val progress = ((bytesDownloaded * 100) / totalBytes).toInt()
                                    emit(DownloadState.Downloading(progress))
                                }
                            }
                        }
                    }

                    emit(DownloadState.Success(file.absolutePath))
                } ?: emit(DownloadState.Error("Empty response body"))
            } else {
                emit(DownloadState.Error("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(DownloadState.Error("Connection error: ${e.message}"))
        }
    }

    fun deleteDownloadedSong(songId: Long): Boolean {
        val musicDir = File(context.filesDir, "music")
        val files = musicDir.listFiles { file ->
            file.name.startsWith("${songId}_")
        }

        return files?.all { it.delete() } ?: false
    }

    fun getDownloadedFilePath(songId: Long): String? {
        val musicDir = File(context.filesDir, "music")
        val file = musicDir.listFiles { file ->
            file.name.startsWith("${songId}_")
        }?.firstOrNull()

        return file?.absolutePath
    }
}