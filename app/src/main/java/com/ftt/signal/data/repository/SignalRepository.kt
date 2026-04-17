package com.ftt.signal.data.repository

/**
 * Domain result wrapper used across the data and presentation layers.
 *
 * [Success] wraps a successfully fetched value.
 * [Error]   wraps an error message string.
 *
 * Kept in the data package (not domain) because it is tightly coupled
 * to Retrofit response handling. ViewModels and UseCases both import it.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
