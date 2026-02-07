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

class BannerAd(
    private val bannerContext: Context,  // store the context
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

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/6300978111"


    private fun createAdView(): AdView {
        val context = getContext() ?: throw IllegalStateException("Context is null")
        return AdView(context).apply {
            this.adUnitId = if (config.isTestMode) getTestAdUnitId() else adUnitId
            this.setAdSize(currentAdSize)

        }
    }

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")

            val result = suspendCancellableCoroutine<AdResult<Unit>> { continuation ->
                adView = createAdView()

                adView?.adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        _listener?.onAdLoaded()
                        emitEvent(AdEvent.Loaded(AdType.BANNER, getAdId()))

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

                        if (!continuation.isCancelled) {
                            continuation.resume(AdResult.Error(error.message))
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
                }

                val adRequest = AdRequest.Builder().build()
                adView?.loadAd(adRequest)
            }

            result
        } catch (e: Exception) {
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun showAd(activity: android.app.Activity?) {
        loadAd()
    }

    override fun destroy() {
        super.destroy()
        adView?.let { adView ->
            (adView.parent as? ViewGroup)?.removeView(adView)
            adView.destroy()
            this.adView = null
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