package com.oscar.smartcontrol.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.smartcontrol.cast.MediaCastManager
import com.oscar.smartcontrol.ir.FlipperIrEntry
import com.oscar.smartcontrol.ir.IrButton
import com.oscar.smartcontrol.ir.IrController
import com.oscar.smartcontrol.ir.IrDatabase
import com.oscar.smartcontrol.ir.IrDeviceCategory
import com.oscar.smartcontrol.ir.IrVariant
import com.oscar.smartcontrol.ir.SavedIrRemote
import com.oscar.smartcontrol.ir.TvBrand
import com.oscar.smartcontrol.protocol.remote.Remotemessage.RemoteDirection
import com.oscar.smartcontrol.protocol.remote.Remotemessage.RemoteKeyCode
import com.oscar.smartcontrol.tv.ConnectionState
import com.oscar.smartcontrol.tv.TvConnectionManager
import com.oscar.smartcontrol.tv.TvDevice
import com.oscar.smartcontrol.tv.TvDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RemoteTab(val title: String) {
    DPAD("D-Pad"),
    TOUCHPAD("Touchpad"),
    NUMPAD("Teclado"),
    APPS("Apps"),
    IR("IR")
}

enum class IrWizardStep {
    NONE,
    PICK_CATEGORY,
    PICK_BRAND,
    PAIR_TEST
}

data class QuickApp(
    val name: String,
    val deepLink: String
)

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("smart_control_prefs", Context.MODE_PRIVATE)

    val connectionManager = TvConnectionManager(application)
    val discovery = TvDiscovery(application)
    val irController = IrController(application)
    val irDatabase = IrDatabase(application)
    val mediaCastManager = MediaCastManager(application)

    val connectionState: StateFlow<ConnectionState> = connectionManager.state
    val discoveredDevices: StateFlow<List<TvDevice>> = discovery.devices
    val isScanning: StateFlow<Boolean> = discovery.isScanning
    val volumeState: StateFlow<Triple<Int, Int, Boolean>?> = connectionManager.volumeState
    val castStatus: StateFlow<String?> = mediaCastManager.castStatus

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

    // Saved IR remotes & active IR remote
    private val _savedIrRemotes = MutableStateFlow<List<SavedIrRemote>>(loadSavedIrRemotes())
    val savedIrRemotes: StateFlow<List<SavedIrRemote>> = _savedIrRemotes.asStateFlow()

    private val _activeIrRemote = MutableStateFlow<SavedIrRemote?>(_savedIrRemotes.value.firstOrNull())
    val activeIrRemote: StateFlow<SavedIrRemote?> = _activeIrRemote.asStateFlow()

    private val _activeIrFileButtons = MutableStateFlow<List<FlipperIrEntry>>(emptyList())
    val activeIrFileButtons: StateFlow<List<FlipperIrEntry>> = _activeIrFileButtons.asStateFlow()

    // Mi Remote-style IR Wizard state
    private val _wizardStep = MutableStateFlow(IrWizardStep.NONE)
    val wizardStep: StateFlow<IrWizardStep> = _wizardStep.asStateFlow()

    private val _wizardCategory = MutableStateFlow(IrDeviceCategory.TV)
    val wizardCategory: StateFlow<IrDeviceCategory> = _wizardCategory.asStateFlow()

    private val _wizardBrands = MutableStateFlow<List<String>>(emptyList())
    val wizardBrands: StateFlow<List<String>> = _wizardBrands.asStateFlow()

    private val _wizardSelectedBrand = MutableStateFlow<String?>(null)
    val wizardSelectedBrand: StateFlow<String?> = _wizardSelectedBrand.asStateFlow()

    private val _wizardVariants = MutableStateFlow<List<IrVariant>>(emptyList())
    val wizardVariants: StateFlow<List<IrVariant>> = _wizardVariants.asStateFlow()

    private val _wizardIndex = MutableStateFlow(0)
    val wizardIndex: StateFlow<Int> = _wizardIndex.asStateFlow()

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
        _activeIrRemote.value?.let { loadActiveIrRemoteButtons(it) }
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

    fun setIrFallback(enabled: Boolean) {
        _irFallback.value = enabled
        connectionManager.irFallbackEnabled = enabled
    }

    fun startScan() = discovery.startDiscovery()
    fun stopScan() = discovery.stopDiscovery()

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

    // --- IR Wizard (Category -> Brand -> Auto/Manual Test -> Save) ---

    fun startIrWizard() {
        _wizardStep.value = IrWizardStep.PICK_CATEGORY
    }

    fun cancelIrWizard() {
        _wizardStep.value = IrWizardStep.NONE
    }

    fun wizardBack() {
        when (_wizardStep.value) {
            IrWizardStep.PICK_CATEGORY -> _wizardStep.value = IrWizardStep.NONE
            IrWizardStep.PICK_BRAND -> _wizardStep.value = IrWizardStep.PICK_CATEGORY
            IrWizardStep.PAIR_TEST -> _wizardStep.value = IrWizardStep.PICK_BRAND
            IrWizardStep.NONE -> Unit
        }
    }

    fun selectWizardCategory(category: IrDeviceCategory) {
        _wizardCategory.value = category
        _wizardStep.value = IrWizardStep.PICK_BRAND
        viewModelScope.launch(Dispatchers.IO) {
            _wizardBrands.value = irDatabase.getBrandsForCategory(category)
        }
    }

    fun selectWizardBrand(brand: String?) {
        _wizardSelectedBrand.value = brand
        _wizardIndex.value = 0
        _wizardStep.value = IrWizardStep.PAIR_TEST
        viewModelScope.launch(Dispatchers.IO) {
            _wizardVariants.value = irDatabase.getVariantsForBrand(_wizardCategory.value, brand)
        }
    }

    fun testCurrentIrVariant() {
        val variant = _wizardVariants.value.getOrNull(_wizardIndex.value) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (variant) {
                is IrVariant.BuiltIn -> {
                    irController.transmitBrandButton(variant.brand, IrButton.POWER)
                }
                is IrVariant.DbFile -> {
                    val entries = irDatabase.loadButtons(variant.item)
                    val target = entries.firstOrNull {
                        it.name.contains("power", ignoreCase = true) ||
                            it.name.equals("on", ignoreCase = true) ||
                            it.name.contains("turn", ignoreCase = true)
                    } ?: entries.firstOrNull()
                    if (target != null) {
                        irController.transmit(target.signal)
                    }
                }
            }
        }
    }

    fun nextIrVariant() {
        val size = _wizardVariants.value.size
        if (size == 0) return
        _wizardIndex.value = (_wizardIndex.value + 1) % size
    }

    fun selectWizardVariantDirectly(index: Int) {
        if (index in _wizardVariants.value.indices) {
            _wizardIndex.value = index
            confirmCurrentIrVariant()
        }
    }

    fun confirmCurrentIrVariant() {
        val variant = _wizardVariants.value.getOrNull(_wizardIndex.value) ?: return
        val category = _wizardCategory.value
        val remote = when (variant) {
            is IrVariant.BuiltIn -> SavedIrRemote(
                id = "${category.name}_${variant.brand.name}_${System.currentTimeMillis()}",
                name = "${variant.brandName} (${category.title})",
                category = category,
                presetBrandName = variant.brand.name
            )
            is IrVariant.DbFile -> SavedIrRemote(
                id = "${category.name}_${variant.item.path.hashCode()}_${System.currentTimeMillis()}",
                name = variant.item.displayTitle,
                category = category,
                assetPath = variant.item.path
            )
        }

        val updated = listOf(remote) + _savedIrRemotes.value.filterNot {
            it.presetBrandName == remote.presetBrandName && it.assetPath == remote.assetPath
        }
        _savedIrRemotes.value = updated
        persistSavedIrRemotes(updated)
        selectSavedIrRemote(remote)

        if (variant is IrVariant.BuiltIn && category == IrDeviceCategory.TV) {
            _irBrand.value = variant.brand
            connectionManager.selectedIrBrand = variant.brand
        }

        _wizardStep.value = IrWizardStep.NONE
        _statusToast.value = "Control guardado: ${remote.name}"
    }

    fun selectSavedIrRemote(remote: SavedIrRemote) {
        _activeIrRemote.value = remote
        remote.presetBrandName?.let { bName ->
            TvBrand.entries.firstOrNull { it.name == bName }?.let { brand ->
                _irBrand.value = brand
                connectionManager.selectedIrBrand = brand
            }
        }
        loadActiveIrRemoteButtons(remote)
    }

    fun deleteSavedIrRemote(remote: SavedIrRemote) {
        val updated = _savedIrRemotes.value.filterNot { it.id == remote.id }
        _savedIrRemotes.value = updated
        persistSavedIrRemotes(updated)
        if (_activeIrRemote.value?.id == remote.id) {
            val next = updated.firstOrNull()
            _activeIrRemote.value = next
            if (next != null) loadActiveIrRemoteButtons(next) else _activeIrFileButtons.value = emptyList()
        }
    }

    fun sendActivePresetButton(button: IrButton) {
        val presetName = _activeIrRemote.value?.presetBrandName ?: return
        val brand = TvBrand.entries.firstOrNull { it.name == presetName } ?: return
        irController.transmitBrandButton(brand, button)
    }

    fun sendFlipperEntry(entry: FlipperIrEntry) {
        irController.transmit(entry.signal)
    }

    fun sendCustomIr(code: String) {
        val ok = irController.transmitCustomCode(code)
        _statusToast.value = if (ok) "Código IR enviado" else "Formato inválido (usa Pronto Hex o .ir)"
    }

    private fun loadActiveIrRemoteButtons(remote: SavedIrRemote) {
        if (remote.assetPath != null) {
            viewModelScope.launch(Dispatchers.IO) {
                _activeIrFileButtons.value = irDatabase.loadButtonsByPath(remote.assetPath)
            }
        } else {
            _activeIrFileButtons.value = emptyList()
        }
    }

    private fun persistSavedIrRemotes(list: List<SavedIrRemote>) {
        val serialized = list.joinToString("\n") { r ->
            listOf(
                r.id,
                r.name.replace("|", " "),
                r.category.name,
                r.presetBrandName ?: "",
                r.assetPath ?: ""
            ).joinToString("|")
        }
        prefs.edit().putString("saved_ir_remotes", serialized).apply()
    }

    private fun loadSavedIrRemotes(): List<SavedIrRemote> {
        val raw = prefs.getString("saved_ir_remotes", null) ?: return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 5 || parts[0].isBlank()) return@mapNotNull null
            val cat = IrDeviceCategory.entries.firstOrNull { it.name == parts[2] } ?: IrDeviceCategory.TV
            SavedIrRemote(
                id = parts[0],
                name = parts[1],
                category = cat,
                presetBrandName = parts[3].ifBlank { null },
                assetPath = parts[4].ifBlank { null }
            )
        }
    }

    fun sendText(text: String) = connectionManager.sendText(text)
    fun sendImeEnter() = connectionManager.sendImeEnter()
    fun launchApp(deepLink: String) = connectionManager.launchApp(deepLink)
    fun startVoice(): Boolean = connectionManager.startVoice()
    fun stopVoice() = connectionManager.stopVoice()
    fun clearToast() { _statusToast.value = null }
    fun castMedia(uri: Uri) = mediaCastManager.castUri(uri)

    override fun onCleared() {
        super.onCleared()
        discovery.stopDiscovery()
        connectionManager.disconnect()
        mediaCastManager.shutdown()
    }
}
