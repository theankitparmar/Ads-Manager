// AdEvent.kt
package com.theankitparmar.adsmanager.adInterface

sealed class AdEvent {
    data class Loaded(val adType: AdType, val adId: String? = null) : AdEvent()
    data class Failed(val adType: AdType, val error: CustomAdError, val adId: String? = null) : AdEvent()
    data class Impression(val adType: AdType, val adId: String? = null) : AdEvent()
    data class Clicked(val adType: AdType, val adId: String? = null) : AdEvent()
    data class Revenue(
        val adType: AdType,
        val valueMicros: Long,
        val currencyCode: String,
        val precision: Int,
        val adId: String? = null
    ) : AdEvent()
    data class Dismissed(val adType: AdType, val adId: String? = null) : AdEvent()
    data class Opened(val adType: AdType, val adId: String? = null) : AdEvent()
}

enum class AdType {
    BANNER, INTERSTITIAL, NATIVE, APP_OPEN, REWARDED
}