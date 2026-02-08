// NativeAd.kt (updated)
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
    config: AdsConfiguration,
    private val nativeType: NativeType = NativeType.MEDIUM,
    private val customNativeLayoutResId: Int? = null,
    private val customShimmerLayoutResId: Int? = null
) : BaseAd<AdState>(
    context = context,
    adUnitId = adUnitId,
    config = config,
    adType = AdType.NATIVE
) {

    private var nativeAd: NativeAd? = null
    private var adLoader: AdLoader? = null

    override fun getTestAdUnitId(): String = "ca-app-pub-3940256099942544/2247696110"

    // Get layout resource ID based on native type
    private fun getLayoutResId(): Int {
        return when (nativeType) {
            NativeType.SMALL -> R.layout.native_ad_small
            NativeType.MEDIUM -> R.layout.native_ad_medium
            NativeType.LARGE -> R.layout.native_ad_large
            NativeType.FULL_SCREEN -> R.layout.native_ad_full_screen
            NativeType.CUSTOM -> customNativeLayoutResId ?: R.layout.native_ad_medium
        }
    }

    // Get shimmer layout resource ID
    fun getShimmerLayoutResId(): Int? {
        return if (nativeType == NativeType.CUSTOM) {
            customShimmerLayoutResId
        } else {
            when (nativeType) {
                NativeType.SMALL -> R.layout.shimmer_native_small
                NativeType.MEDIUM -> R.layout.shimmer_native_medium
                NativeType.LARGE -> R.layout.shimmer_native_large
                NativeType.FULL_SCREEN -> R.layout.shimmer_native_full_screen
                else -> null
            }
        }
    }

    // NativeAd.kt - Update getExpectedHeight to use getExactHeightDp
    fun getExpectedHeight(context: Context): Int {
        return when (nativeType) {
            NativeType.SMALL, NativeType.MEDIUM, NativeType.LARGE -> {
                // Get exact height in dp and convert to pixels
                val heightDp = getExactHeightDp()
                if (heightDp > 0) {
                    (heightDp * context.resources.displayMetrics.density).toInt()
                } else {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            NativeType.FULL_SCREEN -> ViewGroup.LayoutParams.MATCH_PARENT
            NativeType.CUSTOM -> {
                customNativeLayoutResId?.let { layoutId ->
                    try {
                        // Try to measure custom layout
                        val inflater = LayoutInflater.from(context)
                        val previewView = inflater.inflate(layoutId, null as ViewGroup?, false)
                        previewView.measure(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        )
                        previewView.measuredHeight
                    } catch (e: Exception) {
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                } ?: ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

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
                        override fun onAdFailedToLoad(error: LoadAdError) {
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
        layoutResId: Int? = null
    ): NativeAdView? {
        val ad = nativeAd ?: return null
        val context = getContext() ?: return null

        val layoutId = layoutResId ?: getLayoutResId()

        val adView = LayoutInflater.from(context)
            .inflate(layoutId, container, false) as? NativeAdView ?: return null

        populateNativeAdView(ad, adView)
        return adView
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Headline
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        (adView.headlineView as? TextView)?.text = nativeAd.headline

        // Media content
        adView.mediaView = adView.findViewById(R.id.ad_media)
        if (nativeAd.mediaContent != null && adView.mediaView != null) {
            adView.mediaView?.setMediaContent(nativeAd.mediaContent!!)
            adView.mediaView?.visibility = View.VISIBLE
        } else {
            adView.mediaView?.visibility = View.GONE
        }

        // Body
        adView.bodyView = adView.findViewById(R.id.ad_body)
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        // Call to Action
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        }

        // Icon
        adView.iconView = adView.findViewById(R.id.ad_icon)
        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        // Star Rating
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        if (nativeAd.starRating == null) {
            adView.starRatingView?.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        }

        // Advertiser
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        if (nativeAd.advertiser == null) {
            adView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        }

        // Store (if exists)
//        val storeView = adView.findViewById<TextView>(R.id.ad_store)
//        if (nativeAd.store == null) {
//            storeView?.visibility = View.INVISIBLE
//        } else {
//            storeView?.text = nativeAd.store
//            storeView?.visibility = View.VISIBLE
//        }
//
//        // Price (if exists)
//        val priceView = adView.findViewById<TextView>(R.id.ad_price)
//        if (nativeAd.price == null) {
//            priceView?.visibility = View.INVISIBLE
//        } else {
//            priceView?.text = nativeAd.price
//            priceView?.visibility = View.VISIBLE
//        }

        adView.setNativeAd(nativeAd)
    }

    // NativeAd.kt - Add this method
        fun getExactHeightDp(): Int {
        return when (nativeType) {
            NativeType.SMALL -> 72
            NativeType.MEDIUM -> 140
            NativeType.LARGE -> 380
            NativeType.FULL_SCREEN -> -1 // Full screen
            NativeType.CUSTOM -> -2 // Custom/Wrap content
        }
    }

    fun getNativeType(): NativeType = nativeType

    override fun showAd(activity: Activity?) {}

    override fun destroy() {
        super.destroy()
        nativeAd?.destroy()
        nativeAd = null
        adLoader = null
    }
}