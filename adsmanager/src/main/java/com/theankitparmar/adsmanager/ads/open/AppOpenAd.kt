package com.theankitparmar.adsmanager.ads.open

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

class AppOpenAd(
    context: Context,
    adUnitId: String,
    config: AdsConfiguration
) : BaseAd<AdState>(
    context = context,
    adUnitId = adUnitId,
    config = config,
    adType = AdType.APP_OPEN
) {

    private var appOpenAd: com.google.android.gms.ads.appopen.AppOpenAd? = null
    private var loadTime: Long = 0

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/3419835294"

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")

            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()

            val result = suspendCancellableCoroutine<AdResult<Unit>> { continuation ->
                com.google.android.gms.ads.appopen.AppOpenAd.load(
                    context,
                    getAdUnitId(),
                    adRequest,
                    com.google.android.gms.ads.appopen.AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    object : com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback() {
                        override fun onAdLoaded(ad: com.google.android.gms.ads.appopen.AppOpenAd) {
                            appOpenAd = ad
                            loadTime = System.currentTimeMillis()

                            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    appOpenAd = null
                                    _listener?.onAdDismissed()
                                    emitEvent(AdEvent.Dismissed(AdType.APP_OPEN, getAdId()))
                                    if (config.enableAutoReload) {
                                        loadAd()
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                    appOpenAd = null
                                    _listener?.onAdFailedToShow(error.message)
                                    emitEvent(AdEvent.Failed(
                                        AdType.APP_OPEN,
                                        CustomAdError.fromAdError(error),
                                        getAdId()
                                    ))
                                    loadAd()
                                }

                                override fun onAdImpression() {
                                    _listener?.onAdImpression()
                                    emitEvent(AdEvent.Impression(AdType.APP_OPEN, getAdId()))
                                }

                                override fun onAdShowedFullScreenContent() {
                                    emitEvent(AdEvent.Opened(AdType.APP_OPEN, getAdId()))
                                }

                                override fun onAdClicked() {
                                    _listener?.onAdClicked()
                                    emitEvent(AdEvent.Clicked(AdType.APP_OPEN, getAdId()))
                                }
                            }

                            // Set revenue callback
                            ad.setOnPaidEventListener { adValue ->
                                _listener?.onAdRevenue(adValue)
                                emitEvent(AdEvent.Revenue(
                                    AdType.APP_OPEN,
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
                                AdType.APP_OPEN,
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
        if (isAdAvailable() && activity != null && !activity.isFinishing) {
            appOpenAd?.show(activity)
        } else {
            _listener?.onAdFailedToShow("Ad not ready or activity invalid")
            emitEvent(AdEvent.Failed(
                AdType.APP_OPEN,
                CustomAdError.ShowError(1, "Ad not ready or activity invalid"),
                getAdId()
            ))
            loadAd()
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = System.currentTimeMillis() - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    override fun destroy() {
        super.destroy()
        appOpenAd = null
    }

    fun showIfAvailable(activity: Activity): Boolean {
        if (isAdAvailable()) {
            showAd(activity)
            return true
        }
        loadAd()
        return false
    }
}