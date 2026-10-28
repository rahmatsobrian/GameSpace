package com.android.axion.gamespace.compat

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager

/** Ringkasan koneksi aktif: kekuatan sinyal Wi-Fi atau jenis jaringan seluler. */
object NetInfo {
    @Suppress("DEPRECATION")
    fun describe(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(ConnectivityManager::class.java)
            val net = cm.activeNetwork ?: return "No network"
            val caps = cm.getNetworkCapabilities(net) ?: return "-"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
                    val info = wm.connectionInfo
                    "Wi-Fi ${info.rssi}dBm ${info.linkSpeed}Mbps"
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val tm = ctx.getSystemService(TelephonyManager::class.java)
                    val type = try { tm.dataNetworkType } catch (_: SecurityException) { TelephonyManager.NETWORK_TYPE_UNKNOWN }
                    "Mobile " + when (type) {
                        TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                        else -> ""
                    }
                }
                else -> "Network"
            }.trim()
        } catch (_: Exception) {
            "-"
        }
    }
}
