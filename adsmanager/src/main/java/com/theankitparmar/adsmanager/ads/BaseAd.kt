package com.theankitparmar.adsmanager.ads

import android.app.Activity
import android.content.Context
import com.theankitparmar.adsmanager.adInterface.*
import com.theankitparmar.adsmanager.callbacks.AdListener
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

/**
 * Base class for all ad types
 * Provides common functionality for loading, showing, and managing ads
 * 
 * Features:
 * - Centralized ad loading with timeout and retry logic
 * - Event emission for ad lifecycle
 * - Weak reference context to prevent memory leaks
 * - Coroutine-based async ad operations
 * - Mutex-protected concurrent load protection
 * 
 * @param context The Android application context
 * @param adUnitId The AdMob ad unit ID
 * @param config Ad configuration with retry policies
 * @param adType The type of ad (BANNER, INTERSTITIAL, NATIVE, APP_OPEN)
 * @param adId Unique identifier for this ad instance
 */
abstract class BaseAd<State : AdState>(
    context: Context,
    private val adUnitId: String,
    protected val config: AdsConfiguration,
    private val adType: AdType,
    private val adId: String = generateAdId(adType)
) : AdManager {

    protected open val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate +
                CoroutineName("AdManager-$adType-$adId")
    )

    protected val weakContext = WeakReference(context.applicationContext)
    private var currentRetryCount = AtomicInteger(0)
    private val loadMutex = Mutex()
    private var loadJob: Job? = null

    protected val _state = MutableStateFlow<AdState>(AdState.Idle)
    val state: StateFlow<AdState> = _state

    protected val _events = Channel<AdEvent>(Channel.BUFFERED)
    override fun getEvents(): Flow<AdEvent> = _events.receiveAsFlow()

    override fun clearEvents() {
        scope.launch {
            _events.cancel()
        }
    }

    protected var _listener: AdListener? = null

    override fun getAdId(): String = adId
    override fun getAdType(): AdType = adType

    override fun setListener(listener: AdListener?) {
        _listener = listener
    }

    protected abstract suspend fun loadAdInternal(): AdResult<Unit>

    /**
     * Load the ad asynchronously with retry logic
     * 
     * This method:
     * - Prevents concurrent loads with mutex
     * - Applies timeout to prevent hanging
     * - Automatically retries on failure with exponential backoff
     * - Emits events for each state change
     */
    override fun loadAd() {
        if (_state.value is AdState.Loading) {
            android.util.Log.w("BaseAd", "Ad $adId already loading, skipping duplicate request")
            emitEvent(AdEvent.Failed(adType, CustomAdError.InternalError, adId))
            return
        }

        loadJob?.cancel()
        loadJob = scope.launch {
            loadMutex.withLock {
                _state.value = AdState.Loading
                _listener?.onAdLoading()
                android.util.Log.d("BaseAd", "Loading ad $adId (attempt ${currentRetryCount.get() + 1})")

                val result = withTimeoutOrNull(config.loadTimeoutMillis) {
                    loadAdInternal()
                } ?: AdResult.Error("Ad load timeout after ${config.loadTimeoutMillis}ms")

                when (result) {
                    is AdResult.Success -> {
                        _state.value = AdState.Loaded
                        currentRetryCount.set(0)
                        _listener?.onAdLoaded()
                        emitEvent(AdEvent.Loaded(adType, adId))
                        android.util.Log.d("BaseAd", "✓ Ad $adId loaded successfully")
                    }
                    is AdResult.Error -> {
                        _state.value = AdState.Error(result.message)
                        android.util.Log.e("BaseAd", "✗ Ad $adId failed: ${result.message}")
                        handleRetry(result.message)
                    }
                    AdResult.Loading -> {
                        // Already loading
                    }
                }
            }
        }
    }

    /**
     * Handle retry logic with exponential backoff
     * 
     * @param error The error message from the failed load
     */
    private fun handleRetry(error: String) {
        if (currentRetryCount.get() < config.retryPolicy.maxRetries) {
            val delay = (config.retryPolicy.initialDelay *
                    config.retryPolicy.multiplier.pow(currentRetryCount.get())).toLong()

            android.util.Log.d("BaseAd", "Retrying ad $adId in ${delay}ms (attempt ${currentRetryCount.get() + 1}/${config.retryPolicy.maxRetries})")

            scope.launch {
                delay(delay)
                currentRetryCount.incrementAndGet()
                loadAd()
            }
        } else {
            _state.value = AdState.Error(error)
            _listener?.onAdFailedToLoad(error)
            emitEvent(AdEvent.Failed(
                adType,
                CustomAdError.LoadError(currentRetryCount.get(), error),
                adId
            ))
            android.util.Log.e("BaseAd", "✗ Ad $adId exhausted all retries (${currentRetryCount.get()})")
        }
    }

    override fun isLoaded(): Boolean = _state.value is AdState.Loaded

    override fun destroy() {
        android.util.Log.d("BaseAd", "Destroying ad $adId")
        loadJob?.cancel()
        scope.cancel("Ad destroyed")
        _listener = null
        _events.close()
    }

    protected fun getContext(): Context? = weakContext.get()

    /**
     * Get the ad unit ID, using test ID if in test mode
     */
    protected fun getAdUnitId(): String {
        return if (config.isTestMode) {
            getTestAdUnitId()
        } else {
            adUnitId
        }
    }

    protected abstract fun getTestAdUnitId(): String

    /**
     * Show ad with activity validity check
     * 
     * @param showAdAction Lambda to execute ad show logic
     */
    protected fun showAdWithActivityCheck(showAdAction: (Activity) -> Unit) {
        val context = getContext()
        if (context is Activity && !context.isFinishing) {
            try {
                showAdAction(context)
                emitEvent(AdEvent.Opened(adType, adId))
                android.util.Log.d("BaseAd", "✓ Ad $adId shown successfully")
            } catch (e: Exception) {
                android.util.Log.e("BaseAd", "Error showing ad: ${e.message}", e)
                _listener?.onAdFailedToShow("Error: ${e.message}")
                emitEvent(AdEvent.Failed(
                    adType,
                    CustomAdError.ShowError(1, "Error: ${e.message}"),
                    adId
                ))
            }
        } else {
            val reason = when {
                context == null -> "Context is null"
                context !is Activity -> "Context is not an Activity"
                context.isFinishing -> "Activity is finishing"
                else -> "Unknown"
            }
            android.util.Log.w("BaseAd", "Cannot show ad $adId: $reason")
            _listener?.onAdFailedToShow(reason)
            emitEvent(AdEvent.Failed(
                adType,
                CustomAdError.ShowError(1, reason),
                adId
            ))
        }
    }

    /**
     * Emit an event to all subscribers
     * 
     * @param event The ad event to emit
     */
    protected fun emitEvent(event: AdEvent) {
        scope.launch {
            try {
                _events.send(event)
            } catch (e: Exception) {
                android.util.Log.e("BaseAd", "Error emitting event: ${e.message}")
            }
        }
    }

    companion object {
        private var adCounter = AtomicInteger(0)

        /**
         * Generate unique ad ID
         */
        private fun generateAdId(adType: AdType): String {
            return "${adType.name}_${adCounter.incrementAndGet()}_${System.currentTimeMillis()}"
        }
    }
}

// Simplified Ad State
sealed class AdState {
    object Idle : AdState()
    object Loading : AdState()
    object Loaded : AdState()
    data class Error(val message: String) : AdState()
    object Showing : AdState()
}