package com.theankitparmar.adsmanager.testApp

import android.app.Application
import com.theankitparmar.adsmanager.BuildConfig
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.core.AdUnits
import com.theankitparmar.adsmanager.core.AdsConfig

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Ads Manager
        val adUnits = AdUnits(
            appId = "ca-app-pub-3940256099942544~3347511713", // Test App ID
            bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111",
            nativeAdUnitId = "ca-app-pub-3940256099942544/2247696110",
            interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712",
            appOpenAdUnitId = "ca-app-pub-3940256099942544/3419835294"
        )

        val adsConfig = AdsConfig(
            isTestMode = BuildConfig.DEBUG, // Test mode in debug builds
            testDeviceIds = emptyList(), // Add your test device IDs here
            enableConsentForm = true,
            enableDebugLogging = true
        )

        AdsManager.initialize(this, adUnits, adsConfig)
    }
}