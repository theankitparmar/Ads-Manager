// MainActivity.kt
package com.theankitparmar.adsmanager.testApp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdSize
import com.theankitparmar.adsmanager.BuildConfig
import com.theankitparmar.adsmanager.ads.native.NativeType
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.utils.AdHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var isAppOpenShown = false
    private var totalRevenue: Long = 0
    private var isLoadingAds = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupReloadButtons()

        // Initialize ads after a short delay to ensure AdsManager is ready
        initializeAds()
    }

    private fun initializeAds() {
        if (isLoadingAds) return
        isLoadingAds = true

        scope.launch {
            try {
                updateStatus("Initializing ads...")

                // Wait for AdsManager to be initialized
                AdsManager.awaitInitialization()

                // Now that AdsManager is initialized, load all ads
                loadAllAds()

                // Show App Open Ad on first launch
                if (!isAppOpenShown) {
                    showAppOpenAd()
                }

            } catch (e: Exception) {
                updateStatus("Failed to initialize ads: ${e.message}")
                Toast.makeText(this@MainActivity, "Ad initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "Ad initialization error", e)
            } finally {
                isLoadingAds = false
            }
        }
    }

    private fun setupUI() {
        // Update status text
        val statusText = findViewById<android.widget.TextView>(R.id.tv_ad_status)
        statusText.text = "Initializing Ads Manager... Test Mode: ${BuildConfig.DEBUG}"
    }

    private fun loadAllAds() {
        updateStatus("Loading ads...")

        if (!AdsManager.isInitialized()) {
            updateStatus("AdsManager not initialized yet")
            return
        }

        loadBannerAd()
        loadNativeAd()
        setupInterstitialButton()

        updateStatus("Ads Manager initialized successfully")
    }

    private fun loadBannerAd() {
        val bannerContainer = findViewById<android.widget.LinearLayout>(R.id.banner_container)

        try {
            AdHelper.showBannerAd(
                context = this,
                container = bannerContainer,
                showShimmer = true,
                bannerAdSize = AdHelper.BannerAdSize.LARGE,
                onAdLoaded = {
                    runOnUiThread {
                        Toast.makeText(this, "Banner ad loaded successfully", Toast.LENGTH_SHORT).show()
                        updateStatus("Banner ad loaded")
                    }
                },
                onAdFailed = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Banner ad failed: $error", Toast.LENGTH_SHORT).show()
                        updateStatus("Banner ad failed: $error")
                    }
                }
            )
        } catch (e: Exception) {
            updateStatus("Banner ad error: ${e.message}")
            Log.e("MainActivity", "Banner ad error", e)
        }
    }

    private fun loadNativeAd() {
        val nativeContainer = findViewById<android.widget.LinearLayout>(R.id.native_container)

        try {
            AdHelper.showNativeAd(
                context = this,
                nativeType = NativeType.LARGE,
                container = nativeContainer,
                showShimmer = true,
                onAdLoaded = {
                    runOnUiThread {
                        Toast.makeText(this, "Native ad loaded successfully", Toast.LENGTH_SHORT).show()
                        updateStatus("Native ad loaded")
                    }
                },
                onAdFailed = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Native ad failed: $error", Toast.LENGTH_SHORT).show()
                        updateStatus("Native ad failed: $error")

                        // Show a fallback message
                        val textView = android.widget.TextView(this).apply {
                            text = "Native ad failed to load. Error: $error"
                            setTextColor(android.graphics.Color.RED)
                            gravity = android.view.Gravity.CENTER
                            setPadding(16, 16, 16, 16)
                        }
                        nativeContainer.removeAllViews()
                        nativeContainer.addView(textView)
                    }
                }
            )
        } catch (e: Exception) {
            updateStatus("Native ad error: ${e.message}")
            Log.e("MainActivity", "Native ad error", e)
        }
    }

    private fun setupInterstitialButton() {
        val interstitialButton = findViewById<android.widget.Button>(R.id.show_interstitial)

        interstitialButton.setOnClickListener {
            try {
                AdHelper.showInterstitialAd(
                    activity = this,
                    showLoadingDialog = false,
                    onAdDismissed = {
                        runOnUiThread {
                            Toast.makeText(this, "Interstitial ad dismissed", Toast.LENGTH_SHORT).show()
                            updateStatus("Interstitial dismissed")
                        }
                    },
                    onAdFailed = { error ->
                        runOnUiThread {
                            Toast.makeText(this, "Interstitial failed: $error", Toast.LENGTH_SHORT).show()
                            updateStatus("Interstitial failed: $error")
                        }
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Error showing interstitial: ${e.message}", Toast.LENGTH_SHORT).show()
                updateStatus("Interstitial error: ${e.message}")
            }
        }
    }

    private fun setupReloadButtons() {
        val reloadBannerButton = findViewById<android.widget.Button>(R.id.btn_reload_banner)
        val reloadNativeButton = findViewById<android.widget.Button>(R.id.btn_reload_native)

        reloadBannerButton.setOnClickListener {
            val bannerContainer = findViewById<android.widget.LinearLayout>(R.id.banner_container)
            bannerContainer.removeAllViews()
            loadBannerAd()
            Toast.makeText(this, "Reloading banner ad...", Toast.LENGTH_SHORT).show()
        }

        reloadNativeButton.setOnClickListener {
            val nativeContainer = findViewById<android.widget.LinearLayout>(R.id.native_container)
            nativeContainer.removeAllViews()
            loadNativeAd()
            Toast.makeText(this, "Reloading native ad...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppOpenAd() {
        try {
            val wasShown = AdHelper.showAppOpenAd(
                activity = this,
                showLoadingDialog = false,
                onAdDismissed = {
                    runOnUiThread {
                        Toast.makeText(this, "App Open ad dismissed", Toast.LENGTH_SHORT).show()
                        updateStatus("App Open dismissed")
                        isAppOpenShown = true
                    }
                },
                onAdFailed = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "App Open failed: $error", Toast.LENGTH_SHORT).show()
                        updateStatus("App Open failed: $error")
                    }
                }
            )

            if (!wasShown) {
                Log.d("MainActivity", "App Open ad not ready, loading in background")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "App Open ad error", e)
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            val statusText = findViewById<android.widget.TextView>(R.id.tv_ad_status)
            statusText.text = "Status: $message"
        }
    }

    private fun formatRevenue(valueMicros: Long): String {
        val valueInDollars = valueMicros / 1_000_000.0
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        formatter.maximumFractionDigits = 6
        return formatter.format(valueInDollars)
    }

    override fun onPause() {
        super.onPause()
        AdHelper.pauseBannerAds()
    }

    override fun onResume() {
        super.onResume()
        AdHelper.resumeBannerAds()
    }

    override fun onDestroy() {
        super.onDestroy()
        AdHelper.destroyAds()
    }
}