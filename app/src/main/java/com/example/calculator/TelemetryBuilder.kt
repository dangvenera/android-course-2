package com.example.calculator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.telephony.*
import androidx.core.app.ActivityCompat
import com.example.calculator.dto.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TelemetryBuilder {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun buildPacketJson(context: Context): String? {
        val loc = getLocationDto(context) ?: return null
        val (lte, gsm, nr) = getCellDtos(context)

        val packet = TelemetryPacketDto(
            location = loc,
            lte = lte,
            gsm = gsm,
            nr = nr
        )
        return json.encodeToString(packet)
    }

    private fun getLocationDto(context: Context): LocationDto? {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: return null

        return LocationDto(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            timestamp = location.time,
            speed = location.speed,
            accuracy = location.accuracy
        )
    }

    private fun getCellDtos(context: Context): Triple<List<CellInfoLteDto>, List<CellInfoGsmDto>, List<CellInfoNrDto>> {
        val needPhone = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val needLoc = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!needPhone || !needLoc) return Triple(emptyList(), emptyList(), emptyList())

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cells = tm.allCellInfo ?: return Triple(emptyList(), emptyList(), emptyList())

        val lteList = mutableListOf<CellInfoLteDto>()
        val gsmList = mutableListOf<CellInfoGsmDto>()
        val nrList = mutableListOf<CellInfoNrDto>()

        for (cell in cells) {
            when (cell) {
                is CellInfoLte -> {
                    val ci = cell.cellIdentity
                    val ss = cell.cellSignalStrength
                    lteList += CellInfoLteDto(
                        CellIdentityLteDto(
                            band = null,
                            cellIdentity = ci.ci,
                            earfcn = ci.earfcn,
                            mcc = ci.mccString,
                            mnc = ci.mncString,
                            pci = ci.pci,
                            tac = ci.tac
                        ),
                        CellSignalStrengthLteDto(
                            asuLevel = ss.asuLevel,
                            cqi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.cqi else null,
                            rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rsrp else null,
                            rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rsrq else null,
                            rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rssi else null,
                            rssnr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.rssnr else null,
                            timingAdvance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.timingAdvance else null
                        )
                    )
                }

                is CellInfoGsm -> {
                    val ci = cell.cellIdentity
                    val ss = cell.cellSignalStrength
                    gsmList += CellInfoGsmDto(
                        CellIdentityGsmDto(
                            cellIdentity = ci.cid,
                            bsic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ci.bsic else null,
                            arfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ci.arfcn else null,
                            lac = ci.lac,
                            mcc = ci.mccString,
                            mnc = ci.mncString,
                            psc = null
                        ),
                        CellSignalStrengthGsmDto(
                            dbm = ss.dbm,
                            rssi = ss.dbm,
                            timingAdvance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ss.timingAdvance else null
                        )
                    )
                }

                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val ci = cell.cellIdentity as CellIdentityNr
                        val ss = cell.cellSignalStrength as CellSignalStrengthNr
                        nrList += CellInfoNrDto(
                            CellIdentityNrDto(
                                band = null,
                                nci = ci.nci,
                                pci = ci.pci,
                                nrarfcn = ci.nrarfcn,
                                tac = ci.tac,
                                mcc = ci.mccString,
                                mnc = ci.mncString
                            ),
                            CellSignalStrengthNrDto(
                                ssRsrp = ss.ssRsrp,
                                ssRsrq = ss.ssRsrq,
                                ssSinr = ss.ssSinr,
                                timingAdvance = null
                            )
                        )
                    }
                }
            }
        }

        return Triple(lteList, gsmList, nrList)
    }
}
