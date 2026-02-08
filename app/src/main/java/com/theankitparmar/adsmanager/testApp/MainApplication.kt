package com.theankitparmar.adsmanager.testApp
import android.app.Application
import com.theankitparmar.adsmanager.BuildConfig
import com.theankitparmar.adsmanager.core.AdsConfig
import com.theankitparmar.adsmanager.core.AdsManager

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ONE simple initialization call
        AdsManager.initialize(
            application = this,
            config = AdsConfig(
                isTestMode = BuildConfig.DEBUG,
                enableDebugLogging = BuildConfig.DEBUG,

                // Ad Unit IDs
                bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111",
                interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712",
                nativeAdUnitId = "ca-app-pub-3940256099942544/2247696110",
                appOpenAdUnitId = "ca-app-pub-3940256099942544/9257395921",
                rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917",

                // App Open Ad customization
                appOpenAdEnabled = true,
                showAppOpenOnFirstLaunch = false,
                minBackgroundTimeForAppOpen = 1000L, // 2 seconds

                // Activity exclusion callback
                shouldShowAppOpenAd = { activity ->
                    activity?.let {
                        // Exclude specific activities
                        when (activity) {
                            SplashActivity::class -> false
                            else -> true
                        }
                    } ?: true
                }
            ),
            onInitialized = {
                // Optional: Do something after initialization
                println("✅ AdsManager initialized successfully!")
            }
        )
    }

    override fun onTerminate() {
        AdsManager.destroyAllAds()
        super.onTerminate()
    }
}