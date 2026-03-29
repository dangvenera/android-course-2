package com.example.calculator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class TelephonyActivity : AppCompatActivity() {

    private lateinit var tvInfo: TextView
    private val PERMISSION_ID = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telephony)

        tvInfo = findViewById(R.id.tv_info)

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            loadCellInfo()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            loadCellInfo()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        val readPhone = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val coarseLoc = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return readPhone && coarseLoc
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION),PERMISSION_ID)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadCellInfo()
            } else {
                Toast.makeText(this,"Нужны разрешения READ_PHONE_STATE и ACCESS_COARSE_LOCATION",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadCellInfo() {
        if (!checkPermissions()) return

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = telephonyManager.allCellInfo

        if (cellInfoList.isNullOrEmpty()) {
            tvInfo.text = "Нет данных о сотах (allCellInfo пуст)"
            return
        }

        val sb = StringBuilder()

        for (cell in cellInfoList) {
            when (cell) {

                is CellInfoLte -> {
                    val ci = cell.cellIdentity
                    val ss = cell.cellSignalStrength

                    sb.appendLine("===== LTE =====")
                    sb.appendLine("Band: -")
                    sb.appendLine("CellIdentity (CI): ${ci.ci}")
                    sb.appendLine("EARFCN: ${ci.earfcn}")
                    sb.appendLine("MCC: ${ci.mccString}")
                    sb.appendLine("MNC: ${ci.mncString}")
                    sb.appendLine("PCI: ${ci.pci}")
                    sb.appendLine("TAC: ${ci.tac}")

                    sb.appendLine("ASU Level: ${ss.asuLevel}")
                    sb.appendLine("CQI: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.cqi else "-"}")
                    sb.appendLine("RSRP: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rsrp else "-"}")
                    sb.appendLine("RSRQ: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rsrq else "-"}")
                    sb.appendLine("RSSI: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rssi else "-"}")
                    sb.appendLine("RSSNR: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rssnr else "-"}")
                    sb.appendLine("Timing Advance: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.timingAdvance else "-"}")
                    sb.appendLine()
                }

                is CellInfoGsm -> {
                    val ci = cell.cellIdentity
                    val ss = cell.cellSignalStrength

                    sb.appendLine("===== GSM =====")
                    sb.appendLine("CellIdentity (CID): ${ci.cid}")
                    sb.appendLine("LAC: ${ci.lac}")
                    sb.appendLine("MCC: ${ci.mccString}")
                    sb.appendLine("MNC: ${ci.mncString}")
                    sb.appendLine("ARFCN: ${ci.arfcn}")
                    sb.appendLine("BSIC: ${ci.bsic}")
                    sb.appendLine("PSC: -")
                    sb.appendLine("Dbm: ${ss.dbm}")
                    sb.appendLine("RSSI: ${ss.dbm}")
                    sb.appendLine("Timing Advance: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.timingAdvance else "-"}")
                    sb.appendLine()
                }

                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val ci = cell.cellIdentity as CellIdentityNr
                        val ss = cell.cellSignalStrength as CellSignalStrengthNr

                        sb.appendLine("===== 5G NR =====")

                        sb.appendLine("Band: -")
                        sb.appendLine("NCI: ${ci.nci}")
                        sb.appendLine("PCI: ${ci.pci}")
                        sb.appendLine("NRARFCN: ${ci.nrarfcn}")
                        sb.appendLine("TAC: ${ci.tac}")
                        sb.appendLine("MCC: ${ci.mccString}")
                        sb.appendLine("MNC: ${ci.mncString}")

                        sb.appendLine("SS-RSRP: ${ss.ssRsrp}")
                        sb.appendLine("SS-RSRQ: ${ss.ssRsrq}")
                        sb.appendLine("SS-SINR: ${ss.ssSinr}")
                        sb.appendLine("Timing Advance: - (нет в API)")
                        sb.appendLine()
                    }
                }

                else -> {
                    sb.appendLine("===== Другой тип ячейки: ${cell.javaClass.simpleName} =====")
                    sb.appendLine(cell.toString())
                    sb.appendLine()
                }
            }
        }

        tvInfo.text = sb.toString()
    }
}