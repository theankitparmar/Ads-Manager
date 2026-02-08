// AdsManager.kt
package com.theankitparmar.adsmanager.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.theankitparmar.adsmanager.ads.banner.BannerAd
import com.theankitparmar.adsmanager.ads.inter.InterstitialAd
import com.theankitparmar.adsmanager.ads.native.NativeAd
import com.theankitparmar.adsmanager.ads.native.NativeType
import com.theankitparmar.adsmanager.ads.open.AppOpenAd
import com.theankitparmar.adsmanager.utils.AdHelper
import com.theankitparmar.adsmanager.utils.AppOpenManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.theankitparmar.adsmanager.adInterface.AdsConfiguration
import com.theankitparmar.adsmanager.core.AdUnits

object AdsManager {

    private const val TAG = "AdsManager"

    private var isInitialized = false
    private var isInitializing = false
    private lateinit var application: Application
    private lateinit var config: AdsConfig
    private val initializationDeferred = CompletableDeferred<Unit>()

    private val ads = mutableMapOf<String, com.theankitparmar.adsmanager.adInterface.AdManager>()
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Simple initialization method - One call to rule them all!
     */
    fun initialize(
        application: Application,
        config: AdsConfig,
        onInitialized: (() -> Unit)? = null
    ) {
        if (isInitialized || isInitializing) {
            Log.d(TAG, "Already initialized or initializing")
            onInitialized?.invoke()
            return
        }

        this.application = application
        this.config = config
        isInitializing = true

        Log.d(TAG, "Starting AdsManager initialization...")

        // Initialize AdMob SDK
        MobileAds.initialize(application) {
            isInitialized = true
            isInitializing = false

            Log.d(TAG, "✅ AdMob SDK initialized successfully")
            initializationDeferred.complete(Unit)

            // Initialize AppOpenManager automatically
            setupAppOpenManager()

            // Preload ads
            preloadAds()

            // Callback
            onInitialized?.invoke()
        }

        // Set test devices if in test mode
        if (config.isTestMode) {
            val testDeviceIds = listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR)
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
            Log.d(TAG, "Test mode enabled")
        }
    }

    /**
     * Setup AppOpenManager automatically
     */
    private fun setupAppOpenManager() {
        if (!config.appOpenAdEnabled) {
            Log.d(TAG, "App Open Ads are disabled")
            return
        }

        // Initialize AppOpenManager
        AppOpenManager.initialize(application)

        // Set debug mode
        AppOpenManager.setDebugMode(config.enableDebugLogging)

        // Set min background time
        AppOpenManager.setMinBackgroundTime(config.minBackgroundTimeForAppOpen)

        // Set show on first launch based on config
        AppOpenManager.setShowOnFirstLaunch(config.showAppOpenOnFirstLaunch)

        Log.d(TAG, "✅ AppOpenManager initialized automatically. Show on first launch: ${config.showAppOpenOnFirstLaunch}")
    }
    /**
     * Show App Open Ad when app returns from background
     * Call this from Activity's onResume or onCreate
     */
    fun showAppOpenAdOnResume(activity: Activity) {
        if (!config.appOpenAdEnabled) return

        // Use the callback to check if ad should show
        if (!config.shouldShowAppOpenAd(activity)) {
            Log.d(TAG, "App Open Ad excluded for activity: ${activity::class.java.simpleName}")
            return
        }

        // Delegate to AppOpenManager
        AppOpenManager.showAdIfAvailable()
    }

    /**
     * Exclude an activity from showing App Open Ads
     */
    fun excludeActivityFromAppOpenAd(activityClass: Class<*>) {
        AppOpenManager.excludeActivity(activityClass)
    }

    /**
     * Include an activity back for App Open Ads
     */
    fun includeActivityForAppOpenAd(activityClass: Class<*>) {
        AppOpenManager.includeActivity(activityClass)
    }

    /**
     * Force show App Open Ad (for testing or specific cases)
     */
    fun showAppOpenAd(
        activity: Activity,
        showLoadingDialog: Boolean = false,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ) {
        AdHelper.showAppOpenAd(
            activity = activity,
            showLoadingDialog = showLoadingDialog,
            onAdDismissed = onAdDismissed,
            onAdFailed = onAdFailed
        )
    }

    // ============ Existing Ad Methods (simplified) ============

    private fun requireInitialized() {
        check(isInitialized) {
            "AdsManager must be initialized first. Call AdsManager.initialize() in your Application class."
        }
    }

    // Get BannerAd - synchronous version
    fun getBannerAd(context: Context, adSize: AdSize = AdSize.BANNER): BannerAd {
        requireInitialized()
        return BannerAd(
            bannerContext = context,
            adUnitId = config.bannerAdUnitId,
            config = convertConfig(),
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
            adUnitId = config.interstitialAdUnitId,
            config = convertConfig()
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
            adUnitId = config.nativeAdUnitId,
            config = convertConfig()
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
            adUnitId = config.appOpenAdUnitId,
            config = convertConfig()
        ).also { ad ->
            ads["app_open"] = ad
            ad.loadAd()
        }
    }

    /**
     * Convert new AdsConfig to old AdsConfiguration (for compatibility)
     */
    private fun convertConfig(): com.theankitparmar.adsmanager.adInterface.AdsConfiguration {
        val adUnits = com.theankitparmar.adsmanager.core.AdUnits(
            bannerAdUnitId = config.bannerAdUnitId,
            interstitialAdUnitId = config.interstitialAdUnitId,
            nativeAdUnitId = config.nativeAdUnitId,
            appOpenAdUnitId = config.appOpenAdUnitId,
            rewardedAdUnitId = config.rewardedAdUnitId
        )

        return com.theankitparmar.adsmanager.adInterface.AdsConfiguration(
            isTestMode = config.isTestMode,
            adUnits = adUnits,
            enableAutoReload = true,
            enableConsentForm = config.enableConsentForm,
            enableDebugLogging = config.enableDebugLogging
        )
    }

    private fun preloadAds() {
        Log.d(TAG, "Starting ad preloading...")

        // Preload in background
        scope.launch {
            // Preload interstitial
            try {
                getInterstitialAd(application)
                Log.d(TAG, "✅ Interstitial ad preloaded")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload interstitial: ${e.message}")
            }

            // Preload native
            try {
                getNativeAd(application)
                Log.d(TAG, "✅ Native ad preloaded")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload native: ${e.message}")
            }

            // Preload app open if enabled
            if (config.appOpenAdEnabled) {
                try {
                    getAppOpenAd(application)
                    Log.d(TAG, "✅ App Open ad preloaded")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to preload app open: ${e.message}")
                }
            }

            Log.d(TAG, "✅ All ads preloaded successfully")
        }
    }

    suspend fun awaitInitialization() {
        if (!isInitialized) {
            Log.d(TAG, "Waiting for initialization...")
            initializationDeferred.await()
            Log.d(TAG, "Initialization completed")
        }
    }

    fun isInitialized(): Boolean = isInitialized

    fun destroyAllAds() {
        ads.values.forEach { it.destroy() }
        ads.clear()
        AppOpenManager.destroy()
        Log.d(TAG, "All ads destroyed")
    }

    // Get NativeAd with type
    fun getNativeAdWithType(
        context: Context,
        nativeType: NativeType = NativeType.MEDIUM
    ): NativeAd {
        requireInitialized()
        return NativeAd(
            context = context,
            adUnitId = config.nativeAdUnitId,
            config = convertConfig(),
            nativeType = nativeType
        ).also { ad ->
            ads["native_${nativeType.name}_${ads.size}"] = ad
            ad.loadAd()
        }
    }

    // Get NativeAd with custom layout
    fun getNativeAdWithCustomLayout(
        context: Context,
        nativeType: NativeType = NativeType.CUSTOM,
        customNativeLayoutResId: Int,
        customShimmerLayoutResId: Int? = null
    ): NativeAd {
        requireInitialized()
        return NativeAd(
            context = context,
            adUnitId = config.nativeAdUnitId,
            config = convertConfig(),
            nativeType = nativeType,
            customNativeLayoutResId = customNativeLayoutResId,
            customShimmerLayoutResId = customShimmerLayoutResId
        ).also { ad ->
            ads["native_custom_${ads.size}"] = ad
            ad.loadAd()
        }
    }

    // Async versions
    suspend fun getNativeAdWithTypeAsync(
        context: Context,
        nativeType: NativeType = NativeType.MEDIUM
    ): NativeAd {
        awaitInitialization()
        return getNativeAdWithType(context, nativeType)
    }

    suspend fun getNativeAdWithCustomLayoutAsync(
        context: Context,
        nativeType: NativeType = NativeType.CUSTOM,
        customNativeLayoutResId: Int,
        customShimmerLayoutResId: Int? = null
    ): NativeAd {
        awaitInitialization()
        return getNativeAdWithCustomLayout(
            context,
            nativeType,
            customNativeLayoutResId,
            customShimmerLayoutResId
        )
    }
}