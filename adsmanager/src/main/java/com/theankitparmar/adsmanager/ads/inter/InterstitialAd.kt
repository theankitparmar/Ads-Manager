// InterstitialAd.kt
package com.theankitparmar.adsmanager.ads.inter

import android.app.Activity
import android.content.Context
import com.theankitparmar.adsmanager.adInterface.*
import com.theankitparmar.adsmanager.ads.AdState
import com.theankitparmar.adsmanager.ads.BaseAd
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Interstitial Ad implementation for Google AdMob
 * Displays full-screen ads with support for auto-reload on dismissal
 * 
 * Features:
 * - Full-screen ad display
 * - Automatic retry on failure
 * - Revenue tracking
 * - Ad lifecycle callbacks
 * 
 * @param context The Android context
 * @param adUnitId The AdMob interstitial ad unit ID
 * @param config Ad configuration settings
 */
class InterstitialAd(
    context: Context,
    adUnitId: String,
    config: AdsConfiguration
) : BaseAd<AdState>(
    context = context,
    adUnitId = adUnitId,
    config = config,
    adType = AdType.INTERSTITIAL
) {

    private var interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd? = null

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/1033173712"

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")

            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()

            val result = suspendCancellableCoroutine<AdResult<Unit>> { continuation ->
                try {
                    com.google.android.gms.ads.interstitial.InterstitialAd.load(
                        context,
                        getAdUnitId(),
                        adRequest,
                        object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: com.google.android.gms.ads.interstitial.InterstitialAd) {
                                interstitialAd = ad

                                ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        interstitialAd = null
                                        _listener?.onAdDismissed()
                                        emitEvent(AdEvent.Dismissed(AdType.INTERSTITIAL, getAdId()))
                                        android.util.Log.d("InterstitialAd", "✓ Ad dismissed")

                                        if (config.enableAutoReload) {
                                            android.util.Log.d("InterstitialAd", "Auto-reloading ad...")
                                            loadAd()
                                        }
                                    }

                                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                        interstitialAd = null
                                        _listener?.onAdFailedToShow(error.message)
                                        emitEvent(AdEvent.Failed(
                                            AdType.INTERSTITIAL,
                                            CustomAdError.fromAdError(error),
                                            getAdId()
                                        ))
                                        android.util.Log.e("InterstitialAd", "✗ Failed to show: ${error.message}")
                                        loadAd()
                                    }

                                    override fun onAdImpression() {
                                        _listener?.onAdImpression()
                                        emitEvent(AdEvent.Impression(AdType.INTERSTITIAL, getAdId()))
                                        android.util.Log.d("InterstitialAd", "Ad impression recorded")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        emitEvent(AdEvent.Opened(AdType.INTERSTITIAL, getAdId()))
                                        android.util.Log.d("InterstitialAd", "✓ Ad displayed")
                                    }

                                    override fun onAdClicked() {
                                        _listener?.onAdClicked()
                                        emitEvent(AdEvent.Clicked(AdType.INTERSTITIAL, getAdId()))
                                        android.util.Log.d("InterstitialAd", "Ad clicked")
                                    }
                                }

                                // Set revenue callback
                                ad.setOnPaidEventListener { adValue ->
                                    _listener?.onAdRevenue(adValue)
                                    emitEvent(AdEvent.Revenue(
                                        AdType.INTERSTITIAL,
                                        adValue.valueMicros,
                                        adValue.currencyCode,
                                        adValue.precisionType,
                                        getAdId()
                                    ))
                                    android.util.Log.d("InterstitialAd", "Revenue: ${adValue.valueMicros} ${adValue.currencyCode}")
                                }

                                // Emit loaded event
                                _listener?.onAdLoaded()
                                emitEvent(AdEvent.Loaded(AdType.INTERSTITIAL, getAdId()))
                                android.util.Log.d("InterstitialAd", "✓ Interstitial ad loaded successfully")

                                if (!continuation.isCancelled) {
                                    continuation.resume(AdResult.Success(Unit))
                                }
                            }

                            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                _listener?.onAdFailedToLoad(error.message)
                                emitEvent(AdEvent.Failed(
                                    AdType.INTERSTITIAL,
                                    CustomAdError.fromLoadAdError(error),
                                    getAdId()
                                ))
                                android.util.Log.e("InterstitialAd", "✗ Failed to load: ${error.message}")

                                if (!continuation.isCancelled) {
                                    continuation.resume(AdResult.Error(error.message ?: "Unknown error"))
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("InterstitialAd", "Exception during load: ${e.message}", e)
                    if (!continuation.isCancelled) {
                        continuation.resume(AdResult.Error("Exception: ${e.message}"))
                    }
                }
            }

            result
        } catch (e: Exception) {
            android.util.Log.e("InterstitialAd", "Error in loadAdInternal: ${e.message}", e)
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun showAd(activity: Activity?) {
        if (interstitialAd != null && activity != null && !activity.isFinishing) {
            try {
                interstitialAd?.show(activity)
                android.util.Log.d("InterstitialAd", "Attempting to show interstitial ad")
            } catch (e: Exception) {
                android.util.Log.e("InterstitialAd", "Error showing ad: ${e.message}", e)
                _listener?.onAdFailedToShow("Error: ${e.message}")
            }
        } else {
            val reason = when {
                interstitialAd == null -> "Ad not loaded"
                activity == null -> "Activity is null"
                activity.isFinishing -> "Activity is finishing"
                else -> "Unknown"
            }
            android.util.Log.w("InterstitialAd", "Cannot show ad: $reason")
            _listener?.onAdFailedToShow(reason)
            emitEvent(AdEvent.Failed(
                AdType.INTERSTITIAL,
                CustomAdError.ShowError(1, reason),
                getAdId()
            ))
        }
    }

    /**
     * Show ad if already loaded, without waiting
     * 
     * @return true if ad was shown, false if not loaded
     */
    fun showIfLoaded(): Boolean {
        return if (interstitialAd != null) {
            showAdWithActivityCheck { activity ->
                interstitialAd?.show(activity)
            }
            true
        } else {
            false
        }
    }

    override fun destroy() {
        super.destroy()
        try {
            interstitialAd = null
        } catch (e: Exception) {
            android.util.Log.e("InterstitialAd", "Error during destroy: ${e.message}")
        }
    }
}