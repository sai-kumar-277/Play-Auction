package com.playauction.app.data.network

import com.playauction.app.data.model.*
import retrofit2.http.*

interface ApiService {
    @POST("api/session/init")
    suspend fun initSession(@Body body: Map<String, String>): SessionData

    @GET("api/room/{code}")
    suspend fun getRoom(@Path("code") code: String): RoomState

    @GET("api/room/{code}/results")
    suspend fun getResults(@Path("code") code: String): ResultsResponse

    @GET("api/players")
    suspend fun getPlayers(@Query("league") league: String? = null): List<Player>

    @POST("api/feedback")
    suspend fun submitFeedback(@Body body: FeedbackRequest): Map<String, String>
}
