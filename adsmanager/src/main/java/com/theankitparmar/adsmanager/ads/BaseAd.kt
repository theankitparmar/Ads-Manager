package com.theankitparmar.adsmanager.ads

import android.app.Activity
import android.content.Context
import com.theankitparmar.adsmanager.adInterface.AdManager
import com.theankitparmar.adsmanager.adInterface.AdsConfiguration
import com.theankitparmar.adsmanager.callbacks.AdListener
import com.theankitparmar.adsmanager.callbacks.AdResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import kotlin.math.pow

abstract class BaseAd<State : AdState>(
    context: Context,
    private val adUnitId: String,
    protected val config: AdsConfiguration
) : AdManager {

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    protected val weakContext = WeakReference(context.applicationContext)
    private var currentRetryCount = 0

    protected val _state = MutableStateFlow<AdState>(AdState.Idle)
    val state: StateFlow<AdState> = _state

    protected var _listener: AdListener? = null

    override fun setListener(listener: AdListener?) {
        _listener = listener
    }

    protected abstract suspend fun loadAdInternal(): AdResult<Unit>


    override fun loadAd() {
        if (_state.value is AdState.Loading) return

        scope.launch {
            _state.value = AdState.Loading
            _listener?.onAdLoaded() // Notify loading started

            val result = loadAdInternal()

            when (result) {
                is AdResult.Success -> {
                    _state.value = AdState.Loaded
                    currentRetryCount = 0
                    _listener?.onAdLoaded()
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

    private fun handleRetry(error: String) {
        if (currentRetryCount < config.retryPolicy.maxRetries) {
            val delay = (config.retryPolicy.initialDelay *
                    config.retryPolicy.multiplier.pow(currentRetryCount)).toLong()

            scope.launch {
                delay(delay)
                currentRetryCount++
                loadAd()
            }
        } else {
            _state.value = AdState.Error(error)
            _listener?.onAdFailedToLoad(error)
        }
    }

    override fun isLoaded(): Boolean = _state.value is AdState.Loaded

    override fun destroy() {
        scope.cancel()
        _listener = null
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

    // Helper method to show ad with Activity check
    protected fun showAdWithActivityCheck(showAdAction: (Activity) -> Unit) {
        val context = getContext()
        if (context is Activity && !context.isFinishing) {
            showAdAction(context)
        } else {
            _listener?.onAdFailedToShow("Activity is not valid or is finishing")
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