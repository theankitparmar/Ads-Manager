package com.theankitparmar.adsmanager.utils

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.theankitparmar.adsmanager.core.AdsManager

/**
 * Base Activity that automatically handles App Open Ads on resume
 */
abstract class BaseAdsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Optional: Exclude this activity if needed
        // AdsManager.excludeActivityFromAppOpenAd(this::class.java)
    }

    override fun onResume() {
        super.onResume()
        // Show App Open Ad when activity resumes (if coming from background)
        AdsManager.showAppOpenAdOnResume(this)
    }

    override fun onPause() {
        super.onPause()
        // Optional: Pause banner ads
        AdHelper.pauseBannerAds()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up ads if needed
        if (isFinishing) {
            AdHelper.destroyAds()
        }
    }
}