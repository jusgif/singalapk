package com.signalmeter

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.telephony.*
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executor

class SignalMonitorService : Service() {
    private lateinit var tm: TelephonyManager
    private lateinit var executor: Executor
    private val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) { update(signalStrength) }
    }
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("signal", "Signal Meter", NotificationManager.IMPORTANCE_LOW)
        )
        tm = getSystemService(TelephonyManager::class.java)
        executor = mainExecutor
        startForeground(1001, notification("Waiting for signal…"))
        tm.registerTelephonyCallback(executor, callback)
    }
    private fun update(s: SignalStrength) {
        var value: Int? = null
        var tech = "CELL"
        for (cs in s.cellSignalStrengths) {
            if (cs is CellSignalStrengthNr && cs.ssRsrp != CellSignalStrengthNr.UNAVAILABLE) {
                value = cs.ssRsrp; tech = "5G"; break
            }
            if (cs is CellSignalStrengthLte && cs.rsrp != CellSignalStrengthLte.UNAVAILABLE) {
                value = cs.rsrp; tech = "LTE"
            }
        }
        val text = if (value != null) "$tech  $value dBm" else "Signal unavailable"
        getSystemService(NotificationManager::class.java).notify(1001, notification(text))
    }
    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, "signal")
            .setSmallIcon(R.drawable.ic_signal).setContentTitle("SignalMeter")
            .setContentText(text).setOngoing(true).setOnlyAlertOnce(true).build()
    override fun onDestroy() { tm.unregisterTelephonyCallback(callback); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
