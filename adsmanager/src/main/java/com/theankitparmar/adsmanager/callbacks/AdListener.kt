// AdListener.kt
package com.theankitparmar.adsmanager.callbacks

interface AdListener {
    fun onAdLoading()  // Add this missing method
    fun onAdLoaded()
    fun onAdFailedToLoad(error: String)
    fun onAdClicked()
    fun onAdImpression()
    fun onAdDismissed()
    fun onAdFailedToShow(error: String)
    fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int)

    // New method for revenue with AdValue
    fun onAdRevenue(value: com.google.android.gms.ads.AdValue) {
        onAdRevenue(value.valueMicros, value.currencyCode, value.precisionType)
    }
}