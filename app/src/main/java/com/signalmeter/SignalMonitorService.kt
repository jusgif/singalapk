package com.signalmeter

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.telephony.CellInfo
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class SignalMonitorService : Service() {
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var executor: Executor

    private val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            update(signalStrength)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Signal Meter",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        telephonyManager = getSystemService(TelephonyManager::class.java)
        executor = mainExecutor

        startForeground(NOTIFICATION_ID, notification("Waiting for signal…"))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        try {
            telephonyManager.registerTelephonyCallback(executor, callback)
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun update(signalStrength: SignalStrength) {
        var value: Int? = null
        var technology = "CELL"

        for (cellSignalStrength in signalStrength.cellSignalStrengths) {
            when (cellSignalStrength) {
                is CellSignalStrengthNr -> {
                    if (isValidSignal(cellSignalStrength.ssRsrp)) {
                        value = cellSignalStrength.ssRsrp
                        technology = "5G"
                        break
                    }
                }
                is CellSignalStrengthLte -> {
                    if (isValidSignal(cellSignalStrength.rsrp)) {
                        value = cellSignalStrength.rsrp
                        technology = "LTE"
                    }
                }
            }
        }

        val text = if (value != null) "$technology  $value dBm" else "Signal unavailable"
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text))
    }

    private fun isValidSignal(signal: Int): Boolean {
        return signal != CellInfo.UNAVAILABLE && signal != Int.MAX_VALUE && signal in -200..0
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_signal)
            .setContentTitle("SignalMeter")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    override fun onDestroy() {
        try {
            telephonyManager.unregisterTelephonyCallback(callback)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "signal"
        private const val NOTIFICATION_ID = 1001
    }
}
