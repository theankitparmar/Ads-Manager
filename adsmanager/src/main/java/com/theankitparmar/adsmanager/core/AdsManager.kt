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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdsManager {

    private var isInitialized = false
    private var isInitializing = false
    private lateinit var application: Application
    private lateinit var config: AdsConfiguration
    private val initializationDeferred = CompletableDeferred<Unit>()

    private val ads = mutableMapOf<String, com.theankitparmar.adsmanager.adInterface.AdManager>()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize(
        application: Application,
        config: AdsConfiguration
    ) {
        if (isInitialized || isInitializing) {
            Log.d("AdsManager", "Already initialized or initializing")
            return
        }

        this.application = application
        this.config = config
        isInitializing = true

        Log.d("AdsManager", "Starting AdsManager initialization...")

        MobileAds.initialize(application) {
            isInitialized = true
            isInitializing = false

            Log.d("AdsManager", "✅ AdMob SDK initialized successfully")

            // Complete the deferred to signal initialization is done
            initializationDeferred.complete(Unit)

            // Preload ads only after successful initialization
            preloadAds()
        }

        // Set test devices if in test mode
        if (config.isTestMode) {
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(configuration)
            Log.d("AdsManager", "Test mode enabled")
        }
    }

    private fun requireInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("AdsManager must be initialized first. Call AdsManager.initialize() in your Application class.")
        }
    }

    // Get BannerAd - synchronous version
    fun getBannerAd(context: Context, adSize: AdSize = AdSize.BANNER): BannerAd {
        requireInitialized()
        return BannerAd(
            bannerContext = context,
            adUnitId = config.adUnits.bannerAdUnitId,
            config = config,
            adSize = adSize
        ).also { ad ->
            ads["banner_${ads.size}"] = ad
            ad.loadAd()
        }
    }

    // Get BannerAd with async initialization check
    suspend fun getBannerAdAsync(context: Context, adSize: AdSize = AdSize.BANNER): BannerAd {
        awaitInitialization()
        return getBannerAd(context, adSize)
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

    suspend fun getInterstitialAdAsync(context: Context): InterstitialAd {
        awaitInitialization()
        return getInterstitialAd(context)
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

    suspend fun getNativeAdAsync(context: Context): NativeAd {
        awaitInitialization()
        return getNativeAd(context)
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

    suspend fun getAppOpenAdAsync(context: Context): AppOpenAd {
        awaitInitialization()
        return getAppOpenAd(context)
    }

    private fun preloadAds() {
        Log.d("AdsManager", "Starting ad preloading...")

        // Preload interstitial
        try {
            getInterstitialAd(application)
            Log.d("AdsManager", "✅ Interstitial ad preloaded")
        } catch (e: Exception) {
            Log.e("AdsManager", "Failed to preload interstitial: ${e.message}")
        }

        // Preload native
        try {
            getNativeAd(application)
            Log.d("AdsManager", "✅ Native ad preloaded")
        } catch (e: Exception) {
            Log.e("AdsManager", "Failed to preload native: ${e.message}")
        }

        // Preload app open
        try {
            getAppOpenAd(application)
            Log.d("AdsManager", "✅ App Open ad preloaded")
        } catch (e: Exception) {
            Log.e("AdsManager", "Failed to preload app open: ${e.message}")
        }

        Log.d("AdsManager", "✅ All ads preloaded successfully")
    }

    fun destroyAllAds() {
        ads.values.forEach { it.destroy() }
        ads.clear()
        Log.d("AdsManager", "All ads destroyed")
    }

    fun isInitialized(): Boolean = isInitialized

    fun isInitializing(): Boolean = isInitializing

    // Wait for initialization to complete
    suspend fun awaitInitialization() {
        if (!isInitialized) {
            Log.d("AdsManager", "Waiting for initialization...")
            initializationDeferred.await()
            Log.d("AdsManager", "Initialization completed")
        }
    }

    // Initialize with callback for synchronous operations
    fun initializeWithCallback(
        application: Application,
        config: AdsConfiguration,
        onInitialized: () -> Unit
    ) {
        if (isInitialized || isInitializing) {
            onInitialized()
            return
        }

        this.application = application
        this.config = config
        isInitializing = true

        Log.d("AdsManager", "Starting AdsManager initialization...")

        MobileAds.initialize(application) {
            isInitialized = true
            isInitializing = false

            Log.d("AdsManager", "✅ AdMob SDK initialized successfully")
            initializationDeferred.complete(Unit)

            onInitialized()

            // Preload ads in background
            scope.launch {
                preloadAds()
            }
        }

        if (config.isTestMode) {
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }
    }
}