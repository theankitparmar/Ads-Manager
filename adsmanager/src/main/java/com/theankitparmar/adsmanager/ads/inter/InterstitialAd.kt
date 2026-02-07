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
                com.google.android.gms.ads.interstitial.InterstitialAd.load(
                    context,
                    getAdUnitId(),
                    adRequest,
                    object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: com.google.android.gms.ads.interstitial.InterstitialAd) {
                            interstitialAd = ad

                            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                                // Inside InterstitialAd.kt
                                override fun onAdDismissedFullScreenContent() {
                                    interstitialAd = null
                                    _listener?.onAdDismissed()
                                    emitEvent(AdEvent.Dismissed(AdType.INTERSTITIAL, getAdId()))

                                    if (config.enableAutoReload) {
                                        // Only load if we aren't already loading
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
                                    loadAd()
                                }

                                override fun onAdImpression() {
                                    _listener?.onAdImpression()
                                    emitEvent(AdEvent.Impression(AdType.INTERSTITIAL, getAdId()))
                                }

                                override fun onAdShowedFullScreenContent() {
                                    emitEvent(AdEvent.Opened(AdType.INTERSTITIAL, getAdId()))
                                }

                                override fun onAdClicked() {
                                    _listener?.onAdClicked()
                                    emitEvent(AdEvent.Clicked(AdType.INTERSTITIAL, getAdId()))
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
                            }

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

                            if (!continuation.isCancelled) {
                                continuation.resume(AdResult.Error(error.message))
                            }
                        }
                    }
                )
            }

            result
        } catch (e: Exception) {
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun showAd(activity: Activity?) {
        if (interstitialAd != null && activity != null && !activity.isFinishing) {
            interstitialAd?.show(activity)
        } else {
            _listener?.onAdFailedToShow("Ad not loaded or activity invalid")
            emitEvent(AdEvent.Failed(
                AdType.INTERSTITIAL,
                CustomAdError.ShowError(1, "Ad not loaded or activity invalid"),
                getAdId()
            ))
        }
    }

    override fun destroy() {
        super.destroy()
        interstitialAd = null
    }

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
}