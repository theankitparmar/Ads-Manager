// BannerAd.kt
package com.theankitparmar.adsmanager.ads.banner

import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.theankitparmar.adsmanager.adInterface.*
import com.theankitparmar.adsmanager.ads.AdState
import com.theankitparmar.adsmanager.ads.BaseAd
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Banner Ad implementation supporting multiple ad sizes
 * Handles loading and displaying Google AdMob banner ads with automatic retry
 *
 * @param bannerContext The context for ad loading
 * @param adUnitId The AdMob banner ad unit ID
 * @param config Ad configuration settings
 * @param adSize The size of the banner (BANNER, LARGE_BANNER, etc.)
 */
class BannerAd(
    private val bannerContext: Context,
    adUnitId: String,
    config: AdsConfiguration,
    val adSize: AdSize = AdSize.BANNER
) : BaseAd<AdState>(
    context = bannerContext,
    adUnitId = adUnitId,
    config = config,
    adType = AdType.BANNER
) {

    val currentAdSize: AdSize = adSize
    private var adView: AdView? = null
    private var containerReference: android.view.ViewGroup? = null

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/6300978111"


    private fun createAdView(): AdView {
        val context = getContext() ?: throw IllegalStateException("Context is null")
        return AdView(context).apply {
            this.adUnitId = if (config.isTestMode) getTestAdUnitId() else adUnitId
            this.setAdSize(currentAdSize)

        }
    }

    /**
     * Loads the banner ad asynchronously
     * Creates AdView, sets up listeners, and loads the ad
     */
    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")

            val result = suspendCancellableCoroutine<AdResult<Unit>> { continuation ->
                try {
                    adView = createAdView()

                    adView?.adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdLoaded() {
                            _listener?.onAdLoaded()
                            emitEvent(AdEvent.Loaded(AdType.BANNER, getAdId()))

                            // Log successful load
                            android.util.Log.d("BannerAd", "✓ Banner ad loaded successfully: ${getAdId()}")

                            if (!continuation.isCancelled) {
                                continuation.resume(AdResult.Success(Unit))
                            }
                        }

                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            _listener?.onAdFailedToLoad(error.message)
                            emitEvent(AdEvent.Failed(
                                AdType.BANNER,
                                CustomAdError.fromLoadAdError(error),
                                getAdId()
                            ))

                            android.util.Log.e("BannerAd", "✗ Banner ad failed to load: ${error.message}")

                            if (!continuation.isCancelled) {
                                continuation.resume(AdResult.Error(error.message ?: "Unknown error"))
                            }
                        }

                        override fun onAdClicked() {
                            _listener?.onAdClicked()
                            emitEvent(AdEvent.Clicked(AdType.BANNER, getAdId()))
                        }

                        override fun onAdImpression() {
                            _listener?.onAdImpression()
                            emitEvent(AdEvent.Impression(AdType.BANNER, getAdId()))
                        }

                        override fun onAdOpened() {
                            emitEvent(AdEvent.Opened(AdType.BANNER, getAdId()))
                        }

                        override fun onAdClosed() {
                            emitEvent(AdEvent.Closed(AdType.BANNER, getAdId()))
                        }
                    }

                    val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
                    adView?.loadAd(adRequest)
                } catch (e: Exception) {
                    android.util.Log.e("BannerAd", "Exception in loadAdInternal: ${e.message}", e)
                    if (!continuation.isCancelled) {
                        continuation.resume(AdResult.Error("Exception: ${e.message}"))
                    }
                }
            }

            result
        } catch (e: Exception) {
            android.util.Log.e("BannerAd", "Error in loadAdInternal: ${e.message}", e)
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun showAd(activity: android.app.Activity?) {
        loadAd()
    }

    /**
     * Attach the banner ad view to a container
     * Must be called after the ad is loaded
     * 
     * @param container The ViewGroup to attach the ad to
     * @return true if successfully attached, false if ad not loaded
     */
    fun attachToContainer(container: android.view.ViewGroup): Boolean {
        if (adView == null) {
            android.util.Log.w("BannerAd", "Cannot attach: Ad not loaded yet")
            return false
        }

        try {
            // Remove from previous parent if attached
            (adView!!.parent as? android.view.ViewGroup)?.removeView(adView)
            
            // Add to new container
            container.addView(adView)
            containerReference = container
            android.util.Log.d("BannerAd", "✓ Banner ad attached to container")
            return true
        } catch (e: Exception) {
            android.util.Log.e("BannerAd", "Failed to attach ad to container: ${e.message}", e)
            return false
        }
    }

    /**
     * Get the current container reference
     */
    fun getContainer(): android.view.ViewGroup? = containerReference

    override fun destroy() {
        super.destroy()
        try {
            adView?.let { adView ->
                (adView.parent as? android.view.ViewGroup)?.removeView(adView)
                adView.destroy()
                this.adView = null
            }
            containerReference = null
        } catch (e: Exception) {
            android.util.Log.e("BannerAd", "Error during destroy: ${e.message}")
        }
    }

    fun getAdView(): AdView? = adView

    fun pause() {
        adView?.pause()
    }

    fun resume() {
        adView?.resume()
    }
}