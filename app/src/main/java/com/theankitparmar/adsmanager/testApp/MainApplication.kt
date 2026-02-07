package com.theankitparmar.adsmanager.testApp

import android.app.Application
import android.util.Log
import com.theankitparmar.adsmanager.BuildConfig
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.core.AdUnits
import com.theankitparmar.adsmanager.adInterface.AdsConfiguration
import com.theankitparmar.adsmanager.utils.AppOpenManager

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
            rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917"
        )

        // Create AdsConfiguration
        val adsConfig = AdsConfiguration(
            isTestMode = BuildConfig.DEBUG,
            adUnits = adUnits,
            enableAutoReload = true,
            enableConsentForm = false,
            enableDebugLogging = true
        )

        // Initialize AppOpenManager FIRST (before AdsManager)
        AppOpenManager.initialize(this)
        AppOpenManager.setDebugMode(BuildConfig.DEBUG)

        // Exclude specific activities (optional - can also be done in activities)
        excludeAppOpenAdActivities()

        // Initialize AdsManager with callback
        AdsManager.initializeWithCallback(this, adsConfig) {
            Log.d("MainApplication", "✅ AdsManager initialized successfully")

            // Preload App Open Ad after AdsManager is ready
            AppOpenManager.preloadNextAd()
        }

        Log.d("MainApplication", "Application onCreate completed")
    }

    /**
     * Define which activities should NOT show App Open Ads
     */
    private fun excludeAppOpenAdActivities() {
        try {
            // Exclude Splash Screen
            AppOpenManager.excludeActivity(Class.forName("com.theankitparmar.adsmanager.testApp.SplashActivity"))
//
//            // Exclude Login Screen
//            AppOpenManager.excludeActivity(Class.forName("com.theankitparmar.adsmanager.testApp.LoginActivity"))
//
//            // Exclude Payment Screen
//            AppOpenManager.excludeActivity(Class.forName("com.theankitparmar.adsmanager.testApp.PaymentActivity"))

            Log.d("MainApplication", "Excluded activities for App Open Ads")
        } catch (e: ClassNotFoundException) {
            Log.w("MainApplication", "Some activity classes not found for exclusion")
        }
    }

    override fun onTerminate() {
        AppOpenManager.destroy()
        super.onTerminate()
    }
}