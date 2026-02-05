// BannerAd.kt
package com.theankitparmar.adsmanager.ads.banner

import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.theankitparmar.adsmanager.adInterface.AdsConfiguration
import com.theankitparmar.adsmanager.ads.AdState
import com.theankitparmar.adsmanager.ads.BaseAd
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BannerAd(
    private val bannerContext: Context,  // store the context
    adUnitId: String,
    config: AdsConfiguration,
    val adSize: AdSize = AdSize.BANNER
) : BaseAd<AdState>(bannerContext, adUnitId, config) {

    val currentAdSize: AdSize = adSize
    private var adView: AdView? = null

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/6300978111"


    private fun createAdView(): AdView {
        return AdView(bannerContext).apply {  // use the stored context
            this.adUnitId = if (config.isTestMode) getTestAdUnitId() else adUnitId
            this.setAdSize(currentAdSize)
            this.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdLoaded() {
                    _listener?.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    _listener?.onAdFailedToLoad(error.message)
                }

                override fun onAdClicked() {
                    _listener?.onAdClicked()
                }

                override fun onAdImpression() {
                    _listener?.onAdImpression()
                }
            }
        }
    }

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            // Create a new AdView instance
            adView = createAdView()

            val adRequest = AdRequest.Builder().build()
            adView?.loadAd(adRequest)
            AdResult.Loading
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