package com.theankitparmar.adsmanager.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AppOpenLifecycleCallback(
    private val onShowAppOpenAd: () -> Unit
) : Application.ActivityLifecycleCallbacks {
    
    private var currentActivity: Activity? = null
    private var isAppInForeground = false
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }
    
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (!isAppInForeground) {
            isAppInForeground = true
            onShowAppOpenAd.invoke()
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            isAppInForeground = false
            currentActivity = null
        }
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}