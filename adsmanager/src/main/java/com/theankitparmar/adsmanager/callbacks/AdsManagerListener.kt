package com.theankitparmar.adsmanager.callbacks

interface AdsManagerListener {
    fun onAdLoaded() {}
    fun onAdFailedToLoad(message: String) {}
    fun onAdClicked() {}
    fun onAdImpression() {}
}
