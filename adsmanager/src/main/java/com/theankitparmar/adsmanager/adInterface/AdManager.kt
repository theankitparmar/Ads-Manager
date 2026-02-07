package com.theankitparmar.adsmanager.adInterface

import android.app.Activity
import com.theankitparmar.adsmanager.callbacks.AdListener
import kotlinx.coroutines.flow.Flow

// 1. Unified Ad Interface
interface AdManager {
    fun loadAd()
    fun showAd(activity: Activity?)
    fun destroy()
    fun isLoaded(): Boolean
    fun setListener(listener: AdListener?)
    fun getAdId(): String
    fun getAdType(): AdType

    // New methods for event-driven architecture
    fun getEvents(): Flow<AdEvent>
    fun clearEvents()
}