AdsManager Library
A modern, lightweight Android Ads Management Library for Google AdMob that simplifies ad integration with built-in support for Banner, Interstitial, Native, and App Open ads. Features a clean API, automatic lifecycle management, and smart ad preloading.

✨ Features
✅ Multiple Ad Formats: Banner, Interstitial, Native, App Open

✅ Smart App Open Ads: Automatic background-to-foreground detection

✅ Pre-built Ad Types: Multiple Native ad layouts (Small, Medium, Full Screen, Custom)

✅ Built-in Shimmer Effects: Loading placeholders for better UX

✅ Lifecycle Management: Automatic pause/resume/destroy

✅ Test Mode: Built-in test ad IDs for development

✅ Debug Tools: Built-in debug information dialog

✅ Edge-to-Edge: Modern UI with edge-to-edge support

✅ Coroutine Support: Async ad initialization and loading

📦 Installation
Add to your project
Add the dependency to your app-level build.gradle.kts:

kotlin
dependencies {
    implementation("com.github.theankitparmar:adsmanager:2.0.0")
}
Or if using a local module:

kotlin
dependencies {
    implementation(project(":ads-manager"))
}
🔐 Permissions
Ensure your app has these permissions in AndroidManifest.xml:

xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
🧩 Google AdMob Setup
Add your AdMob App ID
In your AndroidManifest.xml:

xml
<manifest>
    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
    </application>
</manifest>
Get your Ad Unit IDs
Get your ad unit IDs from the AdMob Console.

🚀 Quick Start
1) Initialize AdsManager
Initialize in your Application class:

kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ONE simple initialization call
        AdsManager.initialize(
            application = this,
            config = AdsConfig(
                isTestMode = BuildConfig.DEBUG,
                enableDebugLogging = BuildConfig.DEBUG,

                // Ad Unit IDs (test IDs for debug mode)
                bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111",
                interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712",
                nativeAdUnitId = "ca-app-pub-3940256099942544/2247696110",
                appOpenAdUnitId = "ca-app-pub-3940256099942544/9257395921",
                rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917",

                // App Open Ad customization
                appOpenAdEnabled = true,
                showAppOpenOnFirstLaunch = false,
                minBackgroundTimeForAppOpen = 2000L, // 2 seconds

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
}
2) Create Your Main Activity
Extend BaseAdsActivity for automatic ad lifecycle management:

kotlin
class MainActivity : BaseAdsActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupAdsButtons()
    }
    
    private fun setupAdsButtons() {
        binding.btnLoadBanner.setOnClickListener {
            loadBannerAd()
        }
        
        binding.btnShowInterstitial.setOnClickListener {
            showInterstitialAd()
        }
        
        binding.btnLoadNative.setOnClickListener {
            loadNativeAd()
        }
        
        binding.btnTestAppOpen.setOnClickListener {
            testAppOpenAd()
        }
    }
    
    // ... ad loading methods
}
3) Load Banner Ad
kotlin
private fun loadBannerAd() {
    AdHelper.showBannerAd(
        context = this,
        container = binding.bannerAdContainer,
        bannerAdSize = AdHelper.BannerAdSize.STANDARD,
        showShimmer = true,
        onAdLoaded = {
            // Banner ad loaded successfully
            showSnackbar("Banner ad loaded successfully")
        },
        onAdFailed = { error ->
            // Handle error
            showSnackbar("Banner ad failed: $error", true)
        }
    )
}
4) Show Interstitial Ad
kotlin
private fun showInterstitialAd() {
    AdHelper.showInterstitialAd(
        activity = this,
        showLoadingDialog = false,
        onAdDismissed = {
            // Ad dismissed, continue your flow
            showSnackbar("Interstitial ad dismissed")
        },
        onAdFailed = { error ->
            // Handle error
            showSnackbar("Interstitial failed: $error", true)
        }
    )
}
5) Load Native Ad
kotlin
private fun loadNativeAd() {
    AdHelper.showNativeAd(
        context = this,
        container = binding.nativeAdContainer,
        nativeType = NativeType.MEDIUM,
        showShimmer = true,
        onAdLoaded = {
            // Native ad loaded successfully
            showSnackbar("Native ad loaded successfully")
        },
        onAdFailed = { error ->
            // Handle error
            showSnackbar("Native ad failed: $error", true)
        }
    )
}
6) Test App Open Ad
kotlin
private fun testAppOpenAd() {
    AdHelper.showAppOpenAd(
        activity = this,
        showLoadingDialog = false,
        onAdDismissed = {
            // App Open ad dismissed
            showSnackbar("App Open ad dismissed")
        },
        onAdFailed = { error ->
            // Handle error
            showSnackbar("App Open failed: $error", true)
        }
    )
}
🎨 Ad Types & Customization
Banner Ad Sizes
kotlin
enum class BannerAdSize {
    STANDARD,      // 320x50
    LARGE,         // 320x100
    MEDIUM_RECTANGLE, // 300x250
    FULL_BANNER,   // 468x60
    LEADERBOARD,   // 728x90
    ADAPTIVE       // Automatically adjusts
}

// Usage
AdHelper.showBannerAd(
    context = this,
    container = bannerContainer,
    bannerAdSize = AdHelper.BannerAdSize.ADAPTIVE, // or any other size
    // ... other parameters
)
Native Ad Types
kotlin
enum class NativeType {
    SMALL,      // Compact native ad
    MEDIUM,     // Medium size (recommended)
    LARGE,      // Large native ad
    FULL_SCREEN,// Full screen native ad
    CUSTOM      // Custom layout
}

// Usage with predefined type
AdHelper.showNativeAd(
    context = this,
    container = nativeContainer,
    nativeType = NativeType.MEDIUM,
    // ... other parameters
)

// Usage with custom layout
AdHelper.showNativeAd(
    context = this,
    container = nativeContainer,
    nativeType = NativeType.CUSTOM,
    customNativeLayoutResId = R.layout.custom_native_ad,
    customShimmerLayoutResId = R.layout.custom_shimmer,
    // ... other parameters
)
🔧 Advanced Usage
Manual Ad Management
kotlin
// Get ad instance directly from AdsManager
val bannerAd = AdsManager.getBannerAd(context, AdSize.BANNER)
val interstitialAd = AdsManager.getInterstitialAd(context)
val nativeAd = AdsManager.getNativeAd(context)

// Check if AdsManager is initialized
if (AdsManager.isInitialized()) {
    // Ads are ready to use
}

// Wait for initialization (coroutine)
scope.launch {
    AdsManager.awaitInitialization()
    // Now safe to load ads
}
App Open Ad Configuration
kotlin
// In your AdsConfig:
AdsConfig(
    appOpenAdEnabled = true,
    showAppOpenOnFirstLaunch = false, // Don't show on first launch
    minBackgroundTimeForAppOpen = 2000L, // 2 seconds minimum
    
    // Exclude specific activities from showing app open ads
    shouldShowAppOpenAd = { activity ->
        activity?.let {
            when (activity) {
                SplashActivity::class -> false
                PaymentActivity::class -> false
                else -> true
            }
        } ?: true
    }
)
Activity Lifecycle Management
kotlin
override fun onPause() {
    super.onPause()
    AdHelper.pauseBannerAds() // Pause banner ads
}

override fun onResume() {
    super.onResume()
    AdHelper.resumeBannerAds() // Resume banner ads
    // Optional: Show app open ad on resume
    AdsManager.showAppOpenAdOnResume(this)
}

override fun onDestroy() {
    super.onDestroy()
    // Note: Don't destroy all ads in activity destroy
    // They're managed globally by AdsManager
}
Debug Information
kotlin
// Show debug info dialog
private fun showDebugInfo() {
    // Built-in method in MainActivity example
    // Shows: Initialization status, test mode, ad sizes, SDK version
}
📱 Layout Examples
Activity Layout with Ad Containers
xml
<ScrollView
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        
        <!-- Banner Ad Container -->
        <FrameLayout
            android:id="@+id/bannerAdContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:minHeight="50dp"
            android:background="@color/surface"
            android:padding="8dp"/>
        
        <!-- Native Ad Container -->
        <FrameLayout
            android:id="@+id/nativeAdContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:minHeight="120dp"
            android:background="@color/surface"
            android:padding="16dp"/>
        
        <!-- Ad Controls -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">
            
            <Button
                android:id="@+id/btnLoadBanner"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Load Banner Ad"/>
            
            <Button
                android:id="@+id/btnShowInterstitial"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Show Interstitial Ad"/>
            
            <!-- ... more buttons -->
        </LinearLayout>
    </LinearLayout>
</ScrollView>
Custom Native Ad Layout
Create res/layout/custom_native_ad.xml:

xml
<com.google.android.gms.ads.nativead.NativeAdView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/rounded_card"
    android:padding="16dp">
    
    <!-- Header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">
        
        <ImageView
            android:id="@+id/ad_app_icon"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:scaleType="fitCenter"/>
        
        <TextView
            android:id="@+id/ad_headline"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"/>
        
        <TextView
            android:id="@+id/ad_advertiser"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="@color/text_secondary"/>
    </LinearLayout>
    
    <!-- Body -->
    <TextView
        android:id="@+id/ad_body"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:textSize="14sp"
        android:textColor="@color/text_primary"/>
    
    <!-- Media Content -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/ad_media"
        android:layout_width="match_parent"
        android:layout_height="200dp"
        android:layout_marginTop="12dp"
        android:scaleType="fitCenter"
        android:adjustViewBounds="true"/>
    
    <!-- Call to Action -->
    <Button
        android:id="@+id/ad_call_to_action"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:paddingHorizontal="24dp"
        android:paddingVertical="8dp"
        android:textSize="14sp"
        style="@style/Widget.Material3.Button.OutlinedButton"/>
        
</com.google.android.gms.ads.nativead.NativeAdView>
🐛 Troubleshooting
Common Issues
"AdsManager not initialized yet"

Ensure AdsManager.initialize() is called in Application.onCreate()

Wait for initialization using AdsManager.awaitInitialization() in coroutines

Test ads not showing

Verify isTestMode = BuildConfig.DEBUG is set

Check that test device IDs are configured in AdMob console

App Open ads not triggering

Ensure appOpenAdEnabled = true

Check minimum background time (default 2 seconds)

Verify activity is not excluded in shouldShowAppOpenAd callback

Native ad layout issues

Ensure all required view IDs are present in custom layout

Check that container has appropriate height

Enable Debug Logging
kotlin
// In AdsConfig:
AdsConfig(
    isTestMode = BuildConfig.DEBUG,
    enableDebugLogging = BuildConfig.DEBUG, // Enable verbose logging
    // ... other config
)
📊 Best Practices
Test Mode: Always use test ad IDs during development (BuildConfig.DEBUG)

Error Handling: Gracefully handle ad failures without disrupting user experience

Frequency Capping: Don't show ads too frequently; respect user experience

Lifecycle Management: Use pauseBannerAds() and resumeBannerAds() appropriately

Memory Management: Clear ad references in onDestroy() of Application, not Activity

App Open Ads: Exclude sensitive activities (login, payment, etc.)

Native Ads: Use appropriate ad types for different screen sizes

📝 Proguard Rules
If using ProGuard, add these rules to your proguard-rules.pro:

proguard
# Google AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# AdsManager Library
-keep class com.theankitparmar.adsmanager.** { *; }
-keep interface com.theankitparmar.adsmanager.** { *; }

# Shimmer
-keep class com.facebook.shimmer.** { *; }
🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

Fork the repository

Create your feature branch (git checkout -b feature/amazing-feature)

Commit your changes (git commit -m 'Add some amazing feature')

Push to the branch (git push origin feature/amazing-feature)

Open a Pull Request

📄 License
text
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

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
🙏 Acknowledgments
Google AdMob for the ad SDK

Facebook for Shimmer library

Contributors and testers

Open source community

📞 Support
📧 Email: codewithankit056@gmail.com

🐛 Issue Tracker

📖 Documentation Wiki

🔄 Changelog
Version 2.0.0
Complete Rewrite: Modern Kotlin coroutine-based architecture

Smart App Open Ads: Automatic background detection with configurable timing

Multiple Native Types: Pre-built layouts for different use cases

Improved API: Simplified AdHelper with cleaner method signatures

Better Lifecycle: Automatic ad pause/resume with edge cases handled

Debug Tools: Built-in debug information dialog

Edge-to-Edge: Modern UI support with proper insets handling

Version 1.0.0
Initial release with basic ad support

Banner, Interstitial, Native, and App Open ads

Basic lifecycle management

Test mode support
