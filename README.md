# 📱 AdsManager Library - Complete Guide

A **lightweight, production-ready Android Ads Management Library** for **Google AdMob** that simplifies ad integration with built-in support for **Banner**, **Interstitial**, **Native**, and **App Open** ads.

![Version](https://img.shields.io/badge/version-2.0-green) ![Status](https://img.shields.io/badge/status-Production%20Ready-brightgreen) ![Code Quality](https://img.shields.io/badge/code%20quality-★★★★★-blue)

---

## 📚 Table of Contents
- [Features](#-features)
- [Quick Start](#-quick-start)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage Guide](#-usage-guide)
- [API Reference](#-api-reference)
- [Best Practices](#-best-practices)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### Core Features
- ✅ **Multiple Ad Formats**: Banner, Interstitial, Native, App Open
- ✅ **Easy Integration**: Simple, intuitive API
- ✅ **Automatic Preloading**: Ads preload in background
- ✅ **Built-in Retry Logic**: Exponential backoff on failures
- ✅ **Event System**: Reactive event emission
- ✅ **Lifecycle Management**: Automatic pause/resume/destroy
- ✅ **Memory Safe**: Weak references prevent leaks
- ✅ **Production Ready**: Comprehensive error handling

### Advanced Features
- 🔄 **Auto Retry**: Configurable retry policies with exponential backoff
- 🧩 **Flexible API**: Both callback and reactive patterns
- 📊 **Revenue Tracking**: Capture ad revenue events
- 🎯 **Targeting**: Smart activity exclusion for app open ads
- 📝 **Logging**: Detailed logs for debugging
- 🔐 **GDPR Ready**: Consent form support
- 🧪 **Test Mode**: Built-in test ad IDs

---

## 🚀 Quick Start

### 1️⃣ Add Dependency

```kotlin
dependencies {
    implementation("com.github.theankitparmar:adsmanager:2.0.0")
}
```

### 2️⃣ Initialize in Application Class

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = AdsConfig(
            isTestMode = BuildConfig.DEBUG,
            enableDebugLogging = true,
            bannerAdUnitId = if (isTestMode) TEST_BANNER_ID else PROD_BANNER_ID,
            interstitialAdUnitId = if (isTestMode) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID,
            nativeAdUnitId = if (isTestMode) TEST_NATIVE_ID else PROD_NATIVE_ID,
            appOpenAdUnitId = if (isTestMode) TEST_APPOPEN_ID else PROD_APPOPEN_ID,
            appOpenAdEnabled = true
        )

        AdsManager.initialize(this, config)
    }

    companion object {
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        private const val TEST_APPOPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    }
}
```

### 3️⃣ Use in Activity

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Show Banner Ad
        AdHelper.showBannerAd(
            context = this,
            container = findViewById(R.id.banner_container),
            bannerAdSize = AdHelper.BannerAdSize.STANDARD,
            showShimmer = true,
            onAdLoaded = { Log.d("Ad", "✓ Banner loaded") },
            onAdFailed = { error -> Log.e("Ad", "✗ Failed: $error") }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        AdHelper.destroyAds() // Clean up
    }
}
```

---

## 📦 Installation

### Prerequisites
- Android 5.0+ (API level 21)
- AndroidX support libraries
- Google Mobile Ads SDK 20.0+

### Step-by-Step Installation

1. **Add to settings.gradle**
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

2. **Add to build.gradle (App)**
```gradle
dependencies {
    implementation("com.github.theankitparmar:adsmanager:2.0.0")
    
    // Required dependencies
    implementation("com.google.android.gms:play-services-ads:22.0.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
}
```

3. **Add Permissions to AndroidManifest.xml**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

4. **Add AdMob App ID to AndroidManifest.xml**
```xml
<manifest>
    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
    </application>
</manifest>
```

---

## ⚙️ Configuration

### AdsConfig Options

```kotlin
val config = AdsConfig(
    // AdMob Configuration
    isTestMode = true,                      // Use test ads
    enableDebugLogging = true,              // Show detailed logs
    enableConsentForm = false,              // GDPR consent form

    // Ad Unit IDs
    bannerAdUnitId = "YOUR_BANNER_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_ID",
    nativeAdUnitId = "YOUR_NATIVE_ID",
    appOpenAdUnitId = "YOUR_APPOPEN_ID",
    rewardedAdUnitId = "YOUR_REWARDED_ID",

    // App Open Ad Configuration
    appOpenAdEnabled = true,                // Show app open ads
    showAppOpenOnFirstLaunch = false,       // Show on first launch
    minBackgroundTimeForAppOpen = 2000L,    // Min background time (ms)
    
    // Custom callback for app open control
    shouldShowAppOpenAd = { activity ->
        activity != null  // Show for all activities
    }
)

AdsManager.initialize(this, config)
```

---

## 📖 Usage Guide

### Banner Ads

#### Basic Usage
```kotlin
AdHelper.showBannerAd(
    context = this,
    container = findViewById(R.id.banner_container),
    bannerAdSize = AdHelper.BannerAdSize.STANDARD,  // 320x50
    showShimmer = true,  // Loading animation
    onAdLoaded = {
        Log.d("Banner", "✓ Ad loaded and displayed")
    },
    onAdFailed = { error ->
        Log.e("Banner", "✗ Ad failed: $error")
    }
)
```

#### Available Banner Sizes
```kotlin
AdHelper.BannerAdSize.STANDARD           // 320x50
AdHelper.BannerAdSize.LARGE              // 320x100
AdHelper.BannerAdSize.MEDIUM_RECTANGLE   // 300x250
AdHelper.BannerAdSize.FULL_BANNER        // 468x60
AdHelper.BannerAdSize.LEADERBOARD        // 728x90
AdHelper.BannerAdSize.ADAPTIVE           // Auto-adjust
```

#### Manual Container Attachment
```kotlin
val bannerAd = AdsManager.getBannerAd(this, AdSize.BANNER)

bannerAd.setListener(object : AdListener {
    override fun onAdLoaded() {
        // Attach to container after loading
        if (bannerAd.attachToContainer(myContainer)) {
            Log.d("Banner", "✓ Attached to container")
        }
    }
    // ... other callbacks
})

bannerAd.loadAd()
```

---

### Interstitial Ads

#### Show with Loading Dialog
```kotlin
AdHelper.showInterstitialAd(
    activity = this,
    showLoadingDialog = true,
    onAdDismissed = {
        Log.d("Interstitial", "✓ Ad dismissed")
        // Continue with flow
    },
    onAdFailed = { error ->
        Log.e("Interstitial", "✗ Failed: $error")
        // Continue without ad
    }
)
```

#### Show If Already Loaded
```kotlin
val interstitialAd = AdHelper.getReusableInterstitialAd(this)

if (interstitialAd.showIfLoaded()) {
    Log.d("Interstitial", "✓ Ad shown immediately")
} else {
    Log.d("Interstitial", "⏳ Loading ad...")
    AdHelper.showInterstitialAd(this)
}
```

---

### Native Ads

#### Show Native Ad
```kotlin
AdHelper.showNativeAd(
    context = this,
    container = findViewById(R.id.native_container),
    nativeType = NativeType.MEDIUM,  // Small, Medium, Large
    showShimmer = true,
    onAdLoaded = {
        Log.d("Native", "✓ Native ad loaded")
    },
    onAdFailed = { error ->
        Log.e("Native", "✗ Failed: $error")
    }
)
```

#### Custom Native Layout
```kotlin
AdHelper.showNativeAd(
    context = this,
    container = findViewById(R.id.native_container),
    nativeType = NativeType.CUSTOM,
    customNativeLayoutResId = R.layout.my_native_ad_layout,
    customShimmerLayoutResId = R.layout.my_shimmer_layout
)
```

---

### App Open Ads

#### Automatic Background Handling
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Automatically show app open ad when returning from background
    AdsManager.showAppOpenAdOnResume(this)
}
```

#### Manual Control
```kotlin
override fun onResume() {
    super.onResume()
    
    val wasAdShown = AdHelper.showAppOpenAd(
        activity = this,
        showLoadingDialog = false,
        onAdDismissed = {
            startMainActivity()
        },
        onAdFailed = { error ->
            Log.e("AppOpen", "Failed: $error")
            startMainActivity()
        }
    )
    
    if (!wasAdShown) {
        startMainActivity()
    }
}

private fun startMainActivity() {
    startActivity(Intent(this, MainActivity::class.java))
    finish()
}
```

#### Exclude Activities
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Don't show app open ads for these activities
    AdsManager.excludeActivityFromAppOpenAd(LoginActivity::class.java)
    AdsManager.excludeActivityFromAppOpenAd(SplashActivity::class.java)
}
```

---

## 🔌 API Reference

### AdsManager (Main Class)

```kotlin
// Initialize
fun initialize(application: Application, config: AdsConfig, onInitialized: (() -> Unit)? = null)

// Get Ads
fun getBannerAd(context: Context, adSize: AdSize): BannerAd
fun getInterstitialAd(context: Context): InterstitialAd
fun getNativeAd(context: Context): NativeAd
fun getAppOpenAd(context: Context): AppOpenAd

// Get with Type
fun getNativeAdWithType(context: Context, nativeType: NativeType): NativeAd

// Status
fun isInitialized(): Boolean
suspend fun awaitInitialization()

// Lifecycle
fun destroyAllAds()
fun showAppOpenAdOnResume(activity: Activity)
fun excludeActivityFromAppOpenAd(activityClass: Class<*>)
fun includeActivityForAppOpenAd(activityClass: Class<*>)
```

### AdHelper (Helper Class)

```kotlin
// Banner Ads
fun showBannerAd(
    context: Context,
    container: ViewGroup,
    bannerAdSize: BannerAdSize = BannerAdSize.STANDARD,
    showShimmer: Boolean = true,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
): BannerAd

// Interstitial Ads
fun showInterstitialAd(
    activity: Activity,
    showLoadingDialog: Boolean = true,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
)

fun getReusableInterstitialAd(activity: Activity): InterstitialAd

// Native Ads
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

// App Open Ads
fun showAppOpenAd(
    activity: Activity,
    showLoadingDialog: Boolean = false,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailed: ((error: String) -> Unit)? = null
): Boolean

// Lifecycle
fun destroyAds()
fun pauseBannerAds()
fun resumeBannerAds()
```

---

## 🎯 Best Practices

### ✅ DO

```kotlin
// DO: Check if ad is loaded before showing
if (interstitialAd.isLoaded()) {
    interstitialAd.showAd(this)
}

// DO: Always clean up resources
override fun onDestroy() {
    super.onDestroy()
    bannerAd?.destroy()
    AdHelper.destroyAds()
}

// DO: Use test ad IDs in debug mode
val config = AdsConfig(
    isTestMode = BuildConfig.DEBUG,
    bannerAdUnitId = if (isTestMode) TEST_BANNER else PROD_BANNER
)

// DO: Enable debug logging during development
val config = AdsConfig(
    enableDebugLogging = true
)

// DO: Handle errors gracefully
onAdFailed = { error ->
    Log.e("Ad", "Error: $error")
    // Continue without ad
}
```

### ❌ DON'T

```kotlin
// DON'T: Show ads too frequently (annoys users)
// Check native ad properties before showing

// DON'T: Use production ad IDs in debug builds
// Results in "invalid activity" errors from Google

// DON'T: Ignore error callbacks
onAdFailed = null  // BAD!

// DON'T: Keep ads in memory forever
// Always call destroy() in onDestroy()

// DON'T: Show ads on background threads
// Use Main dispatcher always

// DON'T: Block UI waiting for ads to load
// Use callbacks/observers instead
```

---

## 🔧 Event Listening

### Callback Pattern
```kotlin
val interstitialAd = AdsManager.getInterstitialAd(this)

interstitialAd.setListener(object : AdListener {
    override fun onAdLoading() { }
    override fun onAdLoaded() { }
    override fun onAdFailedToLoad(error: String) { }
    override fun onAdClicked() { }
    override fun onAdImpression() { }
    override fun onAdDismissed() { }
    override fun onAdFailedToShow(error: String) { }
    override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) { }
})
```

### Reactive Pattern (Flow)
```kotlin
lifecycleScope.launch {
    nativeAd.getEvents().collect { event ->
        when (event) {
            is AdEvent.Loaded -> Log.d("Ad", "✓ Loaded")
            is AdEvent.Failed -> Log.e("Ad", "✗ Failed: ${event.error}")
            is AdEvent.Clicked -> Log.d("Ad", "Clicked")
            is AdEvent.Impression -> Log.d("Ad", "Impression")
            is AdEvent.Revenue -> Log.d("Ad", "Revenue: ${event.valueMicros}")
            else -> {}
        }
    }
}
```

---

## 🐛 Troubleshooting

### Issue: "Ad not loading"
**Solution:**
```kotlin
// 1. Check internet connectivity
// 2. Verify ad unit IDs
// 3. Check logs for error reason
// 4. Enable debug logging to see details
val config = AdsConfig(enableDebugLogging = true)
```

### Issue: "Ad not displaying"
**Solution (for banner ads):**
```kotlin
// Banner ads must be attached to container
val bannerAd = AdsManager.getBannerAd(this)
bannerAd.setListener(object : AdListener {
    override fun onAdLoaded() {
        bannerAd.attachToContainer(container)
    }
})
```

### Issue: "Crashes with NullPointerException"
**Solution:**
```kotlin
// Always check if context/activity is valid
override fun onDestroy() {
    super.onDestroy()
    try {
        bannerAd?.destroy()
    } catch (e: Exception) {
        Log.e("Ad", "Error: ${e.message}")
    }
}
```

### Issue: "No logs appearing"
**Solution:**
```kotlin
// Enable debug logging
val config = AdsConfig(
    enableDebugLogging = true
)
AdsManager.initialize(this, config)
```

---

## 📊 Debug Logging

Enable detailed logging to see what's happening:

```kotlin
val config = AdsConfig(
    isTestMode = true,
    enableDebugLogging = true
)
AdsManager.initialize(this, config)

// Expected logs:
// 🚀 Starting AdsManager initialization...
// ✅ AdMob SDK initialized successfully
// ⏳ Starting ad preloading in background...
// ✓ Banner ad loaded successfully
// ✗ Interstitial ad failed to load: NETWORK_ERROR
// Retrying ad in 2000ms (attempt 1/3)
```

---

## 📋 Lifecycle Management

### Activity Lifecycle

```kotlin
class MainActivity : AppCompatActivity() {
    private var bannerAd: BannerAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        bannerAd = AdHelper.showBannerAd(
            context = this,
            container = findViewById(R.id.banner_container)
        )
    }

    override fun onPause() {
        super.onPause()
        bannerAd?.pause()
    }

    override fun onResume() {
        super.onResume()
        bannerAd?.resume()
        AdsManager.showAppOpenAdOnResume(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerAd?.destroy()
        AdHelper.destroyAds()
    }
}
```

---

## 🔐 Permissions

Required permissions in AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Optional (for improved targeting):
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## 📞 Support

### Documentation Files
- 📖 `UPDATED_USAGE_GUIDE.md` - Complete usage guide
- 🐛 `BUG_ANALYSIS.md` - Issue analysis
- ✅ `FIXES_SUMMARY.md` - What was fixed
- 🔄 `BEFORE_AFTER_COMPARISON.md` - Code examples
- 📋 `PROJECT_COMPLETION_REPORT.md` - Full report

### Common Resources
- **Issue Tracker:** Check GitHub issues
- **Documentation:** See docs folder
- **Examples:** Check sample activities

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

```text
MIT License

Copyright (c) 2024 Ankit Parmar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🙏 Acknowledgments

- **Google AdMob** for the advertising SDK
- **Facebook Shimmer** for loading effects
- **AndroidX** team for modern Android components
- All contributors and testers

---

## 📞 Contact

- **Email:** codewithankit056@gmail.com
- **GitHub:** [@theankitparmar](https://github.com/theankitparmar)
- **Twitter:** [@theankitparmar](https://twitter.com/theankitparmar)

---

## ✨ Version History

### v2.0 (Current) - 🎉 Production Ready
- ✅ Fixed all critical bugs
- ✅ Added comprehensive documentation
- ✅ Enhanced error handling
- ✅ Improved logging throughout
- ✅ Better API design

### v1.0 - Initial Release
- Basic ad support
- Core functionality

---

**Last Updated:** March 4, 2026  
**Status:** ✅ Production Ready  
**Code Quality:** ⭐⭐⭐⭐⭐ (5/5)

---

Made with ❤️ by Ankit Parmar