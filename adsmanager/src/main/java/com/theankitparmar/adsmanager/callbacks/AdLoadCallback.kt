package com.theankitparmar.adsmanager.callbacks

interface AdLoadCallback {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: String)
    fun onAdImpression()
    fun onAdClicked()
}