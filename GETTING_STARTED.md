# 🚀 Getting Started - AdsManager Library

A complete guide to get you started with the AdsManager library in 5 minutes!

## Prerequisites

- Android 5.0+ (API level 21)
- AndroidX libraries
- Google Play Services

---

## Step 1: Add Dependencies (2 minutes)

### build.gradle (Project)
```gradle
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### build.gradle (App)
```gradle
dependencies {
    // AdsManager Library
    implementation("com.github.theankitparmar:adsmanager:2.0.0")
    
    // Required dependencies
    implementation("com.google.android.gms:play-services-ads:22.0.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
}
```

---

## Step 2: Add Permissions (1 minute)

### AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application>
        <!-- Add AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>

        <!-- Your Activities -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

## Step 3: Initialize AdsManager (1 minute)

Create or update your Application class:

### MyApplication.kt
```kotlin
import android.app.Application
import com.theankitparmar.adsmanager.core.AdsConfig
import com.theankitparmar.adsmanager.core.AdsManager

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        initializeAdsManager()
    }

    private fun initializeAdsManager() {
        val config = AdsConfig(
            // Basic Configuration
            isTestMode = BuildConfig.DEBUG,
            enableDebugLogging = BuildConfig.DEBUG,
            enableConsentForm = false,

            // Ad Unit IDs - Use test IDs for development
            bannerAdUnitId = if (isTestMode) {
                "ca-app-pub-3940256099942544/6300978111"
            } else {
                "YOUR_PROD_BANNER_ID"
            },
            
            interstitialAdUnitId = if (isTestMode) {
                "ca-app-pub-3940256099942544/1033173712"
            } else {
                "YOUR_PROD_INTERSTITIAL_ID"
            },
            
            nativeAdUnitId = if (isTestMode) {
                "ca-app-pub-3940256099942544/2247696110"
            } else {
                "YOUR_PROD_NATIVE_ID"
            },
            
            appOpenAdUnitId = if (isTestMode) {
                "ca-app-pub-3940256099942544/9257395921"
            } else {
                "YOUR_PROD_APPOPEN_ID"
            },

            // App Open Ad Configuration
            appOpenAdEnabled = true,
            showAppOpenOnFirstLaunch = false,
            minBackgroundTimeForAppOpen = 2000L
        )

        // Initialize AdsManager
        AdsManager.initialize(this, config) {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("AdsManager", "✅ Initialization complete")
            }
        }
    }

    companion object {
        private val isTestMode = BuildConfig.DEBUG
    }
}
```

### AndroidManifest.xml (Update application tag)
```xml
<application
    android:name=".MyApplication"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.MyApplication">
    <!-- ... -->
</application>
```

---

## Step 4: Use in Activities (1 minute)

### Display Banner Ad

#### activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- Your Content Here -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Welcome to AdsManager"
                android:textSize="24sp"
                android:padding="16dp" />

        </LinearLayout>
    </ScrollView>

    <!-- Banner Ad Container -->
    <FrameLayout
        android:id="@+id/banner_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:background="#F5F5F5" />

</LinearLayout>
```

#### MainActivity.kt
```kotlin
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.theankitparmar.adsmanager.utils.AdHelper
import com.example.myapp.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Show Banner Ad
        showBannerAd()
    }

    private fun showBannerAd() {
        AdHelper.showBannerAd(
            context = this,
            container = findViewById(R.id.banner_container),
            bannerAdSize = AdHelper.BannerAdSize.STANDARD,  // 320x50
            showShimmer = true,  // Show loading animation
            onAdLoaded = {
                Log.d("MainActivity", "✓ Banner ad loaded successfully")
            },
            onAdFailed = { error ->
                Log.e("MainActivity", "✗ Banner ad failed: $error")
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up ads
        AdHelper.destroyAds()
    }
}
```

---

## Step 5: Test Your Setup (Done! 🎉)

### Run the App
```bash
# Build and run on emulator or device
./gradlew installDebug
```

### Check Logs
```
✅ AdMob SDK initialized successfully
   Test Mode: true
   Debug Logging: true
   App Open Ads: true
⏳ Starting ad preloading in background...
✓ Banner ad loaded successfully
```

---

## Common Issues

### Issue: "Ad not showing"
**Solution:** Make sure container is added to view hierarchy
```kotlin
val container = findViewById<ViewGroup>(R.id.banner_container)
// Container must be in layout XML
```

### Issue: "Invalid Activity Error"
**Solution:** Use test ad IDs in debug mode
```kotlin
isTestMode = BuildConfig.DEBUG  // ✅ Correct
isTestMode = false  // ❌ Wrong for debug builds
```

### Issue: "No logs showing"
**Solution:** Enable debug logging
```kotlin
enableDebugLogging = BuildConfig.DEBUG  // ✅ Enable in debug
```

---

## Next Steps

1. 📖 Read the [full README.md](README.md) for comprehensive guide
2. 🎯 Check [best practices](README.md#-best-practices)
3. 🔧 Explore [API reference](README.md#-api-reference)
4. 🐛 See [troubleshooting](README.md#-troubleshooting)

---

## Quick Reference

### Test Ad Unit IDs
```
Banner:       ca-app-pub-3940256099942544/6300978111
Interstitial: ca-app-pub-3940256099942544/1033173712
Native:       ca-app-pub-3940256099942544/2247696110
App Open:     ca-app-pub-3940256099942544/9257395921
```

### Import Statements
```kotlin
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.core.AdsConfig
import com.theankitparmar.adsmanager.utils.AdHelper
import com.theankitparmar.adsmanager.callbacks.AdListener
import com.google.android.gms.ads.AdSize
```

---

## Support

- 📧 Email: codewithankit056@gmail.com
- 🐛 Issues: Report on GitHub
- 📚 Docs: Check documentation folder

**Happy Coding! 🚀**
