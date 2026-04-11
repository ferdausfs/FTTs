package com.ftt.signal.data.api

import com.ftt.signal.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var baseUrl = BuildConfig.WORKER_BASE_URL

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val api: FttApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(baseUrl))
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FttApiService::class.java)
    }

    private fun ensureTrailingSlash(url: String) =
        if (url.endsWith("/")) url else "$url/"
}
