package com.theankitparmar.adsmanager.ads.open

import android.app.Activity
import android.content.Context
import android.util.Log
import com.theankitparmar.adsmanager.adInterface.*
import com.theankitparmar.adsmanager.ads.AdState
import com.theankitparmar.adsmanager.ads.BaseAd
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.*
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

    override val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/9257395921"

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        // ALWAYS try to load - no cooldown checks
        if (isAdAvailable()) {
            return@withContext AdResult.Success(Unit)
        }

        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()

            suspendCancellableCoroutine<AdResult<Unit>> { continuation ->
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
                                    Log.d("AppOpenAd", "Ad dismissed")
                                    appOpenAd = null
                                    _listener?.onAdDismissed()
                                    // DO NOT auto-reload here - let AdHelper handle it
                                }

                                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                    Log.e("AppOpenAd", "Ad failed to show: ${error.message}")
                                    appOpenAd = null
                                    _listener?.onAdFailedToShow(error.message)
                                }

                                override fun onAdImpression() {
                                    _listener?.onAdImpression()
                                }

                                override fun onAdClicked() {
                                    _listener?.onAdClicked()
                                }
                            }

                            if (!continuation.isCancelled) continuation.resume(AdResult.Success(Unit))
                        }

                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            Log.e("AppOpenAd", "Ad failed to load: ${error.message}")
                            _listener?.onAdFailedToLoad(error.message)

                            // Simple failure - just report, no retry logic
                            if (!continuation.isCancelled) continuation.resume(AdResult.Error(error.message))
                        }
                    }
                )
            }
        } catch (e: Exception) {
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun showAd(activity: Activity?) {
        if (isAdAvailable() && activity != null && !activity.isFinishing) {
            appOpenAd?.show(activity)
        } else {
            _listener?.onAdFailedToShow("Ad not ready")
            // DO NOT auto-load here - AdHelper will handle it
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = System.currentTimeMillis() - loadTime
        return dateDifference < (3600000 * numHours)
    }

    fun showIfAvailable(activity: Activity): Boolean {
        return if (isAdAvailable()) {
            showAd(activity)
            true
        } else {
            false
        }
    }

    // Add this method to check if ad is loaded (used by AdHelper)
    override fun isLoaded(): Boolean {
        return isAdAvailable()
    }
}