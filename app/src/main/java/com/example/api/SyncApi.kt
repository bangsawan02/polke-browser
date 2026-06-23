package com.example.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Mock Data structure for Dummy Endpoint
data class SyncPayload(
    val title: String,
    val body: String,
    val userId: Int
)

data class SyncResponse(
    val id: Int,
    val title: String,
    val body: String,
    val userId: Int
)

interface SyncApi {
    @POST("posts")
    suspend fun uploadBookmarks(@Body payload: SyncPayload): SyncResponse
    
    @GET("posts/1")
    suspend fun getBookmarks(): SyncResponse
}

object ApiClient {
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val syncApi: SyncApi = retrofit.create(SyncApi::class.java)
}
