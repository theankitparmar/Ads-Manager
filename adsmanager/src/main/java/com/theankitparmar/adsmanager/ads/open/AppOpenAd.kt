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
    private var retryAttempt = 0
    private var canLoadAgain = true // Cooldown flag
    private var lastLoadAttemptTime: Long = 0

    override val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/9257395921"

    // CRITICAL FIX: Add cooldown mechanism
    private fun canAttemptLoad(): Boolean {
        if (!canLoadAgain) return false

        // Check if enough time has passed since last attempt
        val timeSinceLastAttempt = System.currentTimeMillis() - lastLoadAttemptTime
        val minCooldown = when {
            retryAttempt == 0 -> 0L // First attempt, no cooldown
            retryAttempt <= 2 -> 15000L // 15 seconds for first 2 retries
            retryAttempt <= 5 -> 30000L // 30 seconds for next 3 retries
            else -> 60000L // 60 seconds for subsequent retries
        }

        return timeSinceLastAttempt >= minCooldown
    }

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        // Remove cooldown check - always try to load

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
                                    // Don't auto-reload - let AdHelper handle it
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
                            _listener?.onAdFailedToLoad(error.message)

                            // Simple retry logic - just report failure
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
            // Don't load immediately - wait for cooldown
            if (canAttemptLoad()) {
                loadAd()
            }
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
            // Only load if we're not in cooldown
            if (canAttemptLoad()) {
                loadAd()
            }
            false
        }
    }
}