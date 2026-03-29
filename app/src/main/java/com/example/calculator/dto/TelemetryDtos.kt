package com.example.calculator.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: Long,
    val speed: Float? = null,
    val accuracy: Float? = null
)

@Serializable
data class CellInfoLteDto(
    val cellIdentityLte: CellIdentityLteDto,
    val cellSignalStrengthLte: CellSignalStrengthLteDto
)

@Serializable
data class CellIdentityLteDto(
    val band: Int? = null,
    val cellIdentity: Int? = null, // CI
    val earfcn: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val pci: Int? = null,
    val tac: Int? = null
)

@Serializable
data class CellSignalStrengthLteDto(
    val asuLevel: Int? = null,
    val cqi: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val rssi: Int? = null,
    val rssnr: Int? = null,
    val timingAdvance: Int? = null
)

@Serializable
data class CellInfoGsmDto(
    val cellIdentityGsm: CellIdentityGsmDto,
    val cellSignalStrengthGsm: CellSignalStrengthGsmDto
)

@Serializable
data class CellIdentityGsmDto(
    val cellIdentity: Int? = null, // CID
    val bsic: Int? = null,
    val arfcn: Int? = null,
    val lac: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val psc: Int? = null
)

@Serializable
data class CellSignalStrengthGsmDto(
    val dbm: Int? = null,
    val rssi: Int? = null,
    val timingAdvance: Int? = null
)

@Serializable
data class CellInfoNrDto(
    val cellIdentityNr: CellIdentityNrDto,
    val cellSignalStrengthNr: CellSignalStrengthNrDto
)

@Serializable
data class CellIdentityNrDto(
    val band: Int? = null,
    val nci: Long? = null,
    val pci: Int? = null,
    val nrarfcn: Int? = null,
    val tac: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null
)

@Serializable
data class CellSignalStrengthNrDto(
    @SerialName("ss_rsrp") val ssRsrp: Int? = null,
    @SerialName("ss_rsrq") val ssRsrq: Int? = null,
    @SerialName("ss_sinr") val ssSinr: Int? = null,
    val timingAdvance: Int? = null
)

@Serializable
data class TelemetryPacketDto(
    val location: LocationDto,
    val lte: List<CellInfoLteDto> = emptyList(),
    val gsm: List<CellInfoGsmDto> = emptyList(),
    val nr: List<CellInfoNrDto> = emptyList()
)
