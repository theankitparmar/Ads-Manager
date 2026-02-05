package com.theankitparmar.adsmanager.adInterface

data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelay: Long = 1000,
    val multiplier: Float = 2f
) {
    companion object {
        val default = RetryPolicy()
    }
}