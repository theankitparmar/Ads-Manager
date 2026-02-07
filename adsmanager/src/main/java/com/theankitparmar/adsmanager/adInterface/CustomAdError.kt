// CustomAdError.kt (RENAME from AdError.kt)
package com.theankitparmar.adsmanager.adInterface

import com.google.android.gms.ads.AdError as GmsAdError
import com.google.android.gms.ads.LoadAdError as GmsLoadAdError

sealed class CustomAdError(
    open val code: Int,
    open val message: String,
    val domain: String = "CUSTOM_AD_ERROR"
) {
    data class LoadError(
        override val code: Int,
        override val message: String,
        val loadAdError: GmsLoadAdError? = null
    ) : CustomAdError(code, message, "LOAD_ERROR")

    data class ShowError(
        override val code: Int,
        override val message: String,
        val adError: GmsAdError? = null
    ) : CustomAdError(code, message, "SHOW_ERROR")

    data class RevenueError(
        override val code: Int,
        override val message: String
    ) : CustomAdError(code, message, "REVENUE_ERROR")

    data class NetworkError(
        override val code: Int,
        override val message: String
    ) : CustomAdError(code, message, "NETWORK_ERROR")

    object NoFill : CustomAdError(3, "Ad request successful, but no ad returned")
    object Timeout : CustomAdError(2, "Ad request timed out")
    object InternalError : CustomAdError(0, "Internal error")
    object InvalidRequest : CustomAdError(1, "Invalid ad request")

    companion object {
        fun fromLoadAdError(error: GmsLoadAdError): LoadError {
            return LoadError(
                code = error.code,
                message = error.message,
                loadAdError = error
            )
        }

        fun fromAdError(error: GmsAdError): ShowError {
            return ShowError(
                code = error.code,
                message = error.message,
                adError = error
            )
        }
    }
}