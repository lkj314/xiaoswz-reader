package com.xiaoswz.reader.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * 网络能力探测（0.16.0）：用于「仅 WiFi 预加载」等流量敏感场景。
 * 仅读取网络状态（ACCESS_NETWORK_STATE 为普通权限，无需运行时申请）。
 */
object NetworkUtils {
    /** 当前是否处于 WiFi 或以太网（非蜂窝）网络 */
    fun isWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
