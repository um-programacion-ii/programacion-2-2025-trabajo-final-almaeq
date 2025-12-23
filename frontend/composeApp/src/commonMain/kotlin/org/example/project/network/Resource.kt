package org.example.project.network

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data object SessionExpired : Resource<Nothing>()
}