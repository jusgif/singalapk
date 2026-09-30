package com.signalmeter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE, Manifest.permission.POST_NOTIFICATIONS)
    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var running by remember { mutableStateOf(false) }
                Surface {
                    Column {
                        Text("SignalMeter", style = MaterialTheme.typography.headlineMedium)
                        Text("Shows LTE/5G RSRP in an ongoing status notification.")
                        Button(onClick = {
                            if (!permissions.all { ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED }) requestPermissions.launch(permissions)
                            ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, SignalMonitorService::class.java))
                            running = true
                        }) { Text(if (running) "Monitoring" else "Start monitoring") }
                        Button(onClick = {
                            stopService(Intent(this@MainActivity, SignalMonitorService::class.java)); running = false
                        }) { Text("Stop monitoring") }
                    }
                }
            }
        }
    }
}
