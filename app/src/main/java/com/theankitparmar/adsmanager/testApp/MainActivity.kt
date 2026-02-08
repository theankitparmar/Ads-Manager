// MainActivity.kt
package com.theankitparmar.adsmanager.testApp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.theankitparmar.adsmanager.BuildConfig
import com.theankitparmar.adsmanager.ads.native.NativeType
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.testApp.databinding.ActivityMainBinding
import com.theankitparmar.adsmanager.utils.AdHelper
import com.theankitparmar.adsmanager.utils.BaseAdsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : BaseAdsActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isLoadingAds = false

    // Current selections
    private var currentBannerSize = AdHelper.BannerAdSize.STANDARD
    private var currentNativeType = NativeType.MEDIUM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupUI()
        setupSpinners()
        setupButtons()
        initializeAds()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainScrollView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        // Update status text
        updateStatus("Initializing Ads Manager...")
        binding.tvTestMode.text = "Test Mode: ${BuildConfig.DEBUG}"

        // Set banner and native type display
        binding.tvBannerSize.text = currentBannerSize.name
        binding.tvNativeType.text = currentNativeType.name
    }

    private fun setupSpinners() {
        // Banner Size Spinner
        val bannerSizes = AdHelper.BannerAdSize.values().map { it.name }
        val bannerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bannerSizes)
        bannerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBannerSize.adapter = bannerAdapter

        binding.spinnerBannerSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentBannerSize = AdHelper.BannerAdSize.values()[position]
                binding.tvBannerSize.text = currentBannerSize.name
                updateStatus("Selected Banner Size: ${currentBannerSize.name}")
                showSnackbar("Banner size set to ${currentBannerSize.name}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Native Type Spinner
        val nativeTypes = NativeType.values().map { it.name }
        val nativeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nativeTypes)
        nativeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNativeType.adapter = nativeAdapter

        binding.spinnerNativeType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentNativeType = NativeType.values()[position]
                binding.tvNativeType.text = currentNativeType.name
                updateStatus("Selected Native Type: ${currentNativeType.name}")
                showSnackbar("Native type set to ${currentNativeType.name}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        // Quick Action Buttons
        binding.btnClearAdsQuick.setOnClickListener {
            clearAllAds()
        }

        // Banner Ad Button
        binding.btnLoadBanner.setOnClickListener {
            loadBannerAd()
        }

        // Native Ad Button
        binding.btnLoadNative.setOnClickListener {
            loadNativeAd()
        }

        // Interstitial Ad Button
        binding.btnShowInterstitial.setOnClickListener {
            showInterstitialAd()
        }

        // App Open Ad Button
        binding.btnTestAppOpen.setOnClickListener {
            testAppOpenAd()
        }

        // Clear All Ads Button
        binding.btnClearAds.setOnClickListener {
            clearAllAds()
        }

        // Refresh Ads Button
        binding.btnRefreshAds.setOnClickListener {
            refreshAllAds()
        }

        // Debug Info Button
        binding.btnDebugInfo.setOnClickListener {
            showDebugInfo()
        }
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

            } catch (e: Exception) {
                updateStatus("Failed to initialize ads: ${e.message}")
                showSnackbar("Ad initialization failed: ${e.message}", true)
                Log.e("MainActivity", "Ad initialization error", e)
            } finally {
                isLoadingAds = false
            }
        }
    }

    private fun loadAllAds() {
        updateStatus("Loading ads...")

        scope.launch {
            try {
                // Wait for AdsManager to be fully initialized
                AdsManager.awaitInitialization()

                if (!AdsManager.isInitialized()) {
                    updateStatus("AdsManager not initialized yet")
                    return@launch
                }

                // Don't auto-load ads on start, let user click buttons
                updateStatus("Ready. Select options and click buttons to test ads.")
                showSnackbar("Ads Manager is ready!")

            } catch (e: Exception) {
                updateStatus("Failed to initialize ads: ${e.message}")
                Log.e("MainActivity", "Ad initialization error", e)
            }
        }
    }

    private fun loadBannerAd() {
        try {
            binding.bannerAdContainer.removeAllViews()

            AdHelper.showBannerAd(
                context = this,
                container = binding.bannerAdContainer,
                bannerAdSize = currentBannerSize,
                showShimmer = true,
                onAdLoaded = {
                    runOnUiThread {
                        showSnackbar("Banner ad (${currentBannerSize.name}) loaded successfully")
                        updateStatus("Banner ad (${currentBannerSize.name}) loaded")
                    }
                },
                onAdFailed = { error ->
                    runOnUiThread {
                        showSnackbar("Banner ad failed: $error", true)
                        updateStatus("Banner ad failed: $error")
                    }
                }
            )
        } catch (e: Exception) {
            updateStatus("Banner ad error: ${e.message}")
            showSnackbar("Banner ad error: ${e.message}", true)
            Log.e("MainActivity", "Banner ad error", e)
        }
    }

    private fun loadNativeAd() {
        try {
            binding.nativeAdContainer.removeAllViews()

            AdHelper.showNativeAd(
                context = this,
                container = binding.nativeAdContainer,
                nativeType = currentNativeType,
                showShimmer = true,
                onAdLoaded = {
                    runOnUiThread {
                        showSnackbar("Native ad (${currentNativeType.name}) loaded successfully")
                        updateStatus("Native ad (${currentNativeType.name}) loaded")
                    }
                },
                onAdFailed = { error ->
                    runOnUiThread {
                        showSnackbar("Native ad failed: $error", true)
                        updateStatus("Native ad failed: $error")
                    }
                }
            )
        } catch (e: Exception) {
            updateStatus("Native ad error: ${e.message}")
            showSnackbar("Native ad error: ${e.message}", true)
            Log.e("MainActivity", "Native ad error", e)
        }
    }

    private fun showInterstitialAd() {
        try {
            AdHelper.showInterstitialAd(
                activity = this,
                showLoadingDialog = false,
                onAdDismissed = {
                    runOnUiThread {
                        showSnackbar("Interstitial ad dismissed")
                        updateStatus("Interstitial dismissed")
                    }
                },
                onAdFailed = { error ->
                    runOnUiThread {
                        showSnackbar("Interstitial failed: $error", true)
                        updateStatus("Interstitial failed: $error")
                    }
                }
            )
        } catch (e: Exception) {
            showSnackbar("Error showing interstitial: ${e.message}", true)
            updateStatus("Interstitial error: ${e.message}")
        }
    }

    private fun testAppOpenAd() {
        try {
            val wasShown = AdHelper.showAppOpenAd(
                activity = this,
                showLoadingDialog = false,
                onAdDismissed = {
                    runOnUiThread {
                        showSnackbar("App Open ad dismissed")
                        updateStatus("App Open dismissed")
                    }
                },
                onAdFailed = { error ->
                    runOnUiThread {
                        showSnackbar("App Open failed: $error", true)
                        updateStatus("App Open failed: $error")
                    }
                }
            )

            if (wasShown) {
                updateStatus("App Open ad shown")
            } else {
                updateStatus("App Open ad not ready, loading...")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "App Open ad error", e)
            updateStatus("App Open error: ${e.message}")
        }
    }

    private fun clearAllAds() {
        binding.bannerAdContainer.removeAllViews()
        binding.nativeAdContainer.removeAllViews()

        // Add back the placeholder text
        val bannerPlaceholder = android.widget.TextView(this).apply {
            text = "👆 Tap 'Load Banner Ad' to display"
            setTextColor(resources.getColor(R.color.tertiary_text))
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        binding.bannerAdContainer.addView(bannerPlaceholder)

        val nativePlaceholder = android.widget.TextView(this).apply {
            text = "👆 Tap 'Load Native Ad' to display"
            setTextColor(resources.getColor(R.color.tertiary_text))
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        binding.nativeAdContainer.addView(nativePlaceholder)

        // Also clear the ad helper's internal references
        AdHelper.destroyAds()

        showSnackbar("All ads cleared successfully")
        updateStatus("All ads cleared")
    }

    private fun refreshAllAds() {
        updateStatus("Refreshing all ads...")
        showSnackbar("Refreshing ads...")

        // Clear existing ads
        clearAllAds()

        // Re-initialize ads
        initializeAds()
    }

    private fun showDebugInfo() {
        val debugInfo = """
            Ads Manager Debug Info:
            • Initialized: ${AdsManager.isInitialized()}
            • Test Mode: ${BuildConfig.DEBUG}
            • Banner Size: ${currentBannerSize.name}
            • Native Type: ${currentNativeType.name}
            • SDK Version: ${com.google.android.gms.ads.MobileAds.getVersion()}
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Debug Information")
            .setMessage(debugInfo)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy") { _, _ ->
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Debug Info", debugInfo)
                clipboard.setPrimaryClip(clip)
                showSnackbar("Debug info copied to clipboard")
            }
            .show()
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.tvAdStatus.text = message
            Log.d("MainActivity", message)
        }
    }

    private fun showSnackbar(message: String, isError: Boolean = false) {
        runOnUiThread {
            val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            if (isError) {
                snackbar.setBackgroundTint(resources.getColor(R.color.red_500))
            }
            snackbar.show()
        }
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
        // Don't destroy all ads on activity destroy since we want to keep them for testing
        // AdHelper.destroyAds()
    }
}