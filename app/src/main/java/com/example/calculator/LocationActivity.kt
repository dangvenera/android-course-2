package com.example.calculator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LocationActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvTime: TextView

    private val PERMISSION_ID = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)
        tvTime = findViewById(R.id.tv_time)

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_show_log).setOnClickListener {
            showLog()
        }

        getLastLocation()
    }

    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                if (
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) return

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    this
                )

                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null) {
                    updateUI(lastLocation)
                    saveToJson(lastLocation)
                }

            } else {
                Toast.makeText(this, "Включите геолокацию", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            requestPermissions()
        }
    }

    private fun updateUI(loc: Location) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeFromLocation = sdf.format(Date(loc.time))

        tvLat.text = "Широта: ${loc.latitude}"
        tvLon.text = "Долгота: ${loc.longitude}"
        tvAlt.text = "Высота: ${loc.altitude}"
        tvTime.text = "Время: $timeFromLocation"
    }

    private fun saveToJson(loc: Location) {
        val json = JSONObject()
        json.put("широта", loc.latitude)
        json.put("долгота", loc.longitude)
        json.put("высота", loc.altitude)
        json.put("время", loc.time)

        val file = File(filesDir, "location_log.json")
        file.appendText(json.toString() + "\n")
    }

    override fun onLocationChanged(location: Location) {
        updateUI(location)
        saveToJson(location)
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

    private fun showLog() {
        val file = File(filesDir, "location_log.json")

        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(this, "Файл лога пуст или ещё не создан", Toast.LENGTH_SHORT).show()
            return
        }

        val content = file.readText()

        AlertDialog.Builder(this)
            .setTitle("Лог местоположения")
            .setMessage(content)
            .setPositiveButton("OK", null)
            .show()
    }
}