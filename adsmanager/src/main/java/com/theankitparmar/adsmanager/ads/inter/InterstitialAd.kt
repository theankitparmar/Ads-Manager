// InterstitialAd.kt
package com.theankitparmar.adsmanager.ads.inter

import android.app.Activity
import android.content.Context
import com.theankitparmar.adsmanager.adInterface.AdsConfiguration
import com.theankitparmar.adsmanager.ads.AdState
import com.theankitparmar.adsmanager.ads.BaseAd
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InterstitialAd(
    context: Context,
    adUnitId: String,
    config: AdsConfiguration
) : BaseAd<AdState>(context, adUnitId, config) {

    private var interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd? = null

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/1033173712"

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")

            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()

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
                                if (config.enableAutoReload) {
                                    loadAd()
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                interstitialAd = null
                                _listener?.onAdFailedToShow(error.message)
                                loadAd()
                            }

                            override fun onAdImpression() {
                                _listener?.onAdImpression()
                            }

                            override fun onAdClicked() {
                                _listener?.onAdClicked()
                            }
                        }

                        // Set revenue callback
                        ad.setOnPaidEventListener { adValue ->
                            _listener?.onAdRevenue(
                                adValue.valueMicros,
                                adValue.currencyCode,
                                adValue.precisionType
                            )
                        }
                    }

                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        throw Exception(error.message)
                    }
                }
            )

            AdResult.Loading
        } catch (e: Exception) {
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun showAd(activity: Activity?) {
        if (interstitialAd != null && activity != null && !activity.isFinishing) {
            interstitialAd?.show(activity)
        } else {
            _listener?.onAdFailedToShow("Ad not loaded or activity invalid")
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