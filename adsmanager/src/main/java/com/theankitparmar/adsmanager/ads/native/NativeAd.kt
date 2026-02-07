// NativeAd.kt
package com.theankitparmar.adsmanager.ads.native

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.theankitparmar.adsmanager.R
import com.theankitparmar.adsmanager.adInterface.*
import com.theankitparmar.adsmanager.ads.AdState
import com.theankitparmar.adsmanager.ads.BaseAd
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class NativeAd(
    context: Context,
    adUnitId: String,
    config: AdsConfiguration
) : BaseAd<AdState>(
    context = context,
    adUnitId = adUnitId,
    config = config,
    adType = AdType.NATIVE
) {

    private var nativeAd: NativeAd? = null
    private var adLoader: AdLoader? = null

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/2247696110"

    override suspend fun loadAdInternal(): AdResult<Unit> = withContext(Dispatchers.Main) {
        return@withContext try {
            val context = getContext() ?: return@withContext AdResult.Error("Context is null")

            val result = suspendCancellableCoroutine<AdResult<Unit>> { continuation ->
                adLoader = AdLoader.Builder(context, getAdUnitId())
                    .forNativeAd { ad ->
                        nativeAd?.destroy()
                        nativeAd = ad

                        ad.setOnPaidEventListener { adValue ->
                            _listener?.onAdRevenue(adValue)
                            emitEvent(AdEvent.Revenue(
                                AdType.NATIVE,
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
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            _listener?.onAdFailedToLoad(error.message)
                            emitEvent(AdEvent.Failed(
                                AdType.NATIVE,
                                CustomAdError.fromLoadAdError(error),
                                getAdId()
                            ))

                            if (!continuation.isCancelled) {
                                continuation.resume(AdResult.Error(error.message))
                            }
                        }

                        override fun onAdClicked() {
                            _listener?.onAdClicked()
                            emitEvent(AdEvent.Clicked(AdType.NATIVE, getAdId()))
                        }

                        override fun onAdImpression() {
                            _listener?.onAdImpression()
                            emitEvent(AdEvent.Impression(AdType.NATIVE, getAdId()))
                        }
                    })
                    .withNativeAdOptions(
                        NativeAdOptions.Builder()
                            .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                            .build())
                    .build()

                adLoader?.loadAd(AdRequest.Builder().build())
            }

            result
        } catch (e: Exception) {
            AdResult.Error(e.message ?: "Unknown error")
        }
    }

    fun inflateNativeAdView(
        container: ViewGroup,
        layoutResId: Int
    ): NativeAdView? {
        val ad = nativeAd ?: return null
        val context = getContext() ?: return null

        val adView = LayoutInflater.from(context)
            .inflate(layoutResId, container, false) as? NativeAdView ?: return null

        populateNativeAdView(ad, adView)

        container.removeAllViews()
        container.addView(adView)

        return adView
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_icon)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        adView.mediaView?.setMediaContent(nativeAd.mediaContent!!)

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        if (nativeAd.starRating == null) {
            adView.starRatingView?.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            adView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        }

        adView.setNativeAd(nativeAd)
    }

    override fun showAd(activity: Activity?) {}

    override fun destroy() {
        super.destroy()
        nativeAd?.destroy()
        nativeAd = null
        adLoader = null
    }
}