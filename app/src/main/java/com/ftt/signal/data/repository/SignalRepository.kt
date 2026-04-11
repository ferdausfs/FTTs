package com.ftt.signal.data.repository

import com.ftt.signal.data.api.RetrofitClient
import com.ftt.signal.data.model.HistoryResponse
import com.ftt.signal.data.model.SignalResponse
import com.ftt.signal.data.model.StatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class SignalRepository {

    private val api = RetrofitClient.api

    suspend fun getSignal(pair: String): Result<SignalResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSignal(pair)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.Success(body)
                else Result.Error("Empty response from server")
            } else {
                Result.Error("Server error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getHistory(pair: String): Result<HistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHistory(pair)
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getStats(pair: String): Result<StatsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStats(pair)
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun reportResult(id: String, result: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.reportResult(id, result)
            if (response.isSuccessful) Result.Success(true)
            else Result.Error("Report failed: ${response.code()}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
