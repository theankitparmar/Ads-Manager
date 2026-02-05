// AdListener.kt
package com.theankitparmar.adsmanager.callbacks

interface AdListener {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: String)
    fun onAdImpression()
    fun onAdClicked()
    fun onAdDismissed()
    fun onAdFailedToShow(error: String)
    fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int)
}