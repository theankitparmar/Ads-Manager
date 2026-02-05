package com.theankitparmar.adsmanager.core

data class AdUnits(
    val bannerAdUnitId: String,
    val interstitialAdUnitId: String,
    val nativeAdUnitId: String,
    val appOpenAdUnitId: String,
    val rewardedAdUnitId: String = ""
)