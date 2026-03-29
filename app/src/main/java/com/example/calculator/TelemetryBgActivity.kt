package com.example.calculator

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class TelemetryBgActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLastJson: TextView

    private val PERMISSION_ID = 300
    private val logFileName = "telemetry_bg_log.jsonl"

    private val bgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelemetryBgService.ACTION_BG_UPDATE) {
                val status = intent.getStringExtra(TelemetryBgService.EXTRA_STATUS) ?: "—"
                val lastJson = intent.getStringExtra(TelemetryBgService.EXTRA_LAST_JSON)

                tvStatus.text = "Статус: $status"

                if (!lastJson.isNullOrBlank()) {
                    tvLastJson.text = "Последний JSON:\n$lastJson"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telemetry_bg)

        tvStatus = findViewById(R.id.tv_status)
        tvLastJson = findViewById(R.id.tv_last_json)

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (!checkPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }
            startBgService()
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopService(Intent(this, TelemetryBgService::class.java))
            tvStatus.text = "Статус: остановлено"
        }

        findViewById<Button>(R.id.btn_usage_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        findViewById<Button>(R.id.btn_show_log).setOnClickListener {
            showLog()
        }
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter(TelemetryBgService.ACTION_BG_UPDATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bgReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bgReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(bgReceiver)
        } catch (_: Exception) {
        }
    }

    private fun startBgService() {
        val intent = Intent(this, TelemetryBgService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        tvStatus.text = "Статус: foreground service запускается..."
    }

    private fun showLog() {
        val f = File(filesDir, logFileName)
        if (!f.exists() || f.length() == 0L) {
            Toast.makeText(this, "Лог пуст или не создан", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Лог (JSON lines)")
            .setMessage(f.readText())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun checkPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val phone = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return fine && coarse && phone
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBgService()
            } else {
                Toast.makeText(
                    this,
                    "Нужны разрешения: Location + READ_PHONE_STATE",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}