package com.theankitparmar.adsmanager.adInterface

import android.app.Activity
import com.theankitparmar.adsmanager.callbacks.AdListener

// 1. Unified Ad Interface
interface AdManager {
    fun loadAd()
    fun showAd(activity: android.app.Activity?)
    fun destroy()
    fun isLoaded(): Boolean
    fun setListener(listener: AdListener?)
}