package com.signalmeter

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.IBinder
import android.telephony.CellInfo
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class SignalMonitorService : Service() {
    private lateinit var telephonyManager: TelephonyManager

    private val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) = update(signalStrength)
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Signal Meter", NotificationManager.IMPORTANCE_LOW)
        )
        telephonyManager = getSystemService(TelephonyManager::class.java)
        startForeground(NOTIFICATION_ID, notification("Waiting for signal…", Color.GRAY))
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", true).apply()
        broadcastUpdate()
        try {
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
        } catch (_: SecurityException) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
            stopSelf()
        }
    }

    private fun update(signalStrength: SignalStrength) {
        var rsrp = Int.MIN_VALUE
        var rsrq = Int.MIN_VALUE
        var sinr = Int.MIN_VALUE
        var technology = "CELL"
        // Prefer a valid NR reading when present; otherwise use a valid LTE reading.
        for (cell in signalStrength.cellSignalStrengths) {
            if (cell is CellSignalStrengthNr && valid(cell.ssRsrp)) {
                rsrp = cell.ssRsrp
                rsrq = cell.ssRsrq.takeIf(::valid) ?: Int.MIN_VALUE
                sinr = cell.ssSinr.takeIf(::valid) ?: Int.MIN_VALUE
                technology = "5G NR"
                break
            }
        }
        if (rsrp == Int.MIN_VALUE) {
            for (cell in signalStrength.cellSignalStrengths) {
                if (cell is CellSignalStrengthLte && valid(cell.rsrp)) {
                    rsrp = cell.rsrp
                    rsrq = cell.rsrq.takeIf(::valid) ?: Int.MIN_VALUE
                    sinr = cell.rssnr.takeIf(::valid) ?: Int.MIN_VALUE
                    technology = "LTE"
                    break
                }
            }
        }

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val oldHistory = prefs.getString("history", "").orEmpty()
        val history = if (rsrp != Int.MIN_VALUE) (oldHistory.split(",").mapNotNull { it.toIntOrNull() } + rsrp).takeLast(60).joinToString(",") else oldHistory
        prefs.edit()
            .putInt("rsrp", rsrp)
            .putInt("rsrq", rsrq)
            .putInt("sinr", sinr)
            .putString("technology", technology.takeIf { rsrp != Int.MIN_VALUE } ?: "Signal unavailable")
            .putString("history", history)
            .putBoolean("running", true)
            .apply()

        val text = if (rsrp != Int.MIN_VALUE) "$technology  $rsrp dBm" else "Signal unavailable"
        val tint = if (rsrp != Int.MIN_VALUE) colorForRsrp(rsrp) else Color.GRAY
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text, tint))
        broadcastUpdate()
    }

    private fun valid(value: Int): Boolean =
        value != CellInfo.UNAVAILABLE && value != Int.MAX_VALUE && value in -200..200

    private fun colorForRsrp(rsrp: Int): Int = when {
        rsrp >= -85 -> Color.rgb(46, 204, 113)
        rsrp >= -100 -> Color.rgb(139, 195, 74)
        rsrp >= -110 -> Color.rgb(255, 193, 7)
        else -> Color.rgb(244, 67, 54)
    }

    private fun notification(text: String, tint: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_signal)
            .setColor(tint)
            .setContentTitle("SignalMeter")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun broadcastUpdate() {
        sendBroadcast(Intent(ACTION_SIGNAL_UPDATE).setPackage(packageName))
    }

    override fun onDestroy() {
        try { telephonyManager.unregisterTelephonyCallback(callback) } catch (_: Exception) {}
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).apply()
        broadcastUpdate()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SIGNAL_UPDATE = "com.signalmeter.SIGNAL_UPDATE"
        const val PREFS = "signalmeter_state"
        private const val CHANNEL_ID = "signal"
        private const val NOTIFICATION_ID = 1001
    }
}
