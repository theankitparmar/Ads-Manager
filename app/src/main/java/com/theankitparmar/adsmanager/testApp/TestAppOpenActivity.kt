package com.theankitparmar.adsmanager.testApp

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.theankitparmar.adsmanager.utils.AppOpenManager
import java.text.SimpleDateFormat
import java.util.*

class TestAppOpenActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_app_open)

        tvStatus = findViewById(R.id.tv_status)
        updateStatus("Activity created at ${getCurrentTime()}")

        // This will show App Open Ads when returning from background
        // (but NOT on first launch)

        setupControls()
    }

    private fun setupControls() {
        val btnBackground = findViewById<Button>(R.id.btn_go_background)
        val btnTest = findViewById<Button>(R.id.btn_test)

        btnBackground.setOnClickListener {
            updateStatus("Going to background at ${getCurrentTime()}")
            Log.d("Test", "Simulating background...")

            // Simulate going to background
            moveTaskToBack(true)

            // Wait 3 seconds then bring back to foreground
            Handler().postDelayed({
                updateStatus("Should show App Open Ad now (if conditions met)")
            }, 3000)
        }

        btnTest.setOnClickListener {
            // Manual test
            updateStatus("Manual test at ${getCurrentTime()}")
            AppOpenManager.showAd()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus("Activity resumed at ${getCurrentTime()}")
        Log.d("Test", "Activity resumed")
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            tvStatus.text = message
        }
    }

    private fun getCurrentTime(): String {
        return dateFormat.format(Date())
    }
}