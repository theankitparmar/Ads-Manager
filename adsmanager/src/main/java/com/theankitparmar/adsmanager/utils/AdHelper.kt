package com.theankitparmar.adsmanager.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import com.theankitparmar.adsmanager.ads.native.NativeType

object AdHelper {

    private const val TAG = "AdHelper"

    private val bannerAds = mutableMapOf<String, BannerAd>()
    private val interstitialAds = mutableMapOf<String, InterstitialAd>()
    private val nativeAds = mutableMapOf<String, NativeAd>()
    private val shimmerDrawables = mutableMapOf<Int, ShimmerDrawable>()
    private var loadingDialog: AlertDialog? = null
    private var reusableInterstitialAd: InterstitialAd? = null
    private var reusableAppOpenAd: com.theankitparmar.adsmanager.ads.open.AppOpenAd? = null

    // Add this to store custom shimmer views
    private val customShimmerViews = mutableMapOf<Int, View>()

    // Banner size options - Developers use these
    enum class BannerAdSize {
        STANDARD,      // 320x50
        LARGE,         // 320x100
        MEDIUM_RECTANGLE, // 300x250
        FULL_BANNER,   // 468x60
        LEADERBOARD,   // 728x90
        ADAPTIVE       // Automatically adjusts
    }

    // Smart Banner Ad with automatic size detection and simplified API
    fun showBannerAd(
        context: Context,
        container: ViewGroup,
        bannerAdSize: BannerAdSize = BannerAdSize.STANDARD,
        showShimmer: Boolean = true,
        onAdLoaded: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ): BannerAd {
        Log.d(TAG, "Loading Banner ad with size: $bannerAdSize")

        // Convert our enum to Google AdSize internally
        val adSize = when (bannerAdSize) {
            BannerAdSize.STANDARD -> AdSize.BANNER
            BannerAdSize.LARGE -> AdSize.LARGE_BANNER
            BannerAdSize.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
            BannerAdSize.FULL_BANNER -> AdSize.FULL_BANNER
            BannerAdSize.LEADERBOARD -> AdSize.LEADERBOARD
            BannerAdSize.ADAPTIVE -> {
                // For adaptive banners, we need an Activity context
                if (context is Activity) {
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 320)
                } else {
                    AdSize.BANNER // Fallback
                }
            }
        }

        return showBannerAdInternal(context, container, adSize, showShimmer, onAdLoaded, onAdFailed)
    }

    // Internal method that handles actual ad loading
    private fun showBannerAdInternal(
        context: Context,
        container: ViewGroup,
        adSize: AdSize,
        showShimmer: Boolean,
        onAdLoaded: (() -> Unit)?,
        onAdFailed: ((error: String) -> Unit)?
    ): BannerAd {
        // Calculate expected banner height in pixels
        val bannerHeightPx = adSize.getHeightInPixels(context)

        // Set container height to match the ad size
        container.post {
            container.layoutParams = container.layoutParams?.apply {
                height = bannerHeightPx
            }
        }

        // Clear any existing views
        container.removeAllViews()

        // Create shimmer placeholder with correct size
        val shimmerContainer = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                bannerHeightPx
            )
            id = View.generateViewId()
        }

        container.addView(shimmerContainer)

        if (showShimmer) {
            showShimmerEffect(context, shimmerContainer, bannerHeightPx)
        }

        val bannerAd = AdsManager.getBannerAd(context, adSize)
        val key = "banner_${container.id}"
        bannerAds[key] = bannerAd

        bannerAd.setListener(object : AdListener {
            override fun onAdLoading() {
                Log.d(TAG, "Banner ad is loading...")
            }

            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully")

                Handler(Looper.getMainLooper()).post {
                    hideShimmerEffect(shimmerContainer)
                    container.removeView(shimmerContainer)

                    val adView = bannerAd.getAdView()
                    if (adView != null) {
                        // Remove from any existing parent
                        (adView.parent as? ViewGroup)?.removeView(adView)

                        // Measure the actual ad to get exact dimensions
                        adView.measure(
                            View.MeasureSpec.makeMeasureSpec(
                                container.measuredWidth,
                                View.MeasureSpec.EXACTLY
                            ),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        )

                        val actualAdHeight = adView.measuredHeight

                        // Update container to match actual ad height
                        container.layoutParams = container.layoutParams?.apply {
                            height = actualAdHeight
                        }

                        // Set ad view layout params
                        adView.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )

                        // Add ad to container
                        container.removeAllViews()
                        container.addView(adView)

                        onAdLoaded?.invoke()
                    }
                }
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "Banner ad failed to load. Error: $error")
                Handler(Looper.getMainLooper()).post {
                    hideShimmerEffect(shimmerContainer)
                    container.removeView(shimmerContainer)
                    onAdFailed?.invoke(error)
                }
            }

            override fun onAdClicked() {
                Log.d(TAG, "Banner ad clicked")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Banner ad impression")
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

    /**
     * Smart Native Ad with size support
     */
    /**
     * Smart Native Ad with proper height calculation
     */
    fun showNativeAd(
        context: Context,
        container: ViewGroup,
        nativeType: NativeType = NativeType.MEDIUM,
        showShimmer: Boolean = true,
        customNativeLayoutResId: Int? = null,
        customShimmerLayoutResId: Int? = null,
        onAdLoaded: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ): NativeAd {
        Log.d(TAG, "Loading Native ad with type: $nativeType")

        // Get NativeAd with type configuration
        val nativeAd = when (nativeType) {
            NativeType.CUSTOM -> {
                if (customNativeLayoutResId == null) {
                    throw IllegalArgumentException("customNativeLayoutResId must be provided for CUSTOM type")
                }
                AdsManager.getNativeAdWithCustomLayout(
                    context = context,
                    nativeType = nativeType,
                    customNativeLayoutResId = customNativeLayoutResId,
                    customShimmerLayoutResId = customShimmerLayoutResId
                )
            }
            else -> {
                AdsManager.getNativeAdWithType(context, nativeType)
            }
        }

        val key = "native_${container.id}_${nativeType.name}"
        nativeAds[key] = nativeAd

        // Store original container state
        // Store a complete copy of layout params by creating a new instance
        val originalContainerParams = container.layoutParams?.let {
            ViewGroup.LayoutParams(it).apply {
                // Copy all properties
                width = it.width
                height = it.height
            }
        }

        // Use NativeAd's getExpectedHeight() method - THIS IS KEY
        val expectedHeight = nativeAd.getExpectedHeight(context)

        Log.d(TAG, "Native ad type: $nativeType, Expected height: $expectedHeight px")

        // Set container to expected height BEFORE adding shimmer
        container.layoutParams = container.layoutParams?.apply {
            height = if (nativeType == NativeType.FULL_SCREEN) {
                ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                expectedHeight
            }
        }

        // Clear any existing views
        container.removeAllViews()

        // Create shimmer with correct size
        if (showShimmer) {
            showNativeShimmer(
                context = context,
                container = container,
                nativeType = nativeType,
                expectedHeight = expectedHeight,
                customShimmerLayoutResId = customShimmerLayoutResId
            )
        }

        nativeAd.setListener(object : AdListener {
            override fun onAdLoading() {
                Log.d(TAG, "Native ad ($nativeType) is loading...")
            }

            override fun onAdLoaded() {
                Log.d(TAG, "Native ad ($nativeType) loaded successfully")

                Handler(Looper.getMainLooper()).post {
                    // Hide shimmer
                    hideNativeShimmer(container, nativeType)

                    // Inflate and display the native ad
                    val adView = nativeAd.inflateNativeAdView(container)
                    if (adView != null) {
                        // Set the ad view to fill the container
                        adView.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Clear container and add ad
                        container.removeAllViews()
                        container.addView(adView)

                        // Request layout update
                        container.requestLayout()
                    }

                    onAdLoaded?.invoke()
                }
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "Native ad ($nativeType) failed to load. Error: $error")
                Handler(Looper.getMainLooper()).post {
                    // Hide shimmer
                    hideNativeShimmer(container, nativeType)

                    // Restore original container layout params
                    originalContainerParams?.let {
                        container.layoutParams = it
                    }

                    onAdFailed?.invoke(error)
                }
            }

            override fun onAdClicked() {
                Log.d(TAG, "Native ad ($nativeType) clicked")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Native ad ($nativeType) impression")
            }

            override fun onAdDismissed() {}
            override fun onAdFailedToShow(error: String) {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })

        return nativeAd
    }


    /**
     * Show native shimmer based on type
     */
    private fun showNativeShimmer(
        context: Context,
        container: ViewGroup,
        nativeType: NativeType,
        expectedHeight: Int,
        customShimmerLayoutResId: Int? = null
    ) {
        hideNativeShimmer(container, nativeType)

        when (nativeType) {
            NativeType.CUSTOM -> {
                customShimmerLayoutResId?.let { layoutId ->
                    try {
                        val inflater = android.view.LayoutInflater.from(context)
                        val shimmerView = inflater.inflate(layoutId, container, false)

                        // Make sure shimmer matches container height
                        shimmerView.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            expectedHeight
                        )

                        container.addView(shimmerView)
                        customShimmerViews[container.id] = shimmerView
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to inflate custom shimmer layout: ${e.message}")
                        // Fallback to programmatic shimmer
                        showShimmerEffect(context, container, expectedHeight)
                    }
                } ?: run {
                    showShimmerEffect(context, container, expectedHeight)
                }
            }
            else -> {
                // For predefined types, get shimmer layout from NativeAd
                val shimmerLayoutResId = nativeAds.values.find { it.getNativeType() == nativeType }
                    ?.getShimmerLayoutResId()

                shimmerLayoutResId?.let { layoutId ->
                    try {
                        val inflater = android.view.LayoutInflater.from(context)
                        val shimmerView = inflater.inflate(layoutId, container, false)

                        // Make sure shimmer matches container height
                        shimmerView.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            expectedHeight
                        )

                        container.addView(shimmerView)
                        customShimmerViews[container.id] = shimmerView
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to inflate shimmer layout: ${e.message}")
                        // Fallback to programmatic shimmer
                        showShimmerEffect(context, container, expectedHeight)
                    }
                } ?: run {
                    // No predefined shimmer layout, use programmatic
                    showShimmerEffect(context, container, expectedHeight)
                }
            }
        }
    }

    /**
     * Hide native shimmer
     */
    private fun hideNativeShimmer(container: ViewGroup, nativeType: NativeType) {
        // Remove custom shimmer view if exists
        customShimmerViews[container.id]?.let { view ->
            container.removeView(view)
        }
        customShimmerViews.remove(container.id)

        // Also remove programmatic shimmer
        hideShimmerEffect(container)
    }

    /**
     * Show shimmer effect for native ad based on type
     */
    private fun showShimmerEffectForNative(
        context: Context,
        container: ViewGroup,
        nativeType: NativeType,
        customShimmerLayoutResId: Int? = null,
        expectedHeight: Int
    ) {
        hideShimmerEffect(container)

        when (nativeType) {
            NativeType.CUSTOM -> {
                customShimmerLayoutResId?.let { layoutId ->
                    // Inflate custom shimmer layout
                    try {
                        val inflater = android.view.LayoutInflater.from(context)
                        val shimmerView = inflater.inflate(layoutId, container, false)
                        container.addView(shimmerView)

                        // Store custom shimmer view
                        customShimmerViews[container.id] = shimmerView
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to inflate custom shimmer layout: ${e.message}")
                        // Fallback to default programmatic shimmer
                        showShimmerEffect(context, container, expectedHeight)
                    }
                } ?: run {
                    // No custom shimmer provided, use programmatic
                    showShimmerEffect(context, container, expectedHeight)
                }
            }
            else -> {
                // Use programmatic shimmer for predefined types
                showShimmerEffect(context, container, expectedHeight)
            }
        }
    }

    /**
     * Hide shimmer effect for native ad
     */
    private fun hideShimmerEffectForNative(container: ViewGroup, nativeType: NativeType) {
        when (nativeType) {
            NativeType.CUSTOM -> {
                // Remove custom shimmer view if exists
                customShimmerViews[container.id]?.let { view ->
                    container.removeView(view)
                }
                customShimmerViews.remove(container.id)
            }
            else -> {
                // Use default shimmer hiding
                hideShimmerEffect(container)
            }
        }
    }

    fun showAppOpenAd(
        activity: Activity,
        showLoadingDialog: Boolean = true,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: ((error: String) -> Unit)? = null
    ): Boolean {
        Log.d(TAG, "Attempting to show an App Open ad.")

        // Always get fresh instance to avoid state issues
        val appOpenAd = AdsManager.getAppOpenAd(activity)

        // Store for reuse, but don't destroy the old one yet
        reusableAppOpenAd?.let { oldAd ->
            // Don't destroy immediately, let GC handle it
            oldAd.destroy()
        }
        reusableAppOpenAd = appOpenAd

        if (showLoadingDialog) {
            showLoadingDialog(activity, "Loading...")
        }

        // Track if we're trying to load
        var isLoadingInProgress = false

        appOpenAd.setListener(object : AdListener {
            override fun onAdLoading() {
                Log.d(TAG, "App Open ad is loading...")
                isLoadingInProgress = true
            }

            override fun onAdLoaded() {
                Log.d(TAG, "App Open ad loaded successfully.")
                dismissLoadingDialog()
                isLoadingInProgress = false

                // Immediately show after loading
                appOpenAd.showIfAvailable(activity)
            }

            override fun onAdDismissed() {
                Log.d(TAG, "App Open ad was dismissed.")
                appOpenAd.setListener(null)
                dismissLoadingDialog()
                onAdDismissed?.invoke()

                // IMMEDIATELY preload for next time
                Handler(Looper.getMainLooper()).postDelayed({
                    preloadAppOpenAdForNextTime(activity)
                }, 500) // Small delay to ensure cleanup
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e(TAG, "App Open ad failed to load. Error: $error")
                appOpenAd.setListener(null)
                dismissLoadingDialog()
                onAdFailed?.invoke(error)

                // Try again after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    preloadAppOpenAdForNextTime(activity)
                }, 5000) // 5 seconds before retry
            }

            override fun onAdFailedToShow(error: String) {
                Log.e(TAG, "App Open ad failed to show. Error: $error")
                appOpenAd.setListener(null)
                dismissLoadingDialog()
                onAdFailed?.invoke(error)

                // Preload for next time
                preloadAppOpenAdForNextTime(activity)
            }

            override fun onAdClicked() {
                Log.d(TAG, "The user clicked on the App Open ad.")
            }

            override fun onAdImpression() {}
            override fun onAdRevenue(valueMicros: Long, currencyCode: String, precision: Int) {}
        })

        // Check if ad is already loaded
        return if (appOpenAd.isLoaded()) {
            Log.d(TAG, "App Open ad already loaded, showing immediately.")
            dismissLoadingDialog()
            appOpenAd.showIfAvailable(activity)
            true
        } else {
            Log.d(TAG, "App Open ad not loaded, starting load...")
            // Only load if not already loading
            if (!isLoadingInProgress) {
                appOpenAd.loadAd()
            }
            false
        }
    }

    // Add this method to AdHelper.kt - Preload without showing
    private fun preloadAppOpenAdForNextTime(context: Context) {
        Log.d(TAG, "Preloading App Open Ad for next foreground...")

        // Clear old instance
        reusableAppOpenAd?.let {
            it.destroy()
            reusableAppOpenAd = null
        }

        // Create and load new instance in background
        try {
            reusableAppOpenAd = AdsManager.getAppOpenAd(context)
            reusableAppOpenAd?.loadAd()
            Log.d(TAG, "App Open Ad preloaded successfully for next foreground")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload App Open Ad", e)
        }
    }

    // Add this new method to AdHelper.kt for reloading
    private fun reloadAppOpenAd(context: Context) {
        Log.d(TAG, "Reloading App Open Ad...")

        // Clear the existing instance
        reusableAppOpenAd?.destroy()
        reusableAppOpenAd = null

        // Create and load new instance
        reusableAppOpenAd = AdsManager.getAppOpenAd(context)
        reusableAppOpenAd?.loadAd()
    }

    // Smart Shimmer Effect - Automatically sizes to match target
    private fun showShimmerEffect(
        context: Context,
        container: ViewGroup,
        heightPx: Int
    ) {
        // Clean up any existing shimmer
        hideShimmerEffect(container)

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
                heightPx
            )
            background = shimmerDrawable
        }

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

        // Find and remove shimmer placeholder
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.background is ShimmerDrawable) {
                container.removeView(child)
                break
            }
        }
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

    // Update the destroyAds method to handle reusableAppOpenAd
    fun destroyAds() {
        Log.d(TAG, "Destroying all ads and clearing references.")
        bannerAds.values.forEach { it.destroy() }
        interstitialAds.values.forEach { it.destroy() }
        nativeAds.values.forEach { it.destroy() }
        reusableAppOpenAd?.destroy() // Add this line
        bannerAds.clear()
        interstitialAds.clear()
        nativeAds.clear()
        shimmerDrawables.values.forEach { it.stopShimmer() }
        shimmerDrawables.clear()
        dismissLoadingDialog()
        reusableAppOpenAd = null // Clear reference
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

    fun reloadAppOpenAdForBackground(context: Context) {
        Log.d(TAG, "Force reloading App Open Ad for background-to-foreground")

        // Clear existing ad
        reusableAppOpenAd?.destroy()
        reusableAppOpenAd = null

        // Preload new ad in background
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                reusableAppOpenAd = AdsManager.getAppOpenAd(context)
                Log.d(TAG, "App Open Ad preloaded for next foreground")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload App Open Ad", e)
            }
        }, 2000) // 2 second delay
    }
}