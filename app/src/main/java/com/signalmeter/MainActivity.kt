package com.signalmeter

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var root: LinearLayout
    private lateinit var signalValue: TextView
    private lateinit var technology: TextView
    private lateinit var quality: TextView
    private lateinit var rsrqValue: TextView
    private lateinit var sinrValue: TextView
    private lateinit var status: TextView
    private lateinit var history: SignalHistoryView

    private val signalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SignalMonitorService.ACTION_SIGNAL_UPDATE) renderSavedSignal()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = color(BG)
        window.navigationBarColor = color(BG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        val scroll = ScrollView(this).apply { setBackgroundColor(color(BG)) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        val header = row()
        val brand = column()
        brand.addView(label("SIGNALMETER", 22f, TEXT, true))
        brand.addView(label("CELLULAR DIAGNOSTICS", 11f, MUTED, true).apply {
            letterSpacing = .12f
        })
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("LIVE", 11f, GREEN, true).apply {
            setPadding(dp(12), dp(7), dp(12), dp(7))
            setBackgroundColor(color(PANEL_ALT))
        })
        root.addView(header)
        addSpace(24)

        root.addView(label("CURRENT SIGNAL", 12f, MUTED, true))
        addSpace(8)
        val hero = card()
        technology = label("Waiting for network", 14f, GREEN, true)
        hero.addView(technology)
        addSpaceTo(hero, 10)
        val valueRow = row()
        signalValue = label("--", 52f, TEXT, true).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        valueRow.addView(signalValue)
        valueRow.addView(label(" dBm", 16f, MUTED, false).apply {
            gravity = Gravity.BOTTOM
            setPadding(0, 0, 0, dp(9))
        })
        hero.addView(valueRow)
        quality = label("Waiting for signal updates", 14f, MUTED, false)
        hero.addView(quality)
        addSpaceTo(hero, 18)
        history = SignalHistoryView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(112))
        }
        hero.addView(history)
        hero.addView(label("Recent RSRP samples · session only", 11f, MUTED, false))
        root.addView(hero)

        addSpace(16)
        root.addView(label("RADIO METRICS", 12f, MUTED, true))
        addSpace(10)
        val metrics = row()
        val rsrqCard = card()
        rsrqCard.addView(label("RSRQ", 12f, MUTED, true))
        rsrqValue = label("--", 26f, TEXT, true)
        rsrqCard.addView(rsrqValue)
        rsrqCard.addView(label("dB", 11f, MUTED, false))
        val sinrCard = card()
        sinrCard.addView(label("SINR / SS-SINR", 12f, MUTED, true))
        sinrValue = label("--", 26f, TEXT, true)
        sinrCard.addView(sinrValue)
        sinrCard.addView(label("dB", 11f, MUTED, false))
        metrics.addView(rsrqCard, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        metrics.addView(sinrCard, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        root.addView(metrics)

        addSpace(16)
        val info = card()
        info.addView(label("ABOUT THESE READINGS", 12f, GREEN, true))
        addSpaceTo(info, 8)
        info.addView(label("RSRP measures reference-signal power. RSRQ describes reference-signal quality, while SINR compares signal power with interference and noise. Availability depends on your phone, modem, network, and Android version.", 13f, MUTED, false))
        root.addView(info)

        addSpace(18)
        status = label("Monitoring is stopped", 13f, MUTED, false)
        root.addView(status)
        addSpace(10)
        val controls = row()
        val start = Button(this).apply {
            text = "Start monitoring"
            setTextColor(color(BG))
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(GREEN)))
            setOnClickListener {
                if (hasAllPermissions()) startMonitoring() else {
                    startAfterPermissionRequest = true
                    requestPermissions(permissions, permissionRequestCode)
                }
            }
        }
        val stop = Button(this).apply {
            text = "Stop"
            setOnClickListener {
                stopService(Intent(this@MainActivity, SignalMonitorService::class.java))
                status.text = "Monitoring stopped"
            }
        }
        controls.addView(start, LinearLayout.LayoutParams(0, dp(52), 1.5f).apply { marginEnd = dp(8) })
        controls.addView(stop, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(controls)
        addSpace(12)
        root.addView(label("Readings are supplied by Android telephony APIs. Some metrics may be unavailable on certain devices.", 11f, MUTED, false))
        renderSavedSignal()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SignalMonitorService.ACTION_SIGNAL_UPDATE)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(signalReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(signalReceiver, filter)
        }
        renderSavedSignal()
    }

    override fun onStop() {
        try { unregisterReceiver(signalReceiver) } catch (_: IllegalArgumentException) {}
        super.onStop()
    }

    private fun renderSavedSignal() {
        val prefs = getSharedPreferences(SignalMonitorService.PREFS, Context.MODE_PRIVATE)
        val rsrp = prefs.getInt("rsrp", Int.MIN_VALUE)
        val tech = prefs.getString("technology", null)
        val rsrq = prefs.getInt("rsrq", Int.MIN_VALUE)
        val sinr = prefs.getInt("sinr", Int.MIN_VALUE)
        val running = prefs.getBoolean("running", false)
        status.text = if (running) "Monitoring active · updates as Android reports them" else "Monitoring is stopped"
        technology.text = tech ?: "Waiting for network"
        signalValue.text = if (rsrp == Int.MIN_VALUE) "--" else rsrp.toString()
        quality.text = when {
            rsrp == Int.MIN_VALUE -> "Signal reading unavailable"
            rsrp >= -85 -> "Strong reference signal"
            rsrp >= -100 -> "Moderate reference signal"
            rsrp >= -110 -> "Weak reference signal"
            else -> "Very weak reference signal"
        }
        rsrqValue.text = if (rsrq == Int.MIN_VALUE) "--" else rsrq.toString()
        sinrValue.text = if (sinr == Int.MIN_VALUE) "--" else sinr.toString()
        history.setSamples(prefs.getString("history", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() })
    }

    private fun hasAllPermissions(): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, requestedPermissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, requestedPermissions, grantResults)
        if (requestCode == permissionRequestCode && startAfterPermissionRequest && hasAllPermissions()) startMonitoring()
        startAfterPermissionRequest = false
    }

    private fun startMonitoring() {
        ContextCompat.startForegroundService(this, Intent(this, SignalMonitorService::class.java))
        status.text = "Starting monitor…"
    }

    private fun color(hex: String) = android.graphics.Color.parseColor(hex)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun card() = column().apply {
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundColor(color(PANEL))
    }
    private fun label(text: String, size: Float, tint: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color(tint))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = true
    }
    private fun addSpace(height: Int) { root.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun addSpaceTo(parent: LinearLayout, height: Int) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }

    companion object {
        private const val BG = "#0B1218"
        private const val PANEL = "#121F28"
        private const val PANEL_ALT = "#18352E"
        private const val TEXT = "#F2F7F5"
        private const val MUTED = "#91A6A0"
        private const val GREEN = "#62E6AE"
    }
}

private class SignalHistoryView(context: Context) : View(context) {
    private val samples = mutableListOf<Int>()
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(98, 230, 174)
        strokeWidth = 2.5f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(43, 64, 70)
        strokeWidth = resources.displayMetrics.density
    }
    fun setSamples(values: List<Int>) {
        samples.clear()
        samples.addAll(values.takeLast(60))
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        for (i in 1..3) canvas.drawLine(0f, h * i / 4f, w, h * i / 4f, grid)
        if (samples.size < 2) return
        val min = (samples.minOrNull()!! - 5).toFloat()
        val max = (samples.maxOrNull()!! + 5).toFloat()
        val span = (max - min).coerceAtLeast(10f)
        val path = Path()
        samples.forEachIndexed { index, value ->
            val x = w * index / (samples.size - 1).toFloat()
            // More negative RSRP is plotted lower on screen.
            val y = h - ((value - min) / span) * h
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, line)
    }
}
