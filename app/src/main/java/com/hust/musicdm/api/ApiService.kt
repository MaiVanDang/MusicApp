package com.hust.musicdm.api

import com.hust.musicdm.model.Song
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {
    @GET("songs.json")
    suspend fun getSongs(): Response<List<Song>>

    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>
}