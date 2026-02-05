package com.theankitparmar.adsmanager.testApp

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.ads.BannerAd
import com.theankitparmar.adsmanager.ads.InterstitialAd
import com.theankitparmar.adsmanager.ads.NativeAd
import com.theankitparmar.adsmanager.callbacks.AdLoadCallback
import com.theankitparmar.adsmanager.utils.AdLifecycleObserver
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bannerAd: BannerAd
    private lateinit var interstitialAd: InterstitialAd
    private lateinit var nativeAd: NativeAd
    private lateinit var tvAdStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvAdStatus = findViewById(R.id.tv_ad_status)

        // Initialize Banner Ad
        val bannerContainer = findViewById<ViewGroup>(R.id.banner_container)
        bannerAd = AdsManager.createBannerAd(this)
        bannerAd.setAdLoadCallback(object : AdLoadCallback {
            override fun onAdLoaded() {
                updateStatus("Banner ad loaded successfully")
            }

            override fun onAdFailedToLoad(error: String) {
                updateStatus("Banner failed: $error")
            }

            override fun onAdImpression() {
                updateStatus("Banner impression recorded")
            }

            override fun onAdClicked() {
                updateStatus("Banner clicked")
            }
        })
        bannerAd.loadAndShow(bannerContainer)

        // Add lifecycle observer
        lifecycle.addObserver(AdLifecycleObserver(bannerAd))

        // Initialize Interstitial Ad
        interstitialAd = AdsManager.createInterstitialAd(this)

        // Set callbacks
        interstitialAd.setAdLoadCallback(object : AdLoadCallback {
            override fun onAdLoaded() {
                Log.d("AdsManager", "Interstitial loaded")
                updateStatus("Interstitial ad loaded")
                findViewById<Button>(R.id.show_interstitial).apply {
                    text = "Show Interstitial Ad"
                    isEnabled = true
                }
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e("AdsManager", "Interstitial failed: $error")
                updateStatus("Interstitial failed: $error")
            }

            override fun onAdImpression() {
                Log.d("AdsManager", "Interstitial impression")
                updateStatus("Interstitial impression")
            }

            override fun onAdClicked() {
                Log.d("AdsManager", "Interstitial clicked")
                updateStatus("Interstitial clicked")
            }
        })

        // Show interstitial on button click
        findViewById<Button>(R.id.show_interstitial).setOnClickListener {
            if (interstitialAd.showIfLoaded()) {
                // Ad shown
                updateStatus("Interstitial ad shown")
            } else {
                // Ad not ready yet
                Toast.makeText(this, "Ad loading, please wait", Toast.LENGTH_SHORT).show()
                updateStatus("Interstitial not ready, loading...")
                interstitialAd.load()
            }
        }

        // Initialize Native Ad
        val nativeContainer = findViewById<ViewGroup>(R.id.native_container)
        nativeAd = AdsManager.createNativeAd(this)
        nativeAd.setAdLoadCallback(object : AdLoadCallback {
            override fun onAdLoaded() {
                updateStatus("Native ad loaded")
                // Inflate the native ad view when loaded
                lifecycleScope.launch {
                    nativeAd.inflateNativeAdView(nativeContainer)
                }
            }

            override fun onAdFailedToLoad(error: String) {
                updateStatus("Native failed: $error")
            }

            override fun onAdImpression() {
                updateStatus("Native impression")
            }

            override fun onAdClicked() {
                updateStatus("Native clicked")
            }
        })
        nativeAd.load()

        // Setup reload buttons
        findViewById<Button>(R.id.btn_reload_banner).setOnClickListener {
            updateStatus("Reloading banner ad...")
            bannerAd.load()
        }

        findViewById<Button>(R.id.btn_reload_native).setOnClickListener {
            updateStatus("Reloading native ad...")
            nativeAd.load()
            nativeContainer.removeAllViews()
        }

        updateStatus("Ads Manager initialized. Loading ads...")
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            tvAdStatus.text = "Status: $message"
            Log.d("AdsManager", message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerAd.destroy()
        interstitialAd.destroy()
        nativeAd.destroy()
    }
}