# AdsManager Library

A lightweight, easy-to-use **Android Ads Management Library** for **Google AdMob** that simplifies ad integration with built-in support for **Banner**, **Interstitial**, **Native**, and **App Open** ads.

---

## 📱 Features

- ✅ **Multiple Ad Formats**: Banner, Interstitial, Native, App Open  
- ✅ **Easy Integration**: Simple API for showing ads  
- ✅ **Built-in Shimmer Effects**: Loading placeholders for better UX  
- ✅ **Auto Retry**: Configurable retry policies for failed ads  
- ✅ **Test Mode**: Built-in test ad IDs for development  
- ✅ **Lifecycle Management**: Automatic pause/resume/destroy  
- ✅ **Consent Form Support**: GDPR compliance ready  

---

## 📦 Installation

### Add to your project

Add the dependency to your **app-level** `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.theankitparmar:adsmanager:1.0.0")
}
```

Or if using a local module:

```kotlin
dependencies {
    implementation(project(":ads-manager"))
}
```

------

## 🔐 Permissions

Ensure your app has these permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

-----

## 🧩 Google AdMob Setup
### Add your AdMob App ID

In your `AndroidManifest.xml`:

```xml
<manifest>
    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
    </application>
</manifest>
```
Get your Ad Unit IDs
Get your ad unit IDs from the AdMob Console.
-----

## 🚀 Quick Start
### 1) Initialize AdsManager

Initialise in your Application class:
```kotlin 
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val isTestMode = BuildConfig.DEBUG // Use test ads in debug mode

        val adUnits = AdUnits(
            bannerAdUnitId = if (isTestMode) "ca-app-pub-3940256099942544/6300978111" else "YOUR_BANNER_ID",
            interstitialAdUnitId = if (isTestMode) "ca-app-pub-3940256099942544/1033173712" else "YOUR_INTERSTITIAL_ID",
            nativeAdUnitId = if (isTestMode) "ca-app-pub-3940256099942544/2247696110" else "YOUR_NATIVE_ID",
            appOpenAdUnitId = if (isTestMode) "ca-app-pub-3940256099942544/3419835294" else "YOUR_APPOPEN_ID"
        )

        val config = AdsConfiguration(
            isTestMode = isTestMode,
            adUnits = adUnits,
            enableAutoReload = true,
            retryPolicy = RetryPolicy.default,
            enableConsentForm = false // Set true for GDPR regions
        )

        AdsManager.initialize(this, config)
    }
}

```

-------

### 2) Show Banner Ad

In your Activity or Fragment:
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AdHelper.showBannerAd(
            context = this,
            container = findViewById(R.id.banner_container),
            showShimmer = true,
            adSize = AdSize.LARGE_BANNER,
            onAdLoaded = {
                // Ad loaded successfully
            },
            onAdFailed = { error ->
                Log.e("MainActivity", "Banner ad failed: $error")
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        AdHelper.destroyAds() // Clean up ads
    }
}

```

----- 

### 3) Show Interstitial Ad

```kotlin
fun showInterstitialAd() {
    AdHelper.showInterstitialAd(
        activity = this,
        showLoadingDialog = true,
        onAdDismissed = {
            // Ad was dismissed, continue your flow
        },
        onAdFailed = { error ->
            // Ad failed to load, continue without ad
        }
    )
}

```

### 4) Show Native Ad
```kotlin
AdHelper.showNativeAd(
    context = this,
    container = findViewById(R.id.native_container),
    layoutResId = R.layout.native_ad_layout, // Your custom layout
    showShimmer = true,
    onAdLoaded = {
        // Native ad loaded
    },
    onAdFailed = { error ->
        // Handle error
    }
)
```

------

### 5. Show App Open Ad

In your `SplashActivity` or main entry activity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val wasAdShown = AdHelper.showAppOpenAd(
        activity = this,
        showLoadingDialog = false,
        onAdDismissed = {
            // Start main activity after ad
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        },
        onAdFailed = { error ->
            // Start main activity even if ad fails
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    )
    
    if (!wasAdShown) {
        // No ad available, proceed immediately
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

----- 

## 🎨 Customization
### Custom Ad Sizes

```kotlin
// Use different banner sizes
AdHelper.showBannerAd(
    context = this,
    container = bannerContainer,
    adSize = AdSize.BANNER, // Standard banner (320x50)
    // or AdSize.LARGE_BANNER (320x100)
    // or AdSize.MEDIUM_RECTANGLE (300x250)
    // or AdSize.FULL_BANNER (468x60)
    // or AdSize.LEADERBOARD (728x90)
)
```

-------

### Custom Retry Policy

```kotlin
val retryPolicy = RetryPolicy(
    maxRetries = 3,
    retryDelayMillis = 2000L,
    exponentialBackoff = true
)

val config = AdsConfiguration(
    isTestMode = BuildConfig.DEBUG,
    adUnits = adUnits,
    enableAutoReload = true,
    retryPolicy = retryPolicy,
    enableConsentForm = false
)
```

------

## 📱 Layout Examples
### Banner Ad Layout

```kotlin
<LinearLayout
    android:id="@+id/banner_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center"
    android:orientation="vertical"
    android:minHeight="50dp"/>
```

--------


## Native Ad Layout
Create `layout/native_ad_layout.xml`:

```kotlin
<com.google.android.gms.ads.nativead.NativeAdView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/card_background">
    
    <ImageView
        android:id="@+id/ad_app_icon"
        android:layout_width="48dp"
        android:layout_height="48dp"/>
    
    <TextView
        android:id="@+id/ad_headline"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="16sp"
        android:textStyle="bold"/>
    
    <TextView
        android:id="@+id/ad_body"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="14sp"/>
        
    <Button
        android:id="@+id/ad_call_to_action"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
</com.google.android.gms.ads.nativead.NativeAdView>
```

----- 

## 🔧 Advanced Usage
Manual Ad Management

```kotlin
// Get ad instance directly
val bannerAd = AdsManager.getBannerAd(this, AdSize.BANNER)

// Set custom listener
bannerAd.setListener(object : AdListener {
    override fun onAdLoaded() {
        // Handle ad loaded
    }
    
    override fun onAdFailedToLoad(error: String) {
        // Handle error
    }
    
    // ... other callbacks
})

// Manual lifecycle control
override fun onPause() {
    super.onPause()
    AdHelper.pauseBannerAds()
}

override fun onResume() {
    super.onResume()
    AdHelper.resumeBannerAds()
}
```

------

## Preloading Ads
Ads are automatically preloaded during initialisation. To manually preload:

```kotlin
// Preload interstitial ad
AdsManager.getInterstitialAd(applicationContext)

// Preload native ad  
AdsManager.getNativeAd(applicationContext)
```
-----

## 🐛 Troubleshooting
### Common Issues
- "Ad size and ad unit ID must be set before loadAd is called"

    - Ensure you're using test ad IDs in debug mode

    - Verify AdMob initialization completed successfully

- "No ad config" or "AdManager not initialized"

    - Make sure AdsManager.initialize() is called in Application.onCreate()

- Ads not showing in production

    - Verify your ad unit IDs are correct

    - Check if your AdMob account is active

    - Ensure ads are enabled for your app in AdMob console

-----

## Enable Debug Logging
```kotlin
// Add in Application.onCreate()
if (BuildConfig.DEBUG) {
    MobileAds.setRequestConfiguration(
        RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
            .build()
    )
}
```
------

## 📊 Best Practices

1) Test Ads in Debug: Always use test ad IDs during development

2) Handle Failures Gracefully: Ads may fail to load - handle errors silently

3) Respect User Experience: Don't show ads too frequently

4) Lifecycle Management: Always clean up ads in onDestroy()

5) Network Awareness: Check network before loading ads

-----

## 📝 Proguard Rules
### If using ProGuard, add these rules:

```proguard
# Google AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Keep AdsManager classes
-keep class com.theankitparmar.adsmanager.** { *; }
```

-----

## 🤝 Contributing

## Contributions are welcome! Please feel free to submit a Pull Request.

Fork the repository

1. Create your feature branch `(git checkout -b feature/amazing-feature)`

2. Commit your changes `(git commit -m 'Add some amazing feature')`

3. Push to the branch `(git push origin feature/amazing-feature)`

4. Open a Pull Request

-------

📄 License

```text
MIT License

Copyright (c) 2024 Your Name

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including, without limitation, the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

```

------

## 🙏 Acknowledgments
### Google AdMob for the ad SDK

- Contributors and testers

- Open source community

## 📞 Support
- 📧 Email: [codewithankit056@gmail.com]

- 🐛 Issue Tracker

- 📖 Documentation Wiki
