package com.theankitparmar.adsmanager.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.google.android.gms.ads.AdSize
import com.theankitparmar.adsmanager.callbacks.AdListener
import com.theankitparmar.adsmanager.core.AdsManager
import com.theankitparmar.adsmanager.ads.banner.BannerAd
import com.theankitparmar.adsmanager.ads.inter.InterstitialAd
import com.theankitparmar.adsmanager.ads.native.NativeAd

object AdHelper {

    private const val TAG = "AdHelper"

    private val bannerAds = mutableMapOf<String, BannerAd>()
    private val interstitialAds = mutableMapOf<String, InterstitialAd>()
    private val nativeAds = mutableMapOf<String, NativeAd>()
    private val shimmerDrawables = mutableMapOf<Int, ShimmerDrawable>()
    private var loadingDialog: AlertDialog? = null

    fun showBannerAd(
        context: Context,
        container: ViewGroup,
        showShimmer: Boolean = true,
        adSize: AdSize = AdSize.BANNER,
        onAdLoaded: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ): BannerAd {
        Log.d(TAG, "Attempting to load a Banner ad.")

        val shimmerContainer = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                adSize.getHeightInPixels(context)
            )
            id = View.generateViewId()
        }

        container.removeAllViews()
        container.addView(shimmerContainer)

        if (showShimmer) {
            showShimmerEffect(context, shimmerContainer)
        }

        val bannerAd = AdsManager.getBannerAd(context, adSize)
        val key = "banner_${container.id}"
        bannerAds[key] = bannerAd

        bannerAd.setListener(object : AdListener {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully and is now visible.")
                hideShimmerEffect(shimmerContainer)
                container.removeView(shimmerContainer)

                val adView = bannerAd.getAdView()
                if (adView != null) {
                    // Remove from any existing parent
                    (adView.parent as? ViewGroup)?.removeView(adView)

                    // Set layout params
                    adView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    // Add to container
                    container.removeAllViews()
                    container.addView(adView)
                    onAdLoaded?.invoke()
                }
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "Banner ad failed to load. Error: $error")
                hideShimmerEffect(shimmerContainer)
                container.removeView(shimmerContainer)
                onAdFailed?.invoke(error)
            }

            override fun onAdClicked() {
                Log.d(TAG, "The user clicked on the Banner ad.")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Banner ad impression recorded.")
            }

            override fun onAdDismissed() {}
            override fun onAdFailedToShow(error: String) {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })

        return bannerAd
    }


    fun showInterstitialAd(
        activity: Activity,
        showLoadingDialog: Boolean = true,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ) {
        Log.d(TAG, "Attempting to show an Interstitial ad.")

        if (showLoadingDialog) {
            showLoadingDialog(activity, "Loading ad...")
        }

        val interstitialAd = AdsManager.getInterstitialAd(activity)
        val key = "interstitial_${activity.localClassName}"
        interstitialAds[key] = interstitialAd

        interstitialAd.setListener(object : AdListener {
            override fun onAdLoaded() {
                Log.d(TAG, "Interstitial ad loaded successfully.")
                dismissLoadingDialog()
                if (interstitialAd.isLoaded()) {
                    Log.d(TAG, "Displaying the Interstitial ad now.")
                    interstitialAd.showAd(activity)
                }
            }

            override fun onAdDismissed() {
                Log.d(TAG, "Interstitial ad was closed by the user.")
                dismissLoadingDialog()
                onAdDismissed?.invoke()
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "Interstitial ad failed to load. Error: $error")
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdClicked() {
                Log.d(TAG, "The user clicked on the Interstitial ad.")
            }

            override fun onAdFailedToShow(error: String) {
                Log.e(TAG, "Interstitial ad failed to show. Error: $error")
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdImpression() {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })

        if (interstitialAd.isLoaded()) {
            Log.d(TAG, "Interstitial ad was already cached, showing immediately.")
            dismissLoadingDialog()
            interstitialAd.showAd(activity)
        }
    }

    fun showNativeAd(
        context: Context,
        container: ViewGroup,
        layoutResId: Int,
        showShimmer: Boolean = true,
        onAdLoaded: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ) {
        Log.d(TAG, "Attempting to load a Native ad.")

        if (showShimmer) {
            showShimmerEffect(context, container)
        }

        val nativeAd = AdsManager.getNativeAd(context)
        val key = "native_${container.id}"
        nativeAds[key] = nativeAd

        nativeAd.setListener(object : AdListener {
            override fun onAdLoaded() {
                Log.d(TAG, "Native ad loaded successfully.")
                hideShimmerEffect(container)
                nativeAd.inflateNativeAdView(container, layoutResId)
                onAdLoaded?.invoke()
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "Native ad failed to load. Error: $error")
                hideShimmerEffect(container)
                onAdFailed?.invoke(error)
            }

            override fun onAdClicked() {
                Log.d(TAG, "The user clicked on the Native ad.")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Native ad impression recorded.")
            }

            override fun onAdDismissed() {}
            override fun onAdFailedToShow(error: String) {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })
    }

    fun showAppOpenAd(
        activity: Activity,
        showLoadingDialog: Boolean = true,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ): Boolean {
        Log.d(TAG, "Attempting to show an App Open ad.")

        if (showLoadingDialog) {
            showLoadingDialog(activity, "Loading...")
        }

        val appOpenAd = AdsManager.getAppOpenAd(activity)

        appOpenAd.setListener(object : AdListener {
            override fun onAdLoaded() {
                Log.d(TAG, "App Open ad loaded successfully.")
                dismissLoadingDialog()
                appOpenAd.showIfAvailable(activity)
            }

            override fun onAdDismissed() {
                Log.d(TAG, "App Open ad was dismissed.")
                dismissLoadingDialog()
                onAdDismissed?.invoke()
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "App Open ad failed to load. Error: $error")
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdClicked() {
                Log.d(TAG, "The user clicked on the App Open ad.")
            }

            override fun onAdFailedToShow(error: String) {
                Log.e(TAG, "App Open ad failed to show. Error: $error")
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdImpression() {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })

        return appOpenAd.showIfAvailable(activity)
    }

    private fun showShimmerEffect(context: Context, container: ViewGroup) {
        val shimmerDrawable = ShimmerDrawable().apply {
            setShimmer(
                Shimmer.AlphaHighlightBuilder()
                    .setDuration(1000L)
                    .setBaseAlpha(0.7f)
                    .setHighlightAlpha(0.9f)
                    .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                    .setTilt(20f)
                    .setAutoStart(true)
                    .build()
            )
        }

        val placeholder = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                100.dpToPx(context)
            )
            background = shimmerDrawable
        }

        container.removeAllViews()
        container.addView(placeholder)
        shimmerDrawable.startShimmer()
        shimmerDrawables[container.id] = shimmerDrawable
    }

    private fun hideShimmerEffect(container: ViewGroup) {
        shimmerDrawables[container.id]?.apply {
            stopShimmer()
            callback = null
        }
        shimmerDrawables.remove(container.id)
        container.removeAllViews()
    }

    private fun showLoadingDialog(activity: Activity, message: String) {
        if (activity.isFinishing) return
        dismissLoadingDialog()
        loadingDialog = AlertDialog.Builder(activity)
            .setMessage(message)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    fun pauseBannerAds() {
        Log.d(TAG, "Pausing all active Banner ads.")
        bannerAds.values.forEach { it.pause() }
    }

    fun resumeBannerAds() {
        Log.d(TAG, "Resuming all active Banner ads.")
        bannerAds.values.forEach { it.resume() }
    }

    fun destroyAds() {
        Log.d(TAG, "Destroying all ads and clearing references.")
        bannerAds.values.forEach { it.destroy() }
        interstitialAds.values.forEach { it.destroy() }
        nativeAds.values.forEach { it.destroy() }
        bannerAds.clear()
        interstitialAds.clear()
        nativeAds.clear()
        shimmerDrawables.values.forEach { it.stopShimmer() }
        shimmerDrawables.clear()
        dismissLoadingDialog()
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
