package com.ftt.signal.data.api

import com.ftt.signal.data.model.GradeDeserializer
import com.ftt.signal.data.model.GradeField
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit factory.
 *
 * Kept as an object (not a Hilt module) because the base URL is
 * dynamic — it comes from user preferences. Hilt cannot inject a
 * Retrofit instance whose base URL is known only at runtime.
 *
 * Called from [SignalRepositoryImpl] which IS injected by Hilt.
 */
object RetrofitClient {

    private val gson = GsonBuilder()
        .registerTypeAdapter(GradeField::class.java, GradeDeserializer())
        .setLenient()
        .create()

    /**
     * OkHttp client shared across all requests.
     * Logging is disabled in release builds for performance + privacy.
     */
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    /** Build a [FttApiService] for the given [baseUrl]. */
    fun create(baseUrl: String): FttApiService {
        val sanitised = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitised)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FttApiService::class.java)
    }
}
