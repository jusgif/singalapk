package com.signalmeter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private var startAfterPermissionRequest = false

    private val permissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "SignalMeter"
            textSize = 28f
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "Shows LTE/5G RSRP in an ongoing status notification."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        val startButton = Button(this).apply {
            text = "Start monitoring"
            setOnClickListener {
                if (hasAllPermissions()) {
                    startMonitoring()
                } else {
                    startAfterPermissionRequest = true
                    requestPermissions(permissions, permissionRequestCode)
                }
            }
        }

        val stopButton = Button(this).apply {
            text = "Stop monitoring"
            setOnClickListener {
                stopService(Intent(this@MainActivity, SignalMonitorService::class.java))
            }
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(startButton)
        layout.addView(stopButton)
        setContentView(layout)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        requestedPermissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, requestedPermissions, grantResults)

        if (requestCode == permissionRequestCode && startAfterPermissionRequest && hasAllPermissions()) {
            startMonitoring()
        }
        startAfterPermissionRequest = false
    }

    private fun hasAllPermissions(): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun startMonitoring() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, SignalMonitorService::class.java)
        )
    }
}
