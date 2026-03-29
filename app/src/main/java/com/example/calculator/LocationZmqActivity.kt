package com.example.calculator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class LocationZmqActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager

    private lateinit var tvStatus: TextView
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvSent: TextView

    private val PERMISSION_ID = 101

    private val serverIp = "192.168.0.11"
    private val serverPort = 5555
    private val endpoint = "tcp://$serverIp:$serverPort"

    @Volatile private var latestJsonToSend: String? = null
    private val senderRunning = AtomicBoolean(false)
    private var senderThread: Thread? = null

    private val localLogFileName = "location_zmq_sent_log.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_zmq)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        tvStatus = findViewById(R.id.tv_status)
        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)
        tvTime = findViewById(R.id.tv_time)
        tvSent = findViewById(R.id.tv_sent)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            startSendingLoop()
            getLastLocationAndSubscribe()
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopSendingLoop()
            stopLocationUpdates()
        }

        findViewById<Button>(R.id.btn_show_log).setOnClickListener {
            showLocalLog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSendingLoop()
        stopLocationUpdates()
    }

    private fun getLastLocationAndSubscribe() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Включите геолокацию", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            1f,
            this
        )

        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { loc ->
            handleLocation(loc)
        }

        runOnUiThread {
            tvStatus.text = "Статус: подписались на GPS, endpoint=$endpoint"
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {
        handleLocation(location)
    }

    private fun handleLocation(loc: Location) {
        updateUI(loc)
        val jsonStr = buildLocationJson(loc)
        latestJsonToSend = jsonStr
        appendLocalLog(jsonStr)
    }

    private fun buildLocationJson(loc: Location): String {
        val json = JSONObject()
        json.put("latitude", loc.latitude)
        json.put("longitude", loc.longitude)
        json.put("altitude", loc.altitude)

        json.put("time", loc.time)

        json.put("accuracy", loc.accuracy.toDouble())
        json.put("provider", loc.provider ?: "unknown")

        return json.toString()
    }

    private fun updateUI(loc: Location) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timePretty = sdf.format(Date(loc.time))

        tvLat.text = "Широта: ${loc.latitude}"
        tvLon.text = "Долгота: ${loc.longitude}"
        tvAlt.text = "Высота: ${loc.altitude}"
        tvTime.text = "Время (экранное): $timePretty"
    }

    private fun appendLocalLog(jsonLine: String) {
        val f = File(filesDir, localLogFileName)
        f.appendText(jsonLine + "\n")
    }

    private fun showLocalLog() {
        val f = File(filesDir, localLogFileName)
        if (!f.exists() || f.length() == 0L) {
            Toast.makeText(this, "Лог отправок пуст или не создан", Toast.LENGTH_SHORT).show()
            return
        }
        val content = f.readText()
        AlertDialog.Builder(this)
            .setTitle("Локальный лог отправок (JSON lines)")
            .setMessage(content)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startSendingLoop() {
        if (senderRunning.get()) return
        senderRunning.set(true)

        senderThread = Thread {
            ZContext().use { ctx ->
                var socket = ctx.createSocket(SocketType.REQ)

                fun recreateSocket() {
                    try { socket.close() } catch (_: Exception) {}
                    socket = ctx.createSocket(SocketType.REQ)

                    socket.receiveTimeOut = 2000
                    socket.sendTimeOut = 2000

                    socket.connect(endpoint)
                }

                recreateSocket()

                var lastSentPayload: String? = null
                var failCount = 0

                while (senderRunning.get()) {
                    val payload = latestJsonToSend

                    if (payload == null || payload == lastSentPayload) {
                        SystemClock.sleep(200)
                        continue
                    }

                    try {
                        socket.send(payload.toByteArray(ZMQ.CHARSET), 0)
                        val replyBytes = socket.recv(0)
                        if (replyBytes == null) {
                            throw RuntimeException("recv timeout / no reply")
                        }
                        val reply = String(replyBytes, ZMQ.CHARSET)

                        lastSentPayload = payload
                        failCount = 0

                        runOnUiThread {
                            tvStatus.text = "Статус: отправлено/получен ответ"
                            tvSent.text = "Последний JSON:\n$payload\n\nОтвет сервера:\n$reply"
                        }
                    } catch (e: Exception) {
                        failCount++
                        runOnUiThread {
                            tvStatus.text = "Статус: ошибка (${e.message}), переподключаемся... (fail=$failCount)"
                        }
                        try { recreateSocket() } catch (_: Exception) {}
                        SystemClock.sleep(1000)
                    }
                }

                try { socket.close() } catch (_: Exception) {}
            }
        }

        senderThread!!.isDaemon = true
        senderThread!!.start()

        runOnUiThread {
            tvStatus.text = "Статус: ZMQ sender запущен, endpoint=$endpoint"
        }
    }

    private fun stopSendingLoop() {
        senderRunning.set(false)
        try { senderThread?.interrupt() } catch (_: Exception) {}
        senderThread = null
        runOnUiThread {
            tvStatus.text = "Статус: остановлено"
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}