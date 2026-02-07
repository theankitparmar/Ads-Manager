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
    // Reusable interstitial ad instance
    private var reusableInterstitialAd: InterstitialAd? = null
    // Add this to AdHelper.kt - Reuse App Open Ad instance
    private var reusableAppOpenAd: com.theankitparmar.adsmanager.ads.open.AppOpenAd? = null


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
            override fun onAdLoading() {
                Log.d(TAG, "Banner ad is loading...")
            }

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

    // Get or create a reusable interstitial ad
    fun getReusableInterstitialAd(activity: Activity): InterstitialAd {
        val key = "interstitial_reusable"

        return if (interstitialAds.containsKey(key)) {
            interstitialAds[key]!!
        } else {
            val interstitialAd = AdsManager.getInterstitialAd(activity)
            interstitialAds[key] = interstitialAd
            interstitialAd
        }
    }

    // Clear the reusable interstitial ad
    fun clearReusableInterstitialAd() {
        val key = "interstitial_reusable"
        interstitialAds.remove(key)?.destroy()
        reusableInterstitialAd = null
    }

// AdHelper.kt

    fun showInterstitialAd(
        activity: Activity,
        showLoadingDialog: Boolean = true,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ) {
        Log.d(TAG, "Attempting to show an Interstitial ad.")

        val interstitialAd = getReusableInterstitialAd(activity)

        // Check if ad is already loaded
        if (interstitialAd.isLoaded()) {
            Log.d(TAG, "Interstitial ad was already cached, showing immediately.")

            // Before showing, set a listener just to handle the DISMISSAL
            interstitialAd.setListener(object : AdListener {
                override fun onAdDismissed() {
                    interstitialAd.setListener(null) // IMPORTANT: Clear listener so reloads don't trigger this
                    onAdDismissed?.invoke()
                }
                override fun onAdFailedToShow(error: String) {
                    interstitialAd.setListener(null)
                    onAdFailed?.invoke(error)
                }
                // ... implement other empty methods or use a BaseListener
                override fun onAdLoading() {}
                override fun onAdLoaded() {}
                override fun onAdFailedToLoad(error: String) {}
                override fun onAdClicked() {}
                override fun onAdImpression() {}
                override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
            })

            interstitialAd.showAd(activity)
            return // Exit here, we've handled the show
        }

        // If NOT loaded, we load it and show it ONCE when finished
        if (showLoadingDialog) {
            showLoadingDialog(activity, "Loading ad...")
        }

        interstitialAd.setListener(object : AdListener {
            override fun onAdLoaded() {
                Log.d(TAG, "Interstitial ad loaded, showing now.")
                dismissLoadingDialog()
                interstitialAd.showAd(activity)
                // Note: We DON'T clear the listener here yet because we need
                // to hear the onAdDismissed event below.
            }

            override fun onAdDismissed() {
                Log.d(TAG, "Interstitial ad was closed.")
                interstitialAd.setListener(null) // <--- CRITICAL FIX: Stop listening after dismissal
                dismissLoadingDialog()
                onAdDismissed?.invoke()
            }

            override fun onAdFailedToLoad(error: String) {
                interstitialAd.setListener(null) // <--- CRITICAL FIX: Stop listening if load fails
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdFailedToShow(error: String) {
                interstitialAd.setListener(null) // <--- CRITICAL FIX
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdLoading() {}
            override fun onAdClicked() {}
            override fun onAdImpression() {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })

        interstitialAd.loadAd()
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
            override fun onAdLoading() {
                Log.d(TAG, "Native ad is loading...")
            }

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

    // Inside AdHelper.kt

    // Updated showAppOpenAd method in AdHelper.kt
    fun showAppOpenAd(
        activity: Activity,
        showLoadingDialog: Boolean = true,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ): Boolean {
        Log.d(TAG, "Attempting to show an App Open ad.")

        // Reuse or create App Open Ad instance
        val appOpenAd = reusableAppOpenAd ?: AdsManager.getAppOpenAd(activity).also {
            reusableAppOpenAd = it
        }

        if (showLoadingDialog) {
            showLoadingDialog(activity, "Loading...")
        }

        // Clear previous listener to avoid conflicts
        appOpenAd.setListener(null)

        appOpenAd.setListener(object : AdListener {
            override fun onAdLoading() {
                Log.d(TAG, "App Open ad is loading...")
            }

            override fun onAdLoaded() {
                Log.d(TAG, "App Open ad loaded successfully.")
                dismissLoadingDialog()
                // Show immediately when loaded
                appOpenAd.showIfAvailable(activity)
            }

            override fun onAdDismissed() {
                Log.d(TAG, "App Open ad was dismissed.")
                appOpenAd.setListener(null) // Clear listener
                dismissLoadingDialog()
                onAdDismissed?.invoke()
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "App Open ad failed to load. Error: $error")
                appOpenAd.setListener(null) // Clear listener
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdFailedToShow(error: String) {
                Log.e(TAG, "App Open ad failed to show. Error: $error")
                appOpenAd.setListener(null) // Clear listener
                dismissLoadingDialog()
                onAdFailed?.invoke(error)
            }

            override fun onAdClicked() {
                Log.d(TAG, "The user clicked on the App Open ad.")
            }

            override fun onAdImpression() {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })

        // Check if ad is already available
        return if (appOpenAd.isLoaded()) {
            Log.d(TAG, "App Open ad already loaded, showing immediately.")
            dismissLoadingDialog()
            appOpenAd.showIfAvailable(activity)
            true
        } else {
            // Only load if not already loading
            appOpenAd.loadAd()
            false
        }
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

    // Add this method to AdHelper.kt
    fun preloadAppOpenAd(context: Context) {
        try {
            if (AdsManager.isInitialized()) {
                // Get the app open ad (this will load it)
                reusableAppOpenAd = AdsManager.getAppOpenAd(context)
                Log.d(TAG, "App Open Ad preloaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload App Open Ad", e)
        }
    }
}