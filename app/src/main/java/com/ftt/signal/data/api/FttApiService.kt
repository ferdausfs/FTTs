package com.ftt.signal.data.api

import com.ftt.signal.data.model.ApiSignalResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FttApiService {
    @GET("api/signal")
    suspend fun getSignal(@Query("pair") pair: String): ApiSignalResponse
}
