package com.example.navarsgpsdemo

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread as thread1

class MainActivity : AppCompatActivity() {

    private lateinit var clockTextView: TextView
    private lateinit var startButton: Button
    private lateinit var handler: Handler
    private var isRunning = true
    private var timeFormat = SimpleDateFormat("HH:mm:ss.S", Locale.US)
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockTextView = findViewById(R.id.tvClock)
        startButton = findViewById(R.id.btnStart)
        handler = Handler(Looper.getMainLooper())

        // Check and request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }

        // Get the CameraManager and the cameraId
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager?.cameraIdList?.get(0) // Assuming the first camera has the flashlight

        fetchAndUpdateTime()

        startButton.setOnClickListener {
            if (isRunning) {
                isRunning = false
                stopClockAndTurnOnFlashlight()
            } else {
                isRunning = true
                resetClockAndTurnOffFlashlight()
            }
        }
    }

    private fun fetchAndUpdateTime() {
        thread1 {
            while (isRunning) {
                try {
                    // Fetch time from a reliable internet time source
                    val timeUrl = URL("http://worldtimeapi.org/api/timezone/Etc/UTC")
                    val response = timeUrl.readText()
                    val currentTime = parseTimeFromResponse(response)

                    // Update the clockTextView
                    runOnUiThread {
                        clockTextView.text = timeFormat.format(currentTime)
                    }

                    Thread.sleep(100)
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        clockTextView.text = "Error fetching time"
                    }
                }
            }
        }
    }

    private fun parseTimeFromResponse(response: String): Date {
        val dateTimeStr = "\"datetime\":\"([^\"]+)\"".toRegex().find(response)?.groups?.get(1)?.value
        return dateTimeStr?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).parse(it)
        } ?: Date()
    }

    @SuppressLint("MissingPermission")
    private fun stopClockAndTurnOnFlashlight() {
        startButton.text = "Reset"
        // Turn on the flashlight
        try {
            cameraManager?.setTorchMode(cameraId!!, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun resetClockAndTurnOffFlashlight() {
        startButton.text = "Start"
        fetchAndUpdateTime()
        // Turn off the flashlight
        try {
            cameraManager?.setTorchMode(cameraId!!, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                runOnUiThread {
                    clockTextView.text = "Camera permission required for flashlight"
                    startButton.isEnabled = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
