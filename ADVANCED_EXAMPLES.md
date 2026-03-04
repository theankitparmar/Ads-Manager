# 💡 AdsManager - Advanced Examples

Advanced usage patterns and examples for AdsManager library.

---

## Table of Contents
- [Multiple Banner Ads](#multiple-banner-ads)
- [Custom Native Ads](#custom-native-ads)
- [Reactive Programming](#reactive-programming-with-flows)
- [Lifecycle Integration](#lifecycle-integration)
- [Error Handling](#comprehensive-error-handling)
- [Analytics](#analytics-tracking)
- [Performance Optimization](#performance-optimization)

---

## Multiple Banner Ads

Display multiple banner ads in different containers:

```kotlin
class MultiAdsActivity : AppCompatActivity() {
    private var topBannerAd: BannerAd? = null
    private var bottomBannerAd: BannerAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_ads)

        showMultipleBannerAds()
    }

    private fun showMultipleBannerAds() {
        // Top banner - Standard size
        topBannerAd = AdHelper.showBannerAd(
            context = this,
            container = findViewById(R.id.top_banner_container),
            bannerAdSize = AdHelper.BannerAdSize.STANDARD,
            onAdLoaded = {
                Log.d("MultiAds", "✓ Top banner loaded")
            },
            onAdFailed = { error ->
                Log.e("MultiAds", "✗ Top banner failed: $error")
            }
        )

        // Bottom banner - Medium rectangle
        bottomBannerAd = AdHelper.showBannerAd(
            context = this,
            container = findViewById(R.id.bottom_banner_container),
            bannerAdSize = AdHelper.BannerAdSize.MEDIUM_RECTANGLE,
            onAdLoaded = {
                Log.d("MultiAds", "✓ Bottom banner loaded")
            },
            onAdFailed = { error ->
                Log.e("MultiAds", "✗ Bottom banner failed: $error")
            }
        )
    }

    override fun onPause() {
        super.onPause()
        topBannerAd?.pause()
        bottomBannerAd?.pause()
    }

    override fun onResume() {
        super.onResume()
        topBannerAd?.resume()
        bottomBannerAd?.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        topBannerAd?.destroy()
        bottomBannerAd?.destroy()
    }
}
```

---

## Custom Native Ads

Create beautiful custom native ads with your own layout:

### Layout File: layout/custom_native_ad.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/white"
    android:padding="16dp">

    <com.google.android.gms.ads.nativead.NativeAdView
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Header -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <ImageView
                android:id="@+id/ad_icon"
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:scaleType="centerInside"
                android:contentDescription="Ad icon" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical"
                android:marginStart="12dp">

                <TextView
                    android:id="@+id/ad_headline"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textSize="16sp"
                    android:textStyle="bold"
                    android:maxLines="1"
                    android:ellipsize="end" />

                <TextView
                    android:id="@+id/ad_advertiser"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textSize="12sp"
                    android:textColor="@android:color/darker_gray"
                    android:maxLines="1"
                    android:ellipsize="end" />
            </LinearLayout>
        </LinearLayout>

        <!-- Media -->
        <com.google.android.gms.ads.nativead.MediaView
            android:id="@+id/ad_media"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:scaleType="centerCrop"
            android:contentDescription="Ad media"
            android:layout_marginTop="12dp" />

        <!-- Body -->
        <TextView
            android:id="@+id/ad_body"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:layout_marginTop="12dp"
            android:maxLines="3"
            android:ellipsize="end" />

        <!-- Rating -->
        <RatingBar
            android:id="@+id/ad_stars"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:numStars="5"
            android:stepSize="0.5"
            android:isIndicator="true"
            android:layout_marginTop="8dp" />

        <!-- CTA -->
        <Button
            android:id="@+id/ad_call_to_action"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="@android:color/white"
            android:layout_marginTop="12dp"
            android:backgroundTint="@color/blue" />

    </com.google.android.gms.ads.nativead.NativeAdView>
</LinearLayout>
```

### Activity Code

```kotlin
class CustomNativeAdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_native)

        showCustomNativeAd()
    }

    private fun showCustomNativeAd() {
        AdHelper.showNativeAd(
            context = this,
            container = findViewById(R.id.native_container),
            nativeType = NativeType.CUSTOM,
            customNativeLayoutResId = R.layout.custom_native_ad,
            showShimmer = true,
            onAdLoaded = {
                Log.d("CustomNative", "✓ Custom native ad loaded")
            },
            onAdFailed = { error ->
                Log.e("CustomNative", "✗ Custom native ad failed: $error")
            }
        )
    }
}
```

---

## Reactive Programming with Flows

Use Flow for reactive ad event handling:

```kotlin
class ReactiveAdsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reactive)

        observeAdEvents()
    }

    private fun observeAdEvents() {
        val interstitialAd = AdsManager.getInterstitialAd(this)

        // Listen to ad events reactively
        lifecycleScope.launch {
            interstitialAd.getEvents().collect { event ->
                when (event) {
                    is AdEvent.Loaded -> {
                        Log.d("Reactive", "✓ Ad loaded")
                        updateUI("Ad Ready", Color.GREEN)
                        interstitialAd.showAd(this@ReactiveAdsActivity)
                    }

                    is AdEvent.Failed -> {
                        Log.e("Reactive", "✗ Ad failed: ${event.error}")
                        updateUI("Ad Failed", Color.RED)
                    }

                    is AdEvent.Clicked -> {
                        Log.d("Reactive", "Ad clicked")
                        updateUI("Ad Clicked", Color.BLUE)
                    }

                    is AdEvent.Impression -> {
                        Log.d("Reactive", "Ad impression")
                        updateUI("Impression Recorded", Color.GRAY)
                    }

                    is AdEvent.Dismissed -> {
                        Log.d("Reactive", "Ad dismissed")
                        updateUI("Ad Dismissed", Color.YELLOW)
                    }

                    is AdEvent.Revenue -> {
                        Log.d("Reactive", "Revenue: ${event.valueMicros} ${event.currencyCode}")
                        updateUI("Revenue: ${event.valueMicros}", Color.GREEN)
                    }

                    else -> {}
                }
            }
        }

        interstitialAd.loadAd()
    }

    private fun updateUI(message: String, color: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.status_text).apply {
                text = message
                setTextColor(color)
            }
        }
    }
}
```

---

## Lifecycle Integration

Proper lifecycle management across app:

```kotlin
class LifecycleAwareActivity : AppCompatActivity() {
    private var bannerAd: BannerAd? = null
    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupAds()
    }

    private fun setupAds() {
        // Banner ad
        bannerAd = AdHelper.showBannerAd(
            context = this,
            container = findViewById(R.id.banner_container)
        )

        // Interstitial ad (preload)
        interstitialAd = AdsManager.getInterstitialAd(this)
    }

    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "onResume")
        
        // Resume ads
        bannerAd?.resume()
        
        // Show app open ad if available
        AdsManager.showAppOpenAdOnResume(this)
    }

    override fun onPause() {
        super.onPause()
        Log.d("Lifecycle", "onPause")
        
        // Pause ads
        bannerAd?.pause()
    }

    override fun onStop() {
        super.onStop()
        Log.d("Lifecycle", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Lifecycle", "onDestroy")
        
        // Clean up all ads
        bannerAd?.destroy()
        interstitialAd?.destroy()
        AdHelper.destroyAds()
    }
}
```

---

## Comprehensive Error Handling

Handle all possible errors gracefully:

```kotlin
class ErrorHandlingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadInterstitialWithErrorHandling()
    }

    private fun loadInterstitialWithErrorHandling() {
        try {
            val ad = AdsManager.getInterstitialAd(this)

            ad.setListener(object : AdListener {
                override fun onAdLoading() {
                    Log.d("ErrorHandling", "⏳ Loading...")
                    showLoadingState()
                }

                override fun onAdLoaded() {
                    Log.d("ErrorHandling", "✓ Loaded")
                    hideLoadingState()
                }

                override fun onAdFailedToLoad(error: String) {
                    Log.e("ErrorHandling", "✗ Load failed: $error")
                    handleLoadError(error)
                }

                override fun onAdFailedToShow(error: String) {
                    Log.e("ErrorHandling", "✗ Show failed: $error")
                    handleShowError(error)
                }

                override fun onAdClicked() {}
                override fun onAdImpression() {}
                override fun onAdDismissed() {}
                override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
            })

            ad.loadAd()

        } catch (e: IllegalStateException) {
            Log.e("ErrorHandling", "AdsManager not initialized: ${e.message}")
            showErrorDialog("Ads not available", "Please try again later")

        } catch (e: Exception) {
            Log.e("ErrorHandling", "Unexpected error: ${e.message}", e)
            showErrorDialog("Error", "Something went wrong: ${e.message}")
        }
    }

    private fun handleLoadError(error: String) {
        when {
            error.contains("NETWORK_ERROR") -> {
                showErrorDialog("Network Error", "Please check your internet connection")
            }
            error.contains("TIMEOUT") -> {
                showErrorDialog("Timeout", "Ad loading took too long. Please try again.")
            }
            else -> {
                showErrorDialog("Error", error)
            }
        }
    }

    private fun handleShowError(error: String) {
        Log.e("ErrorHandling", "Failed to show ad: $error")
        // Continue without showing ad
    }

    private fun showLoadingState() {
        findViewById<View>(R.id.loading_progress)?.visibility = View.VISIBLE
    }

    private fun hideLoadingState() {
        findViewById<View>(R.id.loading_progress)?.visibility = View.GONE
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
```

---

## Analytics Tracking

Track ad events for analytics:

```kotlin
class AnalyticsActivity : AppCompatActivity() {
    private val analyticsHelper = AdAnalyticsHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        trackAdEvents()
    }

    private fun trackAdEvents() {
        val ad = AdsManager.getNativeAd(this)

        ad.setListener(object : AdListener {
            override fun onAdLoading() {
                analyticsHelper.trackEvent("ad_loading", "native")
            }

            override fun onAdLoaded() {
                analyticsHelper.trackEvent("ad_loaded", "native")
            }

            override fun onAdFailedToLoad(error: String) {
                analyticsHelper.trackEvent("ad_failed", mapOf(
                    "type" to "native",
                    "error" to error
                ))
            }

            override fun onAdClicked() {
                analyticsHelper.trackEvent("ad_clicked", "native")
            }

            override fun onAdImpression() {
                analyticsHelper.trackEvent("ad_impression", "native")
            }

            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {
                analyticsHelper.trackEvent("ad_revenue", mapOf(
                    "type" to "native",
                    "value" to valueMicros,
                    "currency" to currencyCode
                ))
            }

            override fun onAdDismissed() {}
            override fun onAdFailedToShow(error: String) {}
        })

        ad.loadAd()
    }
}

// Analytics helper
class AdAnalyticsHelper {
    fun trackEvent(eventName: String, data: String) {
        Log.d("Analytics", "Event: $eventName, Data: $data")
        // Send to Firebase Analytics, Mixpanel, etc.
    }

    fun trackEvent(eventName: String, data: Map<String, Any>) {
        Log.d("Analytics", "Event: $eventName, Data: $data")
        // Send to Firebase Analytics, Mixpanel, etc.
    }
}
```

---

## Performance Optimization

Optimize ad loading and display:

```kotlin
class OptimizedAdsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        optimizeAdLoading()
    }

    private fun optimizeAdLoading() {
        // Load ads in background
        Thread {
            // Preload interstitial ad early
            val interstitialAd = AdsManager.getInterstitialAd(this)
            Log.d("Optimized", "Interstitial ad preloading started")

            runOnUiThread {
                // Show banner ad on UI thread
                AdHelper.showBannerAd(
                    context = this,
                    container = findViewById(R.id.banner_container),
                    bannerAdSize = AdHelper.BannerAdSize.ADAPTIVE,
                    showShimmer = true
                )
            }
        }.start()
    }

    // Show interstitial only when appropriate
    fun showInterstitialAtRightTime() {
        val ad = AdsManager.getInterstitialAd(this)

        // Check if ad is ready before showing
        if (ad.isLoaded()) {
            ad.showAd(this)
        } else {
            Log.d("Optimized", "Ad not ready yet, will retry")
            // Retry after delay
            Handler(Looper.getMainLooper()).postDelayed({
                if (ad.isLoaded()) {
                    ad.showAd(this)
                }
            }, 2000)
        }
    }
}
```

---

## Configuration Best Practices

```kotlin
class OptimizedApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Build config for production
        val config = AdsConfig(
            // Development settings
            isTestMode = BuildConfig.DEBUG,
            enableDebugLogging = BuildConfig.DEBUG,

            // Ad unit IDs (use BuildConfig variants)
            bannerAdUnitId = BuildConfig.BANNER_AD_UNIT_ID,
            interstitialAdUnitId = BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            nativeAdUnitId = BuildConfig.NATIVE_AD_UNIT_ID,
            appOpenAdUnitId = BuildConfig.APPOPEN_AD_UNIT_ID,

            // App open settings
            appOpenAdEnabled = true,
            showAppOpenOnFirstLaunch = false,
            minBackgroundTimeForAppOpen = 2000L,

            // Custom control
            shouldShowAppOpenAd = { activity ->
                activity != null && shouldShowAdForActivity(activity)
            }
        )

        AdsManager.initialize(this, config)
    }

    private fun shouldShowAdForActivity(activity: Activity): Boolean {
        // Don't show ads on certain activities
        return activity !is LoginActivity && 
               activity !is SplashActivity &&
               activity !is SettingsActivity
    }
}
```

---

For more examples and patterns, check README.md and API_REFERENCE.md
