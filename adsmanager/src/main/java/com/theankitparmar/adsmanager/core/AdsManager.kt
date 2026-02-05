// AdsManager.kt
package com.theankitparmar.adsmanager.core

import com.theankitparmar.adsmanager.ads.native.NativeAd
import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.theankitparmar.adsmanager.adInterface.AdsConfiguration
import com.theankitparmar.adsmanager.ads.banner.BannerAd
import com.theankitparmar.adsmanager.ads.inter.InterstitialAd
import com.theankitparmar.adsmanager.ads.open.AppOpenAd

object AdsManager {

    private var isInitialized = false
    private lateinit var application: Application
    private lateinit var config: AdsConfiguration

    private val ads = mutableMapOf<String, com.theankitparmar.adsmanager.adInterface.AdManager>()

    fun initialize(
        application: Application,
        config: AdsConfiguration
    ) {
        this.application = application
        this.config = config

        MobileAds.initialize(application) {
            isInitialized = true
            Log.d("AdsManager", "AdMob SDK initialized successfully")

            // Preload ads
            preloadAds()
        }

        // Set test devices if in test mode
        if (config.isTestMode) {
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }
    }

    private fun requireInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("AdsManager must be initialized first.")
        }
    }

    // NEW: Overloaded function to support custom AdSize
    fun getBannerAd(context: Context, adSize: AdSize = AdSize.BANNER): BannerAd {
        requireInitialized()
        return BannerAd(
            bannerContext = context,
            adUnitId = config.adUnits.bannerAdUnitId,
            config = config,
            adSize = adSize  // Pass the adSize parameter
        ).also { ad ->
            ads["banner_${ads.size}"] = ad
            ad.loadAd()
        }
    }


    fun getInterstitialAd(context: Context): InterstitialAd {
        requireInitialized()
        return InterstitialAd(
            context = context,
            adUnitId = config.adUnits.interstitialAdUnitId,
            config = config
        ).also { ad ->
            ads["interstitial_${ads.size}"] = ad
            ad.loadAd()
        }
    }

    fun getNativeAd(context: Context): NativeAd {
        requireInitialized()
        return NativeAd(
            context = context,
            adUnitId = config.adUnits.nativeAdUnitId,
            config = config
        ).also { ad ->
            ads["native_${ads.size}"] = ad
            ad.loadAd()
        }
    }

    fun getAppOpenAd(context: Context): AppOpenAd {
        requireInitialized()
        return AppOpenAd(
            context = context,
            adUnitId = config.adUnits.appOpenAdUnitId,
            config = config
        ).also { ad ->
            ads["app_open"] = ad
            ad.loadAd()
        }
    }

    private fun preloadAds() {
        // Preload interstitial
        getInterstitialAd(application)

        // Preload native
        getNativeAd(application)

        // Preload app open
        getAppOpenAd(application)
    }

    fun destroyAllAds() {
        ads.values.forEach { it.destroy() }
        ads.clear()
    }

    fun isInitialized(): Boolean = isInitialized
}