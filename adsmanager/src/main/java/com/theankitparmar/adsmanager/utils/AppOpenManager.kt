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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for App Open Ads that shows ads only when app returns from background
 */
object AppOpenManager : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private const val TAG = "AppOpenManager"
    private const val MIN_BACKGROUND_TIME = 2000L // App must be in background for at least 2 seconds
    private const val AD_SHOW_DELAY = 500L // Delay to ensure activity is ready

    private lateinit var application: Application
    private var currentActivity: Activity? = null
    private var isShowingAd = AtomicBoolean(false)
    private var wasAppInBackground = false // Start as false - app wasn't in background on first launch
    private var backgroundTime: Long = 0
    private var lastAdShownTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    // Activities that should NOT show app open ads
    private val excludedActivities = mutableSetOf<String>()

    // Configurable settings
    private var enabled = true
    private var debugMode = false
    private var showOnFirstLaunch = false // Don't show on first app launch by default
    private var cooldownPeriod = 30000L // 30 seconds cooldown by default
    private var minBackgroundTime = MIN_BACKGROUND_TIME // Configurable minimum background time
    private var appStartTime: Long = 0 // Track when app started

    /**
     * Initialize the App Open Manager
     */
    fun initialize(application: Application) {
        if (this::application.isInitialized) {
            Log.d(TAG, "AppOpenManager already initialized")
            return
        }

        this.application = application
        appStartTime = System.currentTimeMillis()

        // Register for activity lifecycle callbacks
        application.registerActivityLifecycleCallbacks(this)

        // Register for app process lifecycle (foreground/background)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Preload first ad
        handler.postDelayed({
            if (AdsManager.isInitialized()) {
                preloadNextAd()
            }
        }, 3000) // Wait 3 seconds to ensure AdsManager is initialized

        Log.d(TAG, "AppOpenManager initialized. Show on first launch: $showOnFirstLaunch")
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

        // Preload next ad while in background for faster showing next time
        handler.postDelayed({
            if (AdsManager.isInitialized()) {
                preloadNextAd()
            }
        }, 1000)
    }

    /**
     * Called when app comes to foreground
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        if (debugMode) Log.d(TAG, "App came to foreground")

        // Check if app was in background
        if (wasAppInBackground) {
            val timeInBackground = System.currentTimeMillis() - backgroundTime
            val timeSinceAppStart = System.currentTimeMillis() - appStartTime

            // CRITICAL: Skip if this is the first launch (app started less than 5 seconds ago)
            if (timeSinceAppStart < 5000 && !showOnFirstLaunch) {
                if (debugMode) Log.d(TAG, "First launch detected (${timeSinceAppStart}ms since start) - skipping ad")
                wasAppInBackground = false
                return
            }

            if (timeInBackground >= minBackgroundTime) {
                if (debugMode) Log.d(TAG, "App was in background for ${timeInBackground}ms - showing ad")

                // Wait a bit before showing ad to ensure activity is ready
                handler.postDelayed({
                    showAdIfAvailable()
                }, AD_SHOW_DELAY)
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

        if (isShowingAd.get()) {
            if (debugMode) Log.d(TAG, "Already showing an ad")
            return false
        }

        val activity = currentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            if (debugMode) Log.d(TAG, "No valid activity")
            return false
        }

        // Check if current activity is excluded
        val currentActivityName = activity.javaClass.name
        if (excludedActivities.contains(currentActivityName)) {
            if (debugMode) Log.d(TAG, "Current activity is excluded: $currentActivityName")
            return false
        }

        // Check cooldown period
        val timeSinceLastAd = System.currentTimeMillis() - lastAdShownTime
        if (timeSinceLastAd < cooldownPeriod) {
            if (debugMode) Log.d(TAG, "Cooldown active: ${cooldownPeriod - timeSinceLastAd}ms remaining")
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
    fun showAdIfAvailable() {
        if (!canShowAd()) {
            return
        }

        val activity = currentActivity ?: return

        if (debugMode) Log.d(TAG, "Attempting to show App Open Ad")

        if (!isShowingAd.compareAndSet(false, true)) {
            if (debugMode) Log.d(TAG, "Another ad is already being shown")
            return
        }

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
                        isShowingAd.set(false)
                        lastAdShownTime = System.currentTimeMillis()
                        if (debugMode) Log.d(TAG, "App Open Ad dismissed")

                        // Preload next ad after dismissal
                        handler.postDelayed({
                            preloadNextAd()
                        }, 1000)
                    },
                    onAdFailed = { error ->
                        isShowingAd.set(false)
                        if (debugMode) Log.e(TAG, "App Open Ad failed: $error")

                        // Try to preload another ad after failure
                        handler.postDelayed({
                            preloadNextAd()
                        }, 2000)
                    }
                )

            } catch (e: Exception) {
                isShowingAd.set(false)
                Log.e(TAG, "Error showing App Open Ad", e)
            }
        }
    }

    /**
     * Preload the next app open ad
     */
    fun preloadNextAd() {
        if (!AdsManager.isInitialized()) {
            if (debugMode) Log.d(TAG, "AdsManager not initialized, can't preload ad")
            return
        }

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
     * Set minimum background time before showing ad
     */
    fun setMinBackgroundTime(minTime: Long) {
        minBackgroundTime = minTime
        if (debugMode) Log.d(TAG, "Minimum background time set to ${minTime}ms")
    }

    /**
     * Set whether to show ad on first launch
     */
    fun setShowOnFirstLaunch(show: Boolean) {
        showOnFirstLaunch = show
        if (debugMode) Log.d(TAG, "Show on first launch: $show")
    }

    /**
     * Set cooldown period between ads
     */
    fun setCooldownPeriod(cooldown: Long) {
        cooldownPeriod = cooldown
        if (debugMode) Log.d(TAG, "Cooldown period set to ${cooldown}ms")
    }

    /**
     * Check if ad can be shown for current activity
     */
    fun canShowAdForCurrentActivity(): Boolean {
        return canShowAd()
    }

    /**
     * Clear all resources
     */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        if (this::application.isInitialized) {
            application.unregisterActivityLifecycleCallbacks(this)
        }
        currentActivity = null
        isShowingAd.set(false)
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

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not used
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