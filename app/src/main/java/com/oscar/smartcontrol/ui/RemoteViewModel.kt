package com.oscar.smartcontrol.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.smartcontrol.cast.MediaCastManager
import com.oscar.smartcontrol.ir.FlipperIrEntry
import com.oscar.smartcontrol.ir.IrButton
import com.oscar.smartcontrol.ir.IrController
import com.oscar.smartcontrol.ir.IrDatabase
import com.oscar.smartcontrol.ir.IrDbItem
import com.oscar.smartcontrol.ir.TvBrand
import com.oscar.smartcontrol.protocol.remote.Remotemessage.RemoteDirection
import com.oscar.smartcontrol.protocol.remote.Remotemessage.RemoteKeyCode
import com.oscar.smartcontrol.tv.ConnectionState
import com.oscar.smartcontrol.tv.TvConnectionManager
import com.oscar.smartcontrol.tv.TvDevice
import com.oscar.smartcontrol.tv.TvDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RemoteTab(val title: String) {
    DPAD("D-Pad"),
    TOUCHPAD("Touchpad"),
    NUMPAD("Teclado"),
    APPS("Apps"),
    IR("IR"),
    IRDB("Base IR")
}

data class QuickApp(
    val name: String,
    val deepLink: String
)

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    val connectionManager = TvConnectionManager(application)
    val discovery = TvDiscovery(application)
    val irController = IrController(application)
    val irDatabase = IrDatabase(application)
    val mediaCastManager = MediaCastManager(application)

    val castStatus: StateFlow<String?> = mediaCastManager.castStatus

    val connectionState: StateFlow<ConnectionState> = connectionManager.state
    val discoveredDevices: StateFlow<List<TvDevice>> = discovery.devices
    val isScanning: StateFlow<Boolean> = discovery.isScanning
    val volumeState: StateFlow<Triple<Int, Int, Boolean>?> = connectionManager.volumeState

    private val _selectedTab = MutableStateFlow(RemoteTab.DPAD)
    val selectedTab: StateFlow<RemoteTab> = _selectedTab.asStateFlow()

    private val _irBrand = MutableStateFlow(connectionManager.selectedIrBrand)
    val irBrand: StateFlow<TvBrand> = _irBrand.asStateFlow()

    private val _irFallback = MutableStateFlow(connectionManager.irFallbackEnabled)
    val irFallback: StateFlow<Boolean> = _irFallback.asStateFlow()

    private val _touchSensitivity = MutableStateFlow(55f)
    val touchSensitivity: StateFlow<Float> = _touchSensitivity.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val _irScanBrand = MutableStateFlow<TvBrand?>(null)
    val irScanBrand: StateFlow<TvBrand?> = _irScanBrand.asStateFlow()

    private val _flipperButtons = MutableStateFlow<List<FlipperIrEntry>>(
        IrController.parseFlipperFile(connectionManager.customIrCode)
    )
    val flipperButtons: StateFlow<List<FlipperIrEntry>> = _flipperButtons.asStateFlow()

    private val _irdbCategories = MutableStateFlow<List<String>>(emptyList())
    val irdbCategories: StateFlow<List<String>> = _irdbCategories.asStateFlow()

    private val _irdbResults = MutableStateFlow<List<IrDbItem>>(emptyList())
    val irdbResults: StateFlow<List<IrDbItem>> = _irdbResults.asStateFlow()

    private val _selectedIrDbItem = MutableStateFlow<IrDbItem?>(null)
    val selectedIrDbItem: StateFlow<IrDbItem?> = _selectedIrDbItem.asStateFlow()

    private val _selectedIrDbButtons = MutableStateFlow<List<FlipperIrEntry>>(emptyList())
    val selectedIrDbButtons: StateFlow<List<FlipperIrEntry>> = _selectedIrDbButtons.asStateFlow()

    private var irScanJob: Job? = null

    val hasIrBlaster: Boolean = irController.hasIrBlaster

    val quickApps = listOf(
        QuickApp("YouTube", "https://www.youtube.com"),
        QuickApp("Netflix", "https://www.netflix.com"),
        QuickApp("Prime Video", "https://app.primevideo.com"),
        QuickApp("Disney+", "https://www.disneyplus.com"),
        QuickApp("Max", "https://play.max.com"),
        QuickApp("Spotify", "spotify://"),
        QuickApp("Plex", "plex://"),
        QuickApp("Stremio", "stremio://"),
        QuickApp("Twitch", "twitch://")
    )

    init {
        connectionManager.autoConnectIfSaved()
        viewModelScope.launch(Dispatchers.IO) {
            _irdbCategories.value = irDatabase.getCategories()
            _irdbResults.value = irDatabase.search("", null, 80)
        }
    }

    fun onForeground() {
        connectionManager.onForeground()
    }

    fun selectTab(tab: RemoteTab) {
        _selectedTab.value = tab
    }

    fun setTouchSensitivity(pxThreshold: Float) {
        _touchSensitivity.value = pxThreshold.coerceIn(25f, 120f)
    }

    fun setIrBrand(brand: TvBrand) {
        _irBrand.value = brand
        connectionManager.selectedIrBrand = brand
    }

    fun setIrFallback(enabled: Boolean) {
        _irFallback.value = enabled
        connectionManager.irFallbackEnabled = enabled
    }

    fun startScan() {
        discovery.startDiscovery()
    }

    fun stopScan() {
        discovery.stopDiscovery()
    }

    fun connectToDevice(device: TvDevice) {
        discovery.stopDiscovery()
        connectionManager.connectOrReconnect(device.host, device.name, forcePairing = false)
    }

    fun connectManual(host: String, forcePairing: Boolean = false) {
        discovery.stopDiscovery()
        connectionManager.connectOrReconnect(host, host, forcePairing = forcePairing)
    }

    fun reconnectSaved() {
        val host = connectionManager.savedHost ?: return
        connectionManager.connectOrReconnect(host, connectionManager.savedName, forcePairing = false)
    }

    fun submitPairingPin(pin: String) {
        connectionManager.providePairingCode(pin)
    }

    fun sendKey(keyCode: RemoteKeyCode, irFallbackButton: IrButton? = null) {
        val sentWifi = connectionManager.sendKey(keyCode, RemoteDirection.SHORT)
        if (!sentWifi && irFallbackButton != null && _irFallback.value && hasIrBlaster) {
            val sentIr = irController.transmitBrandButton(_irBrand.value, irFallbackButton)
            if (sentIr) {
                _statusToast.value = "Enviado por IR (${_irBrand.value.displayName})"
            }
        }
    }

    fun sendIrButton(button: IrButton) {
        if (!hasIrBlaster) {
            _statusToast.value = "Este teléfono no tiene emisor IR"
            return
        }
        val ok = irController.transmitBrandButton(_irBrand.value, button)
        if (!ok) {
            _statusToast.value = "Botón no disponible para ${_irBrand.value.displayName}"
        }
    }

    fun sendCustomIr(code: String) {
        if (!hasIrBlaster) {
            _statusToast.value = "Este teléfono no tiene emisor IR"
            return
        }
        val ok = irController.transmitCustomCode(code)
        _statusToast.value = if (ok) {
            connectionManager.customIrCode = code
            "Código IR enviado"
        } else {
            "Formato inválido (usa Pronto Hex o Flipper .ir)"
        }
    }

    fun importFlipperFile(content: String) {
        val entries = IrController.parseFlipperFile(content)
        if (entries.isEmpty()) {
            _statusToast.value = "No se encontraron botones válidos en el archivo .ir"
        } else {
            connectionManager.customIrCode = content
            _flipperButtons.value = entries
            _statusToast.value = "${entries.size} botones IR importados"
        }
    }

    fun sendFlipperEntry(entry: FlipperIrEntry) {
        if (!hasIrBlaster) {
            _statusToast.value = "Este teléfono no tiene emisor IR"
            return
        }
        irController.transmit(entry.signal)
    }

    fun startIrBrandScan(button: IrButton = IrButton.POWER) {
        if (!hasIrBlaster) {
            _statusToast.value = "Este teléfono no tiene emisor IR"
            return
        }
        irScanJob?.cancel()
        irScanJob = viewModelScope.launch(Dispatchers.IO) {
            for (brand in TvBrand.entries) {
                _irScanBrand.value = brand
                setIrBrand(brand)
                irController.transmitBrandButton(brand, button)
                delay(900)
            }
            _irScanBrand.value = null
            _statusToast.value = "Escaneo IR finalizado"
        }
    }

    fun stopIrBrandScan(confirmCurrent: Boolean = true) {
        irScanJob?.cancel()
        irScanJob = null
        val current = _irScanBrand.value
        _irScanBrand.value = null
        if (confirmCurrent && current != null) {
            setIrBrand(current)
            _statusToast.value = "Perfil guardado: ${current.displayName}"
        }
    }

    fun sendText(text: String) {
        connectionManager.sendText(text)
    }

    fun sendImeEnter() {
        connectionManager.sendImeEnter()
    }

    fun launchApp(deepLink: String) {
        connectionManager.launchApp(deepLink)
    }

    fun startVoice(): Boolean = connectionManager.startVoice()

    fun stopVoice() = connectionManager.stopVoice()

    fun clearToast() {
        _statusToast.value = null
    }

    fun searchIrDb(query: String, category: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _irdbResults.value = irDatabase.search(query, category, 80)
        }
    }

    fun openIrDbItem(item: IrDbItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val buttons = irDatabase.loadButtons(item)
            _selectedIrDbItem.value = item
            _selectedIrDbButtons.value = buttons
        }
    }

    fun closeIrDbItem() {
        _selectedIrDbItem.value = null
        _selectedIrDbButtons.value = emptyList()
    }

    fun castMedia(uri: Uri) {
        mediaCastManager.castUri(uri)
    }

    fun stopCast() {
        mediaCastManager.stopCast()
    }

    override fun onCleared() {
        super.onCleared()
        discovery.stopDiscovery()
        connectionManager.disconnect()
        mediaCastManager.shutdown()
    }
}
