package com.theankitparmar.adsmanager.core

import android.content.Context

data class AdsConfig(
    val isTestMode: Boolean = false,
    val testDeviceIds: List<String> = emptyList(),
    val enableConsentForm: Boolean = true,
    val enableDebugLogging: Boolean = false
)