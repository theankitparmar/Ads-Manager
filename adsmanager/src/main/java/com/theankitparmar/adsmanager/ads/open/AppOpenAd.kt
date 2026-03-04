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

/**
 * App Open Ad implementation for Google AdMob
 * Displays ads when app returns from background with 4-hour expiration window
 * 
 * Features:
 * - Background-to-foreground ad display
 * - 4-hour ad freshness window
 * - Automatic cooldown between shows
 * - Revenue tracking
 * 
 * @param context The Android context
 * @param adUnitId The AdMob app open ad unit ID
 * @param config Ad configuration settings
 */
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
            Log.d("AppOpenAd", "✓ Ad already available, skipping reload")
            return@withContext AdResult.Success(Unit)
        }

        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()

            suspendCancellableCoroutine<AdResult<Unit>> { continuation ->
                try {
                    com.google.android.gms.ads.appopen.AppOpenAd.load(
                        context,
                        getAdUnitId(),
                        adRequest,
                        com.google.android.gms.ads.appopen.AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                        object : com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback() {
                            override fun onAdLoaded(ad: com.google.android.gms.ads.appopen.AppOpenAd) {
                                appOpenAd = ad
                                loadTime = System.currentTimeMillis()
                                Log.d("AppOpenAd", "✓ App open ad loaded successfully")

                                ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        Log.d("AppOpenAd", "✓ Ad dismissed by user")
                                        appOpenAd = null
                                        _listener?.onAdDismissed()
                                        emitEvent(AdEvent.Dismissed(AdType.APP_OPEN, getAdId()))
                                    }

                                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                        Log.e("AppOpenAd", "✗ Ad failed to show: ${error.message}")
                                        appOpenAd = null
                                        _listener?.onAdFailedToShow(error.message)
                                        emitEvent(AdEvent.Failed(
                                            AdType.APP_OPEN,
                                            CustomAdError.ShowError(error.code, error.message),
                                            getAdId()
                                        ))
                                    }

                                    override fun onAdImpression() {
                                        Log.d("AppOpenAd", "Ad impression recorded")
                                        _listener?.onAdImpression()
                                        emitEvent(AdEvent.Impression(AdType.APP_OPEN, getAdId()))
                                    }

                                    override fun onAdClicked() {
                                        Log.d("AppOpenAd", "Ad clicked")
                                        _listener?.onAdClicked()
                                        emitEvent(AdEvent.Clicked(AdType.APP_OPEN, getAdId()))
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Log.d("AppOpenAd", "✓ Ad displayed on screen")
                                        emitEvent(AdEvent.Opened(AdType.APP_OPEN, getAdId()))
                                    }
                                }

                                // Set revenue callback
                                ad.setOnPaidEventListener { adValue ->
                                    Log.d("AppOpenAd", "Revenue: ${adValue.valueMicros} ${adValue.currencyCode}")
                                    _listener?.onAdRevenue(adValue)
                                    emitEvent(AdEvent.Revenue(
                                        AdType.APP_OPEN,
                                        adValue.valueMicros,
                                        adValue.currencyCode,
                                        adValue.precisionType,
                                        getAdId()
                                    ))
                                }

                                if (!continuation.isCancelled) continuation.resume(AdResult.Success(Unit))
                            }

                            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                Log.e("AppOpenAd", "✗ Ad failed to load: ${error.message}")
                                _listener?.onAdFailedToLoad(error.message)
                                emitEvent(AdEvent.Failed(
                                    AdType.APP_OPEN,
                                    CustomAdError.fromLoadAdError(error),
                                    getAdId()
                                ))

                                if (!continuation.isCancelled) continuation.resume(AdResult.Error(error.message ?: "Unknown error"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("AppOpenAd", "Exception during load: ${e.message}", e)
                    if (!continuation.isCancelled) {
                        continuation.resume(AdResult.Error("Exception: ${e.message}"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppOpenAd", "Error in loadAdInternal: ${e.message}", e)
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun showAd(activity: Activity?) {
        if (isAdAvailable() && activity != null && !activity.isFinishing) {
            try {
                appOpenAd?.show(activity)
                Log.d("AppOpenAd", "Attempting to show app open ad")
            } catch (e: Exception) {
                Log.e("AppOpenAd", "Error showing ad: ${e.message}", e)
                _listener?.onAdFailedToShow("Error: ${e.message}")
            }
        } else {
            val reason = when {
                !isAdAvailable() -> "Ad not available or expired"
                activity == null -> "Activity is null"
                activity.isFinishing -> "Activity is finishing"
                else -> "Unknown"
            }
            Log.w("AppOpenAd", "Cannot show ad: $reason")
            _listener?.onAdFailedToShow(reason)
        }
    }

    /**
     * Check if ad is currently available and not expired
     * 
     * @return true if ad is loaded and fresh (less than 4 hours old)
     */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /**
     * Check if ad was loaded within N hours
     * 
     * @param numHours Number of hours to check
     * @return true if ad load time is within N hours
     */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = System.currentTimeMillis() - loadTime
        val result = dateDifference < (3600000 * numHours)
        Log.d("AppOpenAd", "Ad freshness check: ${dateDifference}ms old, valid for ${3600000 * numHours}ms - $result")
        return result
    }

    /**
     * Show app open ad if available, without waiting
     * 
     * @param activity The activity to show the ad in
     * @return true if ad was shown, false if not available
     */
    fun showIfAvailable(activity: Activity): Boolean {
        return if (isAdAvailable()) {
            showAd(activity)
            true
        } else {
            false
        }
    }

    /**
     * Check if ad is currently loaded (inherited from AdManager interface)
     */
    override fun isLoaded(): Boolean {
        return isAdAvailable()
    }

    override fun destroy() {
        super.destroy()
        try {
            appOpenAd = null
            loadTime = 0
            Log.d("AppOpenAd", "AppOpenAd destroyed")
        } catch (e: Exception) {
            Log.e("AppOpenAd", "Error during destroy: ${e.message}")
        }
    }
}