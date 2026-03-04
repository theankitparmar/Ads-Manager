# 📚 AdsManager API Reference

Complete API documentation for AdsManager library.

---

## Table of Contents
- [AdsManager](#adsmanager-main-class)
- [AdHelper](#adhelper-helper-class)
- [Ad Types](#ad-types)
- [Callbacks](#callbacks)
- [Configuration](#configuration)
- [Events](#events)

---

## AdsManager (Main Class)

The main entry point for the AdsManager library. Use this to initialize and manage all ads.

### Initialization

```kotlin
/**
 * Initialize AdsManager with configuration
 * Must be called once in Application.onCreate()
 * 
 * @param application The Application instance
 * @param config The ads configuration
 * @param onInitialized Optional callback when initialization completes
 */
fun initialize(
    application: Application,
    config: AdsConfig,
    onInitialized: (() -> Unit)? = null
)
```

**Example:**
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = AdsConfig(
            isTestMode = BuildConfig.DEBUG,
            bannerAdUnitId = "YOUR_BANNER_ID",
            interstitialAdUnitId = "YOUR_INTERSTITIAL_ID",
            nativeAdUnitId = "YOUR_NATIVE_ID",
            appOpenAdUnitId = "YOUR_APPOPEN_ID"
        )
        
        AdsManager.initialize(this, config) {
            Log.d("AdsManager", "Initialized")
        }
    }
}
```

---

### Getting Ads

#### Banner Ads

```kotlin
/**
 * Get a banner ad instance
 * 
 * @param context The context for ad loading
 * @param adSize The banner ad size (default: BANNER)
 * @return BannerAd instance
 */
fun getBannerAd(context: Context, adSize: AdSize = AdSize.BANNER): BannerAd
```

**Example:**
```kotlin
val bannerAd = AdsManager.getBannerAd(this, AdSize.BANNER)
```

#### Interstitial Ads

```kotlin
/**
 * Get an interstitial ad instance
 * 
 * @param context The context for ad loading
 * @return InterstitialAd instance
 */
fun getInterstitialAd(context: Context): InterstitialAd
```

**Example:**
```kotlin
val interstitialAd = AdsManager.getInterstitialAd(this)
```

#### Native Ads

```kotlin
/**
 * Get a native ad instance
 * 
 * @param context The context for ad loading
 * @return NativeAd instance
 */
fun getNativeAd(context: Context): NativeAd

/**
 * Get a native ad with specific type
 * 
 * @param context The context
 * @param nativeType The type of native ad (SMALL, MEDIUM, LARGE, etc.)
 * @return NativeAd instance with specified type
 */
fun getNativeAdWithType(
    context: Context,
    nativeType: NativeType = NativeType.MEDIUM
): NativeAd

/**
 * Get a native ad with custom layout
 * 
 * @param context The context
 * @param nativeType The native ad type (usually CUSTOM)
 * @param customNativeLayoutResId Layout resource for ad view
 * @param customShimmerLayoutResId Layout resource for shimmer (optional)
 * @return NativeAd instance with custom layout
 */
fun getNativeAdWithCustomLayout(
    context: Context,
    nativeType: NativeType = NativeType.CUSTOM,
    customNativeLayoutResId: Int,
    customShimmerLayoutResId: Int? = null
): NativeAd
```

**Examples:**
```kotlin
// Standard native ad
val nativeAd = AdsManager.getNativeAd(this)

// With specific type
val mediumNativeAd = AdsManager.getNativeAdWithType(this, NativeType.MEDIUM)

// With custom layout
val customNativeAd = AdsManager.getNativeAdWithCustomLayout(
    context = this,
    nativeType = NativeType.CUSTOM,
    customNativeLayoutResId = R.layout.my_native_layout
)
```

#### App Open Ads

```kotlin
/**
 * Get an app open ad instance
 * 
 * @param context The context for ad loading
 * @return AppOpenAd instance
 */
fun getAppOpenAd(context: Context): AppOpenAd
```

**Example:**
```kotlin
val appOpenAd = AdsManager.getAppOpenAd(this)
```

---

### App Open Ad Management

```kotlin
/**
 * Show app open ad when app returns from background
 * Call from Activity.onResume()
 * 
 * @param activity The activity to show ad in
 */
fun showAppOpenAdOnResume(activity: Activity)

/**
 * Exclude an activity from app open ads
 * 
 * @param activityClass The activity class to exclude
 */
fun excludeActivityFromAppOpenAd(activityClass: Class<*>)

/**
 * Include an activity back for app open ads
 * 
 * @param activityClass The activity class to include
 */
fun includeActivityForAppOpenAd(activityClass: Class<*>)

/**
 * Force show app open ad
 * 
 * @param activity The activity to show ad in
 * @param showLoadingDialog Whether to show loading dialog
 * @param onAdDismissed Callback when ad is dismissed
 * @param onAdFailed Callback if ad fails
 */
fun showAppOpenAd(
    activity: Activity,
    showLoadingDialog: Boolean = false,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
)
```

---

### Status & Lifecycle

```kotlin
/**
 * Check if AdsManager is initialized
 * 
 * @return true if initialized, false otherwise
 */
fun isInitialized(): Boolean

/**
 * Wait for AdsManager initialization to complete
 * Use with coroutines
 * 
 * @throws CancellationException if coroutine is cancelled
 */
suspend fun awaitInitialization()

/**
 * Destroy all ads and clean up resources
 * Call in Activity.onDestroy()
 */
fun destroyAllAds()
```

**Example:**
```kotlin
// Check initialization
if (AdsManager.isInitialized()) {
    // Safe to use
}

// Wait for initialization (coroutine)
lifecycleScope.launch {
    AdsManager.awaitInitialization()
    Log.d("App", "Ready to use ads")
}

// Clean up
override fun onDestroy() {
    super.onDestroy()
    AdsManager.destroyAllAds()
}
```

---

## AdHelper (Helper Class)

Simplified API for common ad operations.

### Banner Ads

```kotlin
/**
 * Show a banner ad in a container
 * Automatically handles loading, sizing, and shimmer effects
 * 
 * @param context The context
 * @param container The ViewGroup to show ad in
 * @param bannerAdSize The banner size (default: STANDARD)
 * @param showShimmer Whether to show loading shimmer
 * @param onAdLoaded Callback when ad loads
 * @param onAdFailed Callback if ad fails
 * @return BannerAd instance
 */
fun showBannerAd(
    context: Context,
    container: ViewGroup,
    bannerAdSize: BannerAdSize = BannerAdSize.STANDARD,
    showShimmer: Boolean = true,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
): BannerAd

/**
 * Get or create a reusable banner ad
 * Useful for showing same banner in multiple places
 * 
 * @param activity The activity context
 * @return BannerAd instance
 */
fun getReusableBannerAd(activity: Activity): BannerAd

/**
 * Clear the reusable banner ad
 */
fun clearReusableBannerAd()

/**
 * Pause all banner ads
 * Call in Activity.onPause()
 */
fun pauseBannerAds()

/**
 * Resume all banner ads
 * Call in Activity.onResume()
 */
fun resumeBannerAds()
```

**Banner Sizes:**
```kotlin
enum class BannerAdSize {
    STANDARD,           // 320x50
    LARGE,              // 320x100
    MEDIUM_RECTANGLE,   // 300x250
    FULL_BANNER,        // 468x60
    LEADERBOARD,        // 728x90
    ADAPTIVE            // Auto-adjust to screen width
}
```

---

### Interstitial Ads

```kotlin
/**
 * Show an interstitial ad
 * Automatically handles loading and showing
 * 
 * @param activity The activity to show ad in
 * @param showLoadingDialog Whether to show loading dialog
 * @param onAdDismissed Callback when ad is dismissed
 * @param onAdFailed Callback if ad fails to load or show
 */
fun showInterstitialAd(
    activity: Activity,
    showLoadingDialog: Boolean = true,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
)

/**
 * Get or create a reusable interstitial ad
 * 
 * @param activity The activity context
 * @return InterstitialAd instance
 */
fun getReusableInterstitialAd(activity: Activity): InterstitialAd

/**
 * Clear the reusable interstitial ad
 */
fun clearReusableInterstitialAd()
```

---

### Native Ads

```kotlin
/**
 * Show a native ad in a container
 * Handles loading and inflating the native ad view
 * 
 * @param context The context
 * @param container The ViewGroup to show ad in
 * @param nativeType The type of native ad
 * @param showShimmer Whether to show loading shimmer
 * @param customNativeLayoutResId Custom layout (for CUSTOM type)
 * @param customShimmerLayoutResId Custom shimmer layout
 * @param onAdLoaded Callback when ad loads
 * @param onAdFailed Callback if ad fails
 * @return NativeAd instance
 */
fun showNativeAd(
    context: Context,
    container: ViewGroup,
    nativeType: NativeType = NativeType.MEDIUM,
    showShimmer: Boolean = true,
    customNativeLayoutResId: Int? = null,
    customShimmerLayoutResId: Int? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
): NativeAd
```

**Native Types:**
```kotlin
enum class NativeType {
    SMALL,       // Small card (72dp height)
    MEDIUM,      // Medium card (140dp height)
    LARGE,       // Large card (380dp height)
    FULL_SCREEN, // Full screen
    CUSTOM       // Custom layout
}
```

---

### App Open Ads

```kotlin
/**
 * Show an app open ad
 * 
 * @param activity The activity to show ad in
 * @param showLoadingDialog Whether to show loading dialog
 * @param onAdDismissed Callback when ad is dismissed
 * @param onAdFailed Callback if ad fails
 * @return true if ad was shown, false if not available
 */
fun showAppOpenAd(
    activity: Activity,
    showLoadingDialog: Boolean = false,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
): Boolean
```

---

### Cleanup

```kotlin
/**
 * Destroy all ads and clean up resources
 * Call in Activity.onDestroy()
 */
fun destroyAds()
```

---

## Ad Types

### BannerAd

```kotlin
interface BannerAd {
    // Lifecycle
    fun loadAd()
    fun destroy()
    
    // Display
    fun getAdView(): AdView?
    fun attachToContainer(container: ViewGroup): Boolean
    fun getContainer(): ViewGroup?
    
    // State
    fun isLoaded(): Boolean
    
    // Lifecycle control
    fun pause()
    fun resume()
    
    // Listeners & Events
    fun setListener(listener: AdListener?)
    fun getEvents(): Flow<AdEvent>
}
```

---

### InterstitialAd

```kotlin
interface InterstitialAd {
    // Loading
    fun loadAd()
    fun showAd(activity: Activity?)
    fun showIfLoaded(): Boolean
    
    // State
    fun isLoaded(): Boolean
    
    // Cleanup
    fun destroy()
    
    // Listeners & Events
    fun setListener(listener: AdListener?)
    fun getEvents(): Flow<AdEvent>
}
```

---

### NativeAd

```kotlin
interface NativeAd {
    // Loading
    fun loadAd()
    
    // Display
    fun inflateNativeAdView(
        container: ViewGroup,
        layoutResId: Int? = null
    ): NativeAdView?
    
    // Info
    fun getNativeType(): NativeType
    fun getExpectedHeight(context: Context): Int
    
    // State
    fun isLoaded(): Boolean
    
    // Cleanup
    fun destroy()
    
    // Listeners & Events
    fun setListener(listener: AdListener?)
    fun getEvents(): Flow<AdEvent>
}
```

---

### AppOpenAd

```kotlin
interface AppOpenAd {
    // Loading
    fun loadAd()
    fun showAd(activity: Activity?)
    fun showIfAvailable(activity: Activity): Boolean
    
    // State
    fun isLoaded(): Boolean
    
    // Cleanup
    fun destroy()
    
    // Listeners & Events
    fun setListener(listener: AdListener?)
    fun getEvents(): Flow<AdEvent>
}
```

---

## Callbacks

### AdListener Interface

```kotlin
interface AdListener {
    /**
     * Called when ad starts loading
     */
    fun onAdLoading()
    
    /**
     * Called when ad finishes loading successfully
     */
    fun onAdLoaded()
    
    /**
     * Called when ad fails to load
     */
    fun onAdFailedToLoad(error: String)
    
    /**
     * Called when user clicks on ad
     */
    fun onAdClicked()
    
    /**
     * Called when ad impression is recorded
     */
    fun onAdImpression()
    
    /**
     * Called when interstitial/app open ad is dismissed
     */
    fun onAdDismissed()
    
    /**
     * Called when ad fails to show
     */
    fun onAdFailedToShow(error: String)
    
    /**
     * Called when ad generates revenue
     */
    fun onAdRevenue(
        valueMicros: Long,
        currencyCode: String,
        precision: Int
    )
}
```

**Example:**
```kotlin
val ad = AdsManager.getInterstitialAd(this)

ad.setListener(object : AdListener {
    override fun onAdLoading() {
        Log.d("Ad", "⏳ Loading...")
    }
    
    override fun onAdLoaded() {
        Log.d("Ad", "✓ Loaded")
        ad.showAd(this@MainActivity)
    }
    
    override fun onAdFailedToLoad(error: String) {
        Log.e("Ad", "✗ Failed: $error")
    }
    
    override fun onAdClicked() {
        Log.d("Ad", "Clicked")
    }
    
    override fun onAdImpression() {
        Log.d("Ad", "Impression")
    }
    
    override fun onAdDismissed() {
        Log.d("Ad", "Dismissed")
    }
    
    override fun onAdFailedToShow(error: String) {
        Log.e("Ad", "Show failed: $error")
    }
    
    override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {
        Log.d("Ad", "Revenue: $valueMicros $currencyCode")
    }
})

ad.loadAd()
```

---

## Configuration

### AdsConfig

```kotlin
/**
 * Configuration for AdsManager
 */
data class AdsConfig(
    // AdMob Configuration
    val isTestMode: Boolean = false,
    val enableDebugLogging: Boolean = false,
    val enableConsentForm: Boolean = false,

    // Ad Unit IDs
    val bannerAdUnitId: String = "",
    val interstitialAdUnitId: String = "",
    val nativeAdUnitId: String = "",
    val appOpenAdUnitId: String = "",
    val rewardedAdUnitId: String = "",

    // App Open Ad Configuration
    val appOpenAdEnabled: Boolean = true,
    val showAppOpenOnFirstLaunch: Boolean = false,
    val minBackgroundTimeForAppOpen: Long = 2000L,
    
    // Custom callback for app open control
    val shouldShowAppOpenAd: ((activity: Activity?) -> Boolean) = { activity ->
        activity != null
    }
)
```

---

## Events

### AdEvent Hierarchy

```kotlin
sealed class AdEvent {
    data class Loaded(
        val adType: AdType,
        val adId: String
    ) : AdEvent()
    
    data class Failed(
        val adType: AdType,
        val error: CustomAdError,
        val adId: String
    ) : AdEvent()
    
    data class Clicked(
        val adType: AdType,
        val adId: String
    ) : AdEvent()
    
    data class Impression(
        val adType: AdType,
        val adId: String
    ) : AdEvent()
    
    data class Dismissed(
        val adType: AdType,
        val adId: String
    ) : AdEvent()
    
    data class Opened(
        val adType: AdType,
        val adId: String
    ) : AdEvent()
    
    data class Closed(
        val adType: AdType,
        val adId: String
    ) : AdEvent()
    
    data class Revenue(
        val adType: AdType,
        val valueMicros: Long,
        val currencyCode: String,
        val precisionType: Int,
        val adId: String
    ) : AdEvent()
}
```

**Example using Flow:**
```kotlin
lifecycleScope.launch {
    val ad = AdsManager.getNativeAd(this@MainActivity)
    
    ad.getEvents().collect { event ->
        when (event) {
            is AdEvent.Loaded -> {
                Log.d("Ad", "✓ ${event.adType} loaded")
            }
            is AdEvent.Failed -> {
                Log.e("Ad", "✗ ${event.adType} failed: ${event.error}")
            }
            is AdEvent.Clicked -> {
                Log.d("Ad", "${event.adType} clicked")
            }
            is AdEvent.Impression -> {
                Log.d("Ad", "${event.adType} impression")
            }
            is AdEvent.Revenue -> {
                Log.d("Ad", "Revenue: ${event.valueMicros}")
            }
            else -> {}
        }
    }
}
```

---

**For complete examples and usage patterns, see README.md**
