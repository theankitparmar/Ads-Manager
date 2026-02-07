package com.theankitparmar.adsmanager.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.theankitparmar.adsmanager.core.AdsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager for App Open Ads that shows ads only when app returns from background
 */
object AppOpenManager : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private const val TAG = "AppOpenManager"
    private const val COOLDOWN_PERIOD = 30000L // 30 seconds cooldown between ads
    private const val BACKGROUND_DELAY = 2000L // 2 seconds after background before showing ad

    private lateinit var application: Application
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private var isAppInBackground = true // Start as background until first activity starts
    private var lastAdShownTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    // Activities that should NOT show app open ads
    private val excludedActivities = mutableSetOf<String>()

    // Configurable settings
    private var enabled = true
    private var debugMode = false

    /**
     * Initialize the App Open Manager
     */
    fun initialize(application: Application) {
        if (this::application.isInitialized) {
            Log.d(TAG, "AppOpenManager already initialized")
            return
        }

        this.application = application

        // Register for activity lifecycle callbacks
        application.registerActivityLifecycleCallbacks(this)

        // Register for app process lifecycle (foreground/background)
        ProcessLifecycleOwner.Companion.get().lifecycle.addObserver(this)

        Log.d(TAG, "AppOpenManager initialized")
    }

    /**
     * Enable or disable App Open Ads
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        Log.d(TAG, "AppOpenAds ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Set debug mode for logging
     */
    fun setDebugMode(debug: Boolean) {
        this.debugMode = debug
    }

    /**
     * Exclude an activity from showing App Open Ads
     */
    fun excludeActivity(activityClass: Class<*>) {
        excludedActivities.add(activityClass.name)
        Log.d(TAG, "Excluded activity: ${activityClass.simpleName}")
    }

    /**
     * Include an activity back for App Open Ads
     */
    fun includeActivity(activityClass: Class<*>) {
        excludedActivities.remove(activityClass.name)
        Log.d(TAG, "Included activity: ${activityClass.simpleName}")
    }

    /**
     * Check if current activity is excluded
     */
    private fun isCurrentActivityExcluded(): Boolean {
        val currentActivityName = currentActivity?.javaClass?.name ?: return false
        return excludedActivities.contains(currentActivityName)
    }

    /**
     * Called when app goes to background
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        if (debugMode) Log.d(TAG, "App went to background")
        isAppInBackground = true

        // Schedule ad check after delay when app returns to foreground
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Called when app comes to foreground
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        if (debugMode) Log.d(TAG, "App came to foreground")

        // If app was in background and now coming to foreground
        if (isAppInBackground) {
            isAppInBackground = false

            // Wait a bit before showing ad to ensure activity is ready
            handler.postDelayed({
                showAdIfAvailable()
            }, BACKGROUND_DELAY)
        }
    }

    /**
     * Check if ad can be shown (cooldown and conditions)
     */
    private fun canShowAd(): Boolean {
        if (!enabled) {
            if (debugMode) Log.d(TAG, "AppOpenAds disabled")
            return false
        }

        if (isShowingAd) {
            if (debugMode) Log.d(TAG, "Already showing an ad")
            return false
        }

        if (currentActivity == null || currentActivity?.isFinishing == true) {
            if (debugMode) Log.d(TAG, "No valid activity")
            return false
        }

        if (isCurrentActivityExcluded()) {
            if (debugMode) Log.d(TAG, "Current activity is excluded")
            return false
        }

        // Check cooldown period
        val timeSinceLastAd = System.currentTimeMillis() - lastAdShownTime
        if (timeSinceLastAd < COOLDOWN_PERIOD) {
            if (debugMode) Log.d(TAG, "In cooldown period: ${COOLDOWN_PERIOD - timeSinceLastAd}ms remaining")
            return false
        }

        // Check if AdsManager is initialized
        if (!AdsManager.isInitialized()) {
            if (debugMode) Log.d(TAG, "AdsManager not initialized")
            return false
        }

        return true
    }

    /**
     * Show app open ad if available and conditions are met
     */
    private fun showAdIfAvailable() {
        if (!canShowAd()) {
            return
        }

        val activity = currentActivity ?: return

        if (debugMode) Log.d(TAG, "Attempting to show App Open Ad")

        isShowingAd = true

        // Use AdHelper to show the ad
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Wait for AdsManager initialization if needed
                if (!AdsManager.isInitialized()) {
                    AdsManager.awaitInitialization()
                }

                val wasShown = AdHelper.showAppOpenAd(
                    activity = activity,
                    showLoadingDialog = false,
                    onAdDismissed = {
                        isShowingAd = false
                        lastAdShownTime = System.currentTimeMillis()
                        if (debugMode) Log.d(TAG, "App Open Ad dismissed")

                        // Preload next ad
                        handler.postDelayed({
                            preloadNextAd()
                        }, 1000)
                    },
                    onAdFailed = { error ->
                        isShowingAd = false
                        Log.e(TAG, "App Open Ad failed: $error")

                        // Retry after delay
                        handler.postDelayed({
                            preloadNextAd()
                        }, 30000)
                    }
                )

                if (!wasShown) {
                    isShowingAd = false
                    if (debugMode) Log.d(TAG, "App Open Ad not ready, will try later")

                    // Try again after delay
                    handler.postDelayed({
                        showAdIfAvailable()
                    }, 5000)
                } else {
                    lastAdShownTime = System.currentTimeMillis()
                }

            } catch (e: Exception) {
                isShowingAd = false
                Log.e(TAG, "Error showing App Open Ad", e)
            }
        }
    }

    /**
     * Preload the next app open ad
     */
    fun preloadNextAd() {
        if (!AdsManager.isInitialized()) return

        try {
            AdsManager.getAppOpenAd(application)
            if (debugMode) Log.d(TAG, "App Open Ad preloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload App Open Ad", e)
        }
    }

    /**
     * Force show an App Open Ad (for testing or special cases)
     */
    fun showAd() {
        if (canShowAd()) {
            showAdIfAvailable()
        }
    }

    /**
     * Clear all resources
     */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        application.unregisterActivityLifecycleCallbacks(this)
        currentActivity = null
        Log.d(TAG, "AppOpenManager destroyed")
    }

    // Activity Lifecycle Callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Not used
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (debugMode) Log.d(TAG, "Activity started: ${activity.javaClass.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        // Not showing ad here to avoid showing when switching between activities
    }

    override fun onActivityPaused(activity: Activity) {
        // Not used
    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not used
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}