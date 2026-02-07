package com.theankitparmar.adsmanager.adInterface

import com.theankitparmar.adsmanager.core.AdUnits

data class AdsConfiguration(
    val isTestMode: Boolean = false,
    val adUnits: AdUnits,
    val enableAutoReload: Boolean = true,
    val retryPolicy: RetryPolicy = RetryPolicy.default,
    val enableConsentForm: Boolean = false,
    val enableDebugLogging: Boolean = false,
    val loadTimeoutMillis: Long = 10000L, // 10 seconds timeout
    val maxConcurrentLoads: Int = 3
) {
    init {
        require(adUnits.bannerAdUnitId.isNotBlank()) { "Banner ad unit ID is required" }
        require(adUnits.interstitialAdUnitId.isNotBlank()) { "Interstitial ad unit ID is required" }
        require(adUnits.nativeAdUnitId.isNotBlank()) { "Native ad unit ID is required" }
        require(adUnits.appOpenAdUnitId.isNotBlank()) { "App Open ad unit ID is required" }

        require(retryPolicy.maxRetries in 0..10) { "Max retries must be between 0 and 10" }
        require(retryPolicy.initialDelay >= 0) { "Initial delay cannot be negative" }
        require(retryPolicy.multiplier >= 1.0f) { "Multiplier must be >= 1.0" }
        require(loadTimeoutMillis > 0) { "Load timeout must be positive" }
        require(maxConcurrentLoads > 0) { "Max concurrent loads must be positive" }
    }

    companion object {
        fun createDefault(
            bannerAdUnitId: String,
            interstitialAdUnitId: String,
            nativeAdUnitId: String,
            appOpenAdUnitId: String,
            isTestMode: Boolean = false
        ): AdsConfiguration {
            return AdsConfiguration(
                isTestMode = isTestMode,
                adUnits = AdUnits(
                    bannerAdUnitId = bannerAdUnitId,
                    interstitialAdUnitId = interstitialAdUnitId,
                    nativeAdUnitId = nativeAdUnitId,
                    appOpenAdUnitId = appOpenAdUnitId
                )
            )
        }
    }
}