package com.signalmeter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private var startAfterPermissionRequest = false

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = permissions.all { permission ->
                result[permission] == true ||
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
            if (granted && startAfterPermissionRequest) {
                startMonitoring()
            }
            startAfterPermissionRequest = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var running by remember { mutableStateOf(false) }
                Surface {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("SignalMeter", style = MaterialTheme.typography.headlineMedium)
                        Text("Shows LTE/5G RSRP in an ongoing status notification.")
                        Button(onClick = {
                            if (hasAllPermissions()) {
                                startMonitoring()
                                running = true
                            } else {
                                startAfterPermissionRequest = true
                                requestPermissions.launch(permissions)
                            }
                        }) {
                            Text(if (running) "Monitoring" else "Start monitoring")
                        }
                        Button(onClick = {
                            stopService(Intent(this@MainActivity, SignalMonitorService::class.java))
                            running = false
                        }) {
                            Text("Stop monitoring")
                        }
                    }
                }
            }
        }
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
