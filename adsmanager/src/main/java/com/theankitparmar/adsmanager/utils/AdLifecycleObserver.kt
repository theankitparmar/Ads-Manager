package com.theankitparmar.adsmanager.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.theankitparmar.adsmanager.ads.banner.BannerAd

class AdLifecycleObserver(
    private val bannerAd: BannerAd? = null
) : LifecycleEventObserver {
    
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> bannerAd?.resume()
            Lifecycle.Event.ON_PAUSE -> bannerAd?.pause()
            Lifecycle.Event.ON_DESTROY -> bannerAd?.destroy()
            else -> {}
        }
    }
}
