package com.theankitparmar.adsmanager.callbacks

sealed class AdResult<out T> {
    data class Success<T>(val data: T) : AdResult<T>()
    data class Error(val message: String, val code: Int = 0) : AdResult<Nothing>()
    object Loading : AdResult<Nothing>()
}


//sealed class AdResult<out T> {
//    object Loading : AdResult<Nothing>()
//    data class Success<out T>(val data: T) : AdResult<T>()
//    data class Error(val message: String) : AdResult<Nothing>()
//}