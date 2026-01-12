package com.umbra.umbradex.utils

// Uma classe selada ajuda a gerir se estamos a carregar, se deu erro ou sucesso
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}