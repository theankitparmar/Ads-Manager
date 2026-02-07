package com.theankitparmar.adsmanager.testApp

import android.app.Application
import android.util.Log
import com.theankitparmar.adsmanager.BuildConfig
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.core.AdUnits
import com.theankitparmar.adsmanager.adInterface.AdsConfiguration

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("MainApplication", "Application onCreate started")

        // Create AdUnits (using test IDs for development)
        val adUnits = AdUnits(
            bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111",
            interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712",
            nativeAdUnitId = "ca-app-pub-3940256099942544/2247696110",
            appOpenAdUnitId = "ca-app-pub-3940256099942544/9257395921",
            rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917" // Optional, leave empty if not using
        )

        // Create AdsConfiguration
        val adsConfig = AdsConfiguration(
            isTestMode = BuildConfig.DEBUG, // Test mode in debug builds
            adUnits = adUnits,
            enableAutoReload = true,
            enableConsentForm = false,
            enableDebugLogging = true
        )

        // Initialize AdsManager with callback
        AdsManager.initializeWithCallback(this, adsConfig) {
            Log.d("MainApplication", "✅ AdsManager initialized successfully")
        }

        Log.d("MainApplication", "Application onCreate completed")
    }
}