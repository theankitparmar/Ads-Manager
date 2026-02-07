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
    private const val COOLDOWN_PERIOD = 0L // Remove cooldown completely
    private const val MIN_BACKGROUND_TIME = 2000L // App must be in background for at least 2 seconds

    private lateinit var application: Application
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private var isAppInForeground = false
    private var wasAppInBackground = false // TRACK if app was truly in background
    private var backgroundTime: Long = 0
    private var lastAdShownTime: Long = 0
    private var isFirstLaunch = true // NEW: Track first launch
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
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

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
     * Check if an activity is excluded
     */
    fun isActivityExcluded(activityClass: Class<*>): Boolean {
        return excludedActivities.contains(activityClass.name)
    }

    /**
     * Called when app goes to background
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        if (debugMode) Log.d(TAG, "App went to background")
        wasAppInBackground = true
        backgroundTime = System.currentTimeMillis()

        // Clear any pending ad show calls
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Called when app comes to foreground
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        if (debugMode) Log.d(TAG, "App came to foreground")

        // REMOVED: Don't skip on first launch for testing
        // if (isFirstLaunch) {
        //     if (debugMode) Log.d(TAG, "First launch - skipping App Open Ad")
        //     isFirstLaunch = false
        //     return
        // }

        // Only show ad if app was in background for sufficient time
        if (wasAppInBackground) {
            val timeInBackground = System.currentTimeMillis() - backgroundTime

            if (timeInBackground >= MIN_BACKGROUND_TIME) {
                if (debugMode) Log.d(TAG, "App was in background for ${timeInBackground}ms - showing ad")

                // Wait a bit before showing ad to ensure activity is ready
                handler.postDelayed({
                    showAdIfAvailable()
                }, 500)
            } else {
                if (debugMode) Log.d(TAG, "App was in background for only ${timeInBackground}ms - not showing ad")
            }

            wasAppInBackground = false
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

        // Check if current activity is excluded
        val currentActivityName = currentActivity?.javaClass?.name
        if (currentActivityName != null && excludedActivities.contains(currentActivityName)) {
            if (debugMode) Log.d(TAG, "Current activity is excluded: $currentActivityName")
            return false
        }

        // REMOVED: Cooldown check
        // No timer logic - always try to show if other conditions are met

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

                AdHelper.showAppOpenAd(
                    activity = activity,
                    showLoadingDialog = false,
                    onAdDismissed = {
                        isShowingAd = false
                        if (debugMode) Log.d(TAG, "App Open Ad dismissed")
                    },
                    onAdFailed = { error ->
                        isShowingAd = false
                        if (debugMode) Log.e(TAG, "App Open Ad failed: $error")
                    }
                )

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
     * Reset the first launch flag (useful for testing)
     */
    fun resetFirstLaunch() {
        isFirstLaunch = true
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
        isAppInForeground = true
        if (debugMode) Log.d(TAG, "Activity started: ${activity.javaClass.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        // Not showing ad here to avoid showing when switching between activities
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {

    }

    override fun onActivityPaused(activity: Activity) {
        // Not used
    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}