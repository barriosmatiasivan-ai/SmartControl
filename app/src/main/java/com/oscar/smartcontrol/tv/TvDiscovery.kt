package com.oscar.smartcontrol.tv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address

data class TvDevice(
    val name: String,
    val host: String,
    val port: Int = 6466
)

class TvDiscovery(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _devices = MutableStateFlow<List<TvDevice>>(emptyList())
    val devices: StateFlow<List<TvDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun startDiscovery() {
        stopDiscovery()
        _devices.value = emptyList()

        try {
            multicastLock = wifiManager?.createMulticastLock("SmartControlMdns")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isScanning.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("_androidtvremote2")) {
                    resolveService(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _devices.value = _devices.value.filterNot { it.name == service.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }
        }

        discoveryListener = listener
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
            _isScanning.value = false
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        try {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val host = resolved.host?.let { addr ->
                        if (addr is Inet4Address) addr.hostAddress else addr.hostAddress
                    } ?: return
                    val device = TvDevice(
                        name = resolved.serviceName ?: host,
                        host = host,
                        port = resolved.port.takeIf { it > 0 } ?: 6466
                    )
                    val current = _devices.value.toMutableList()
                    val existingIdx = current.indexOfFirst { it.host == device.host }
                    if (existingIdx >= 0) {
                        current[existingIdx] = device
                    } else {
                        current.add(device)
                    }
                    _devices.value = current
                }
            })
        } catch (_: Exception) {
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (_: Exception) {
            }
        }
        discoveryListener = null
        _isScanning.value = false

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {
        }
        multicastLock = null
    }

    companion object {
        private const val SERVICE_TYPE = "_androidtvremote2._tcp."
    }
}
