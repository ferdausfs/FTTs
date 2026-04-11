package com.ftt.signal.data.api

import com.ftt.signal.data.model.HistoryResponse
import com.ftt.signal.data.model.SignalResponse
import com.ftt.signal.data.model.StatsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FttApiService {

    @GET("api/signal")
    suspend fun getSignal(
        @Query("pair") pair: String
    ): Response<SignalResponse>

    @GET("api/history")
    suspend fun getHistory(
        @Query("pair")  pair:  String,
        @Query("limit") limit: Int = 20
    ): Response<HistoryResponse>

    @GET("api/stats")
    suspend fun getStats(
        @Query("pair") pair: String
    ): Response<StatsResponse>

    @GET("api/report")
    suspend fun reportResult(
        @Query("id")     id:     String,
        @Query("result") result: String
    ): Response<Map<String, Any>>
}
