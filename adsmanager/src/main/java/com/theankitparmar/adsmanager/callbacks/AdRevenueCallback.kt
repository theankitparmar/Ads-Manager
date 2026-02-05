package com.theankitparmar.adsmanager.callbacks

interface AdRevenueCallback {
    fun onPaidEvent(valueMicros: Long, currencyCode: String, precision: Int)
}