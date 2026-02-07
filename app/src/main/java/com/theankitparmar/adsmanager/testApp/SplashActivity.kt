package com.theankitparmar.adsmanager.testApp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Wait for 5 seconds (5000 milliseconds) before moving to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, TestAppOpenActivity::class.java)
            startActivity(intent)
            finish()
        }, 5000)
    }
}
