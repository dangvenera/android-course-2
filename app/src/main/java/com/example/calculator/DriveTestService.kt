package com.example.calculator

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.telephony.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class DriveTestService : Service(), LocationListener {

    companion object {
        const val NOTIFICATION_ID = 2020
        const val CHANNEL_ID = "drive_test_channel"
    }

    private val running = AtomicBoolean(false)

    @Volatile
    private var lastLocation: Location? = null

    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager

    private val logFileName = "drive_test_log.jsonl"
    private var workerThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        startAsForeground("Drive-test запущен")

        if (running.get()) return START_STICKY

        running.set(true)
        subscribeLocation()

        workerThread = Thread {
            while (running.get()) {
                try {
                    val payload = buildTelemetryJson().toString()
                    File(filesDir, logFileName).appendText(payload + "\n")
                    updateNotification("Записано: ${System.currentTimeMillis()}")
                    Thread.sleep(5000)
                } catch (_: Exception) {
                    try {
                        Thread.sleep(1500)
                    } catch (_: Exception) {
                    }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        running.set(false)

        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {
        }

        try {
            workerThread?.interrupt()
        } catch (_: Exception) {
        }

        workerThread = null
        super.onDestroy()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun startAsForeground(text: String) {
        createNotificationChannelIfNeeded()
        val notification = buildNotification(text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Drive Test",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Drive Test")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun subscribeLocation() {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    1f,
                    this
                )
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    1f,
                    this
                )
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        } catch (_: Exception) {
        }
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    private fun buildTelemetryJson(): JSONObject {
        val root = JSONObject()
        root.put("type", "telemetry")
        root.put("ts_client_ms", System.currentTimeMillis())
        root.put("location", collectLocationJson())
        root.put("cells", collectCellsJson())
        root.put("traffic", collectTrafficJson())
        return root
    }

    private fun collectLocationJson(): JSONObject {
        val locObj = JSONObject()
        val loc = lastLocation

        if (loc != null) {
            locObj.put("latitude", loc.latitude)
            locObj.put("longitude", loc.longitude)
            locObj.put("altitude", loc.altitude)
            locObj.put("time", loc.time)
            locObj.put("accuracy", loc.accuracy.toDouble())
            locObj.put("provider", loc.provider ?: "unknown")
        } else {
            locObj.put("status", "no_location")
        }

        return locObj
    }

    private fun collectTrafficJson(): JSONObject {
        val t = JSONObject()
        t.put("total_rx_bytes", TrafficStats.getTotalRxBytes())
        t.put("total_tx_bytes", TrafficStats.getTotalTxBytes())
        t.put("top_apps_2sigma", JSONArray())
        t.put("top_apps_note", "")
        return t
    }

    private fun collectCellsJson(): JSONArray {
        val arr = JSONArray()

        val phoneGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!phoneGranted) return arr

        val list = try {
            telephonyManager.allCellInfo
        } catch (_: Exception) {
            null
        }

        if (list.isNullOrEmpty()) return arr

        for (cell in list) {
            when (cell) {
                is CellInfoLte -> {
                    val o = JSONObject()
                    val ci = cell.cellIdentity
                    val ss = cell.cellSignalStrength

                    o.put("radio", "LTE")
                    o.put("band", "-")
                    o.put("ci", ci.ci)
                    o.put("earfcn", ci.earfcn)
                    o.put("mcc", ci.mccString ?: "")
                    o.put("mnc", ci.mncString ?: "")
                    o.put("pci", ci.pci)
                    o.put("tac", ci.tac)

                    o.put("asuLevel", ss.asuLevel)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        o.put("cqi", ss.cqi)
                        o.put("rsrp", ss.rsrp)
                        o.put("rsrq", ss.rsrq)
                        o.put("rssi", ss.rssi)
                        o.put("rssnr", ss.rssnr)
                        o.put("timingAdvance", ss.timingAdvance)
                    }
                    arr.put(o)
                }

                is CellInfoGsm -> {
                    val o = JSONObject()
                    val ci = cell.cellIdentity
                    val ss = cell.cellSignalStrength

                    o.put("radio", "GSM")
                    o.put("cid", ci.cid)
                    o.put("bsic", ci.bsic)
                    o.put("arfcn", ci.arfcn)
                    o.put("lac", ci.lac)
                    o.put("mcc", ci.mccString ?: "")
                    o.put("mnc", ci.mncString ?: "")
                    o.put("psc", "-")

                    o.put("dbm", ss.dbm)
                    o.put("rssi", ss.dbm)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        o.put("timingAdvance", ss.timingAdvance)
                    }
                    arr.put(o)
                }

                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val o = JSONObject()
                        val ci = cell.cellIdentity as CellIdentityNr
                        val ss = cell.cellSignalStrength as CellSignalStrengthNr

                        o.put("radio", "NR")
                        o.put("band", "-")
                        o.put("nci", ci.nci)
                        o.put("pci", ci.pci)
                        o.put("nrarfcn", ci.nrarfcn)
                        o.put("tac", ci.tac)
                        o.put("mcc", ci.mccString ?: "")
                        o.put("mnc", ci.mncString ?: "")

                        o.put("ssRsrp", ss.ssRsrp)
                        o.put("ssRsrq", ss.ssRsrq)
                        o.put("ssSinr", ss.ssSinr)
                        o.put("timingAdvance", "-")

                        arr.put(o)
                    }
                }
            }
        }

        return arr
    }
}