package com.oscar.smartcontrol.tv

import android.content.Context
import com.oscar.smartcontrol.ir.TvBrand
import com.oscar.smartcontrol.protocol.AndroidRemoteContext
import com.oscar.smartcontrol.protocol.AndroidRemoteTv
import com.oscar.smartcontrol.protocol.AndroidTvListener
import com.oscar.smartcontrol.protocol.remote.Remotemessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val host: String) : ConnectionState()
    data class PairingRequired(val host: String) : ConnectionState()
    data class Connected(val host: String, val name: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class TvConnectionManager(context: Context) {

    private val prefs = context.getSharedPreferences("smart_control_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var remoteTv: AndroidRemoteTv? = null
    private var reconnectJob: Job? = null
    private var shouldAutoReconnect = false

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _imeEvent = MutableStateFlow<Pair<String, Int>?>(null)
    val imeEvent: StateFlow<Pair<String, Int>?> = _imeEvent.asStateFlow()

    private val _volumeState = MutableStateFlow<Triple<Int, Int, Boolean>?>(null)
    val volumeState: StateFlow<Triple<Int, Int, Boolean>?> = _volumeState.asStateFlow()

    var savedHost: String?
        get() = prefs.getString("last_host", null)
        private set(value) = prefs.edit().putString("last_host", value).apply()

    var savedName: String
        get() = prefs.getString("last_name", "Google TV") ?: "Google TV"
        private set(value) = prefs.edit().putString("last_name", value).apply()

    var selectedIrBrand: TvBrand
        get() {
            val name = prefs.getString("ir_brand", TvBrand.TCL.name)
            return TvBrand.entries.firstOrNull { it.name == name } ?: TvBrand.TCL
        }
        set(value) = prefs.edit().putString("ir_brand", value.name).apply()

    var irFallbackEnabled: Boolean
        get() = prefs.getBoolean("ir_fallback", true)
        set(value) = prefs.edit().putBoolean("ir_fallback", value).apply()

    var customIrCode: String
        get() = prefs.getString("custom_ir_code", "") ?: ""
        set(value) = prefs.edit().putString("custom_ir_code", value).apply()

    init {
        val ctx = AndroidRemoteContext.getInstance()
        ctx.keyStoreFile = File(context.filesDir, "androidtv.keystore")
        ctx.serviceName = "com.oscar.smartcontrol"
    }

    fun autoConnectIfSaved() {
        val host = savedHost ?: return
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            return
        }
        connectOrReconnect(host, savedName, forcePairing = false)
    }

    fun onForeground() {
        val host = savedHost ?: return
        if (remoteTv?.isConnected() != true && _state.value !is ConnectionState.PairingRequired) {
            connectOrReconnect(host, savedName, forcePairing = false)
        }
    }

    fun connectOrReconnect(host: String, name: String = host, forcePairing: Boolean = false) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) return

        savedHost = cleanHost
        savedName = name
        shouldAutoReconnect = true
        reconnectJob?.cancel()

        scope.launch {
            disconnectInternal(keepAutoReconnect = true)
            _state.value = ConnectionState.Connecting(cleanHost)

            val tv = AndroidRemoteTv()
            remoteTv = tv
            tv.setVolumeChangedCallback { level, maxLevel, muted ->
                _volumeState.value = Triple(level, maxLevel, muted)
            }

            val listener = createListener(cleanHost, name)
            try {
                if (forcePairing) {
                    tv.connect(cleanHost, listener)
                } else {
                    tv.reconnect(cleanHost, listener)
                }
            } catch (e: Exception) {
                // If reconnect failed (e.g. certificate not paired yet), fall back to pairing
                if (!forcePairing) {
                    try {
                        tv.abort()
                        val pairingTv = AndroidRemoteTv()
                        remoteTv = pairingTv
                        pairingTv.connect(cleanHost, listener)
                    } catch (pairEx: Exception) {
                        _state.value = ConnectionState.Error(pairEx.message ?: "Error al conectar")
                    }
                } else {
                    _state.value = ConnectionState.Error(e.message ?: "Error al conectar")
                }
            }
        }
    }

    fun providePairingCode(pin: String) {
        val clean = pin.trim()
        if (clean.isEmpty()) return
        scope.launch {
            try {
                remoteTv?.sendSecret(clean)
            } catch (e: Exception) {
                _state.value = ConnectionState.Error(e.message ?: "PIN inválido")
            }
        }
    }

    fun sendKey(
        keyCode: Remotemessage.RemoteKeyCode,
        direction: Remotemessage.RemoteDirection = Remotemessage.RemoteDirection.SHORT
    ): Boolean {
        val tv = remoteTv
        if (tv == null || !tv.isConnected()) return false
        scope.launch {
            try {
                tv.sendCommand(keyCode, direction)
            } catch (_: Exception) {
                scheduleAutoReconnect()
            }
        }
        return true
    }

    fun sendText(text: String) {
        val tv = remoteTv ?: return
        if (!tv.isConnected()) return
        scope.launch {
            try {
                val counter = _imeEvent.value?.second ?: 0
                tv.sendText(text, counter, counter)
            } catch (_: Exception) {
            }
        }
    }

    fun sendImeEnter() {
        val tv = remoteTv ?: return
        if (!tv.isConnected()) return
        scope.launch {
            try {
                tv.sendImeEnter()
            } catch (_: Exception) {
            }
        }
    }

    fun launchApp(appLink: String) {
        val tv = remoteTv ?: return
        if (!tv.isConnected()) return
        scope.launch {
            try {
                tv.sendAppLink(appLink)
            } catch (_: Exception) {
            }
        }
    }

    fun startVoice(): Boolean {
        val tv = remoteTv ?: return false
        if (!tv.isConnected()) return false
        return try {
            tv.startVoice()
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun stopVoice() {
        try {
            remoteTv?.stopVoice()
        } catch (_: Exception) {
        }
    }

    fun disconnect() {
        shouldAutoReconnect = false
        reconnectJob?.cancel()
        scope.launch {
            disconnectInternal(keepAutoReconnect = false)
            _state.value = ConnectionState.Disconnected
        }
    }

    private fun disconnectInternal(keepAutoReconnect: Boolean) {
        shouldAutoReconnect = keepAutoReconnect
        try {
            remoteTv?.abort()
        } catch (_: Exception) {
        }
        remoteTv = null
    }

    private fun scheduleAutoReconnect() {
        if (!shouldAutoReconnect) return
        val host = savedHost ?: return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            val delays = listOf(1000L, 2500L, 5000L)
            for (waitMs in delays) {
                if (!shouldAutoReconnect) return@launch
                if (remoteTv?.isConnected() == true) return@launch
                delay(waitMs)
                if (!shouldAutoReconnect) return@launch
                try {
                    _state.value = ConnectionState.Connecting(host)
                    val tv = AndroidRemoteTv()
                    remoteTv = tv
                    tv.reconnect(host, createListener(host, savedName))
                    return@launch
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun createListener(host: String, name: String) = object : AndroidTvListener {
        override fun onSessionCreated() {}

        override fun onSecretRequested() {
            _state.value = ConnectionState.PairingRequired(host)
        }

        override fun onPaired() {}

        override fun onConnectingToRemote() {
            _state.value = ConnectionState.Connecting(host)
        }

        override fun onConnected() {
            _state.value = ConnectionState.Connected(host, name)
        }

        override fun onDisconnect() {
            if (_state.value !is ConnectionState.PairingRequired) {
                _state.value = ConnectionState.Disconnected
                scheduleAutoReconnect()
            }
        }

        override fun onImeShow(text: String?, fieldCounter: Int) {
            _imeEvent.value = (text ?: "") to fieldCounter
        }

        override fun onError(message: String?) {
            val msg = message ?: "Error de conexión"
            if (msg.contains("pairing", ignoreCase = true) || msg.contains("ssl", ignoreCase = true)) {
                connectOrReconnect(host, name, forcePairing = true)
            } else {
                _state.value = ConnectionState.Error(msg)
                scheduleAutoReconnect()
            }
        }
    }
}
