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

    override fun loadAd() {
        if (_state.value is AdState.Loading) {
            emitEvent(AdEvent.Failed(adType, CustomAdError.InternalError, adId))
            return
        }

        loadJob?.cancel()
        loadJob = scope.launch {
            loadMutex.withLock {
                _state.value = AdState.Loading
                _listener?.onAdLoading() // Call the new loading callback

                val result = withTimeoutOrNull(config.loadTimeoutMillis) {
                    loadAdInternal()
                } ?: AdResult.Error("Ad load timeout")

                when (result) {
                    is AdResult.Success -> {
                        _state.value = AdState.Loaded
                        currentRetryCount.set(0)
                        _listener?.onAdLoaded()
                        emitEvent(AdEvent.Loaded(adType, adId))
                    }
                    is AdResult.Error -> {
                        _state.value = AdState.Error(result.message)
                        handleRetry(result.message)
                    }
                    AdResult.Loading -> {
                        // Already loading
                    }
                }
            }
        }
    }

    private fun handleRetry(error: String) {
        if (currentRetryCount.get() < config.retryPolicy.maxRetries) {
            val delay = (config.retryPolicy.initialDelay *
                    config.retryPolicy.multiplier.pow(currentRetryCount.get())).toLong()

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
        }
    }

    override fun isLoaded(): Boolean = _state.value is AdState.Loaded

    override fun destroy() {
        loadJob?.cancel()
        scope.cancel("Ad destroyed")
        _listener = null
        _events.close()
    }

    protected fun getContext(): Context? = weakContext.get()

    protected fun getAdUnitId(): String {
        return if (config.isTestMode) {
            getTestAdUnitId()
        } else {
            adUnitId
        }
    }

    protected abstract fun getTestAdUnitId(): String

    protected fun showAdWithActivityCheck(showAdAction: (Activity) -> Unit) {
        val context = getContext()
        if (context is Activity && !context.isFinishing) {
            showAdAction(context)
            emitEvent(AdEvent.Opened(adType, adId))
        } else {
            _listener?.onAdFailedToShow("Activity is not valid or is finishing")
            emitEvent(AdEvent.Failed(
                adType,
                CustomAdError.ShowError(1, "Activity is not valid or is finishing"),
                adId
            ))
        }
    }

    // Remove suspend modifier - make it a regular function
    protected fun emitEvent(event: AdEvent) {
        scope.launch {
            _events.send(event)
        }
    }

    companion object {
        private var adCounter = AtomicInteger(0)

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