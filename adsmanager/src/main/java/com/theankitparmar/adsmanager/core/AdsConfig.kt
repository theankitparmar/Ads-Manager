package com.theankitparmar.adsmanager.core

import android.app.Activity

/**
 * Unified configuration for all ads
 */
data class AdsConfig(
    // AdMob configuration
    val isTestMode: Boolean = false,
    val enableDebugLogging: Boolean = false,
    val enableConsentForm: Boolean = false,

    // Ad Units
    val bannerAdUnitId: String = "",
    val interstitialAdUnitId: String = "",
    val nativeAdUnitId: String = "",
    val appOpenAdUnitId: String = "",
    val rewardedAdUnitId: String = "",

    // App Open Ad specific
    val appOpenAdEnabled: Boolean = true,
    val showAppOpenOnFirstLaunch: Boolean = false,
    val minBackgroundTimeForAppOpen: Long = 2000L, // 2 seconds
    val shouldShowAppOpenAd: ((activity: Activity?) -> Boolean) = { activity ->
        // Default: Show on all activities except excluded ones
        activity != null
    }
)