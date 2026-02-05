package com.theankitparmar.adsmanager.adInterface

import com.theankitparmar.adsmanager.core.AdUnits

data class AdsConfiguration(
    val isTestMode: Boolean = false,
    val adUnits: AdUnits,
    val enableAutoReload: Boolean = true,
    val retryPolicy: RetryPolicy = RetryPolicy.default,
    val enableConsentForm: Boolean = false
)
