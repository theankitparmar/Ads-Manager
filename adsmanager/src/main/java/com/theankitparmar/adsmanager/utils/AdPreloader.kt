package com.theankitparmar.adsmanager.utils

import android.content.Context
import com.theankitparmar.adsmanager.core.AdsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdPreloader(private val context: Context) {
    
    private var preloadJob: Job? = null
    private val preloadedAds = mutableMapOf<String, Any>()
    
    fun preloadInterstitial() {
        preloadJob = CoroutineScope(Dispatchers.IO).launch {
            val interstitial = AdsManager.getInterstitialAd(context)
            preloadedAds["interstitial"] = interstitial
        }
    }
    
    fun preloadNative() {
        preloadJob = CoroutineScope(Dispatchers.IO).launch {
            val native = AdsManager.getNativeAd(context)
            native.loadAd()
            preloadedAds["native"] = native
        }
    }
    
    fun preloadAll() {
        preloadInterstitial()
        preloadNative()
    }
    
    fun getPreloadedAd(key: String): Any? {
        return preloadedAds[key]
    }
    
    fun clearPreloadedAds() {
        preloadJob?.cancel()
        preloadedAds.clear()
    }
}