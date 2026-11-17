package com.oscar.smartcontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oscar.smartcontrol.ir.FlipperIrEntry
import com.oscar.smartcontrol.ir.IrButton
import com.oscar.smartcontrol.ir.IrDeviceCategory
import com.oscar.smartcontrol.ir.IrVariant
import com.oscar.smartcontrol.ir.SavedIrRemote
import com.oscar.smartcontrol.ir.TvBrand
import com.oscar.smartcontrol.protocol.remote.Remotemessage.RemoteKeyCode
import com.oscar.smartcontrol.tv.ConnectionState
import kotlin.math.abs

@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onRequestVoice: () -> Boolean,
    onPickMediaToCast: () -> Unit,
    onOpenSystemCast: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val volumeState by viewModel.volumeState.collectAsState()
    val irBrand by viewModel.irBrand.collectAsState()
    val irFallback by viewModel.irFallback.collectAsState()
    val touchSensitivity by viewModel.touchSensitivity.collectAsState()
    val statusToast by viewModel.statusToast.collectAsState()
    val savedIrRemotes by viewModel.savedIrRemotes.collectAsState()
    val activeIrRemote by viewModel.activeIrRemote.collectAsState()
    val activeIrFileButtons by viewModel.activeIrFileButtons.collectAsState()
    val wizardStep by viewModel.wizardStep.collectAsState()
    val wizardCategory by viewModel.wizardCategory.collectAsState()
    val wizardBrands by viewModel.wizardBrands.collectAsState()
    val wizardSelectedBrand by viewModel.wizardSelectedBrand.collectAsState()
    val wizardVariants by viewModel.wizardVariants.collectAsState()
    val wizardIndex by viewModel.wizardIndex.collectAsState()
    val castStatus by viewModel.castStatus.collectAsState()

    var showDevicePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(statusToast) {
        statusToast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(castStatus) {
        castStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.mediaCastManager.clearStatus()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Connection status + Power / Input / Settings
            TopRemoteHeader(
                connectionState = connectionState,
                hasIr = viewModel.hasIrBlaster,
                irBrand = irBrand,
                onOpenPicker = {
                    viewModel.startScan()
                    showDevicePicker = true
                },
                onQuickReconnect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.reconnectSaved()
                },
                onPower = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.sendKey(RemoteKeyCode.KEYCODE_POWER, IrButton.POWER)
                },
                onInput = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.sendKey(RemoteKeyCode.KEYCODE_TV_INPUT, IrButton.INPUT)
                },
                onSettings = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.sendKey(RemoteKeyCode.KEYCODE_SETTINGS, IrButton.SETTINGS)
                },
                onCastMedia = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPickMediaToCast()
                },
                onOpenSystemCast = onOpenSystemCast
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mode Tabs (IR section only shown on devices with hardware IR blaster)
            val availableTabs = remember(viewModel.hasIrBlaster) {
                if (viewModel.hasIrBlaster) RemoteTab.entries
                else RemoteTab.entries.filter { it != RemoteTab.IR }
            }
            val activeTabIndex = availableTabs.indexOf(selectedTab).coerceAtLeast(0)

            ScrollableTabRow(
                selectedTabIndex = activeTabIndex,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                availableTabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(tab)
                        },
                        text = { Text(tab.title, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTab) {
                    RemoteTab.DPAD -> DpadPanel(
                        onKey = { key, irBtn ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendKey(key, irBtn)
                        },
                        onVoiceStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRequestVoice()
                        },
                        onVoiceStop = { viewModel.stopVoice() }
                    )

                    RemoteTab.TOUCHPAD -> TouchpadPanel(
                        thresholdPx = touchSensitivity,
                        onThresholdChange = { viewModel.setTouchSensitivity(it) },
                        onKey = { key ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendKey(key)
                        }
                    )

                    RemoteTab.NUMPAD -> KeyboardNumpadPanel(
                        onSendText = { viewModel.sendText(it) },
                        onSendEnter = { viewModel.sendImeEnter() },
                        onKey = { key ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendKey(key)
                        }
                    )

                    RemoteTab.APPS -> AppsPanel(
                        apps = viewModel.quickApps,
                        onLaunchApp = { link ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.launchApp(link)
                        },
                        onOpenTvSettings = {
                            viewModel.sendKey(RemoteKeyCode.KEYCODE_SETTINGS, IrButton.SETTINGS)
                        }
                    )

                    RemoteTab.IR -> IrSectionPanel(
                        savedRemotes = savedIrRemotes,
                        activeRemote = activeIrRemote,
                        activeFileButtons = activeIrFileButtons,
                        irFallback = irFallback,
                        wizardStep = wizardStep,
                        wizardCategory = wizardCategory,
                        wizardBrands = wizardBrands,
                        wizardSelectedBrand = wizardSelectedBrand,
                        wizardVariants = wizardVariants,
                        wizardIndex = wizardIndex,
                        onStartWizard = { viewModel.startIrWizard() },
                        onWizardBack = { viewModel.wizardBack() },
                        onCancelWizard = { viewModel.cancelIrWizard() },
                        onSelectCategory = { viewModel.selectWizardCategory(it) },
                        onSelectBrand = { viewModel.selectWizardBrand(it) },
                        onTestCurrentVariant = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.testCurrentIrVariant()
                        },
                        onNextVariant = { viewModel.nextIrVariant() },
                        onConfirmVariant = { viewModel.confirmCurrentIrVariant() },
                        onPickVariantDirectly = { idx -> viewModel.selectWizardVariantDirectly(idx) },
                        onSelectSavedRemote = { viewModel.selectSavedIrRemote(it) },
                        onDeleteSavedRemote = { viewModel.deleteSavedIrRemote(it) },
                        onToggleFallback = { viewModel.setIrFallback(it) },
                        onSendPreset = { btn ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendActivePresetButton(btn)
                        },
                        onSendFileEntry = { entry ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendFlipperEntry(entry)
                        },
                        onSendCustomCode = { code ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.sendCustomIr(code)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Persistent Bottom Bar: Volume + Media + Back/Home
            BottomMediaBar(
                volumeState = volumeState,
                onKey = { key, irBtn ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.sendKey(key, irBtn)
                }
            )
        }
    }

    if (showDevicePicker) {
        DevicePickerDialog(
            devices = discoveredDevices,
            isScanning = isScanning,
            lastHost = viewModel.connectionManager.savedHost,
            onScanAgain = { viewModel.startScan() },
            onSelectDevice = { device ->
                showDevicePicker = false
                viewModel.connectToDevice(device)
            },
            onConnectManual = { ip, forcePair ->
                showDevicePicker = false
                viewModel.connectManual(ip, forcePair)
            },
            onDismiss = {
                viewModel.stopScan()
                showDevicePicker = false
            }
        )
    }

    val pairingState = connectionState as? ConnectionState.PairingRequired
    if (pairingState != null) {
        PairingPinDialog(
            host = pairingState.host,
            onSubmitPin = { pin -> viewModel.submitPairingPin(pin) },
            onCancel = { viewModel.connectionManager.disconnect() }
        )
    }
}

@Composable
private fun TopRemoteHeader(
    connectionState: ConnectionState,
    hasIr: Boolean,
    irBrand: TvBrand,
    onOpenPicker: () -> Unit,
    onQuickReconnect: () -> Unit,
    onPower: () -> Unit,
    onInput: () -> Unit,
    onSettings: () -> Unit,
    onCastMedia: () -> Unit,
    onOpenSystemCast: () -> Unit
) {
    val (dotColor, statusText) = when (connectionState) {
        is ConnectionState.Connected -> Color(0xFF4ADE80) to connectionState.name
        is ConnectionState.Connecting -> Color(0xFFFBBF24) to "Conectando..."
        is ConnectionState.PairingRequired -> Color(0xFF60A5FA) to "Ingresar PIN"
        is ConnectionState.Error -> Color(0xFFFF6B6B) to "Sin conexiÃ³n"
        ConnectionState.Disconnected -> Color(0xFFFF6B6B) to "Desconectado"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenPicker)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Icon(
                        Icons.Rounded.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasIr) {
                        Text(
                            text = "IR:${irBrand.name.take(3)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (connectionState !is ConnectionState.Connected) {
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalIconButton(onClick = onQuickReconnect) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reconectar")
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            FilledTonalIconButton(onClick = onCastMedia) {
                Icon(Icons.Rounded.Cast, contentDescription = "Transmitir Foto/Video")
            }
        }

        // Critical quick actions row: Power, Input/HDMI, Settings, Espejar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalButton(
                onClick = onPower,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF3B1A1E),
                    contentColor = Color(0xFFFF8A8A)
                )
            ) {
                Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Power", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Power", maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = onInput,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Rounded.Input, contentDescription = "Input", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("HDMI", maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = onSettings,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "ConfiguraciÃ³n", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ajustes", maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = onOpenSystemCast,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Rounded.Tv, contentDescription = "Espejar", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Espejar", maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DpadPanel(
    onKey: (RemoteKeyCode, IrButton?) -> Unit,
    onVoiceStart: () -> Boolean,
    onVoiceStop: () -> Unit
) {
    var voiceActive by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circular D-Pad
        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Up
            DpadArrowButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                desc = "Arriba",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                onClick = { onKey(RemoteKeyCode.KEYCODE_DPAD_UP, IrButton.UP) }
            )
            // Down
            DpadArrowButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                desc = "Abajo",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                onClick = { onKey(RemoteKeyCode.KEYCODE_DPAD_DOWN, IrButton.DOWN) }
            )
            // Left
            DpadArrowButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                desc = "Izquierda",
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                onClick = { onKey(RemoteKeyCode.KEYCODE_DPAD_LEFT, IrButton.LEFT) }
            )
            // Right
            DpadArrowButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                desc = "Derecha",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                onClick = { onKey(RemoteKeyCode.KEYCODE_DPAD_RIGHT, IrButton.RIGHT) }
            )
            // Center OK
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .clickable { onKey(RemoteKeyCode.KEYCODE_DPAD_CENTER, IrButton.OK) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "OK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Secondary navigation + Voice
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { onKey(RemoteKeyCode.KEYCODE_MENU, IrButton.SETTINGS) },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Rounded.Menu, contentDescription = "MenÃº")
            }

            Surface(
                shape = CircleShape,
                color = if (voiceActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                voiceActive = onVoiceStart()
                                tryAwaitRelease()
                                if (voiceActive) {
                                    onVoiceStop()
                                    voiceActive = false
                                }
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "Voz (mantener)",
                        tint = if (voiceActive) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { onKey(RemoteKeyCode.KEYCODE_INFO, null) },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Rounded.Info, contentDescription = "Info")
            }
        }
    }
}

@Composable
private fun DpadArrowButton(
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun TouchpadPanel(
    thresholdPx: Float,
    onThresholdChange: (Float) -> Unit,
    onKey: (RemoteKeyCode) -> Unit
) {
    var accX by remember { mutableFloatStateOf(0f) }
    var accY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .pointerInput(thresholdPx) {
                    detectTapGestures(
                        onTap = { onKey(RemoteKeyCode.KEYCODE_DPAD_CENTER) },
                        onDoubleTap = { onKey(RemoteKeyCode.KEYCODE_BACK) },
                        onLongPress = { onKey(RemoteKeyCode.KEYCODE_MENU) }
                    )
                }
                .pointerInput(thresholdPx) {
                    detectDragGestures(
                        onDragEnd = {
                            accX = 0f
                            accY = 0f
                        },
                        onDragCancel = {
                            accX = 0f
                            accY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        accX += dragAmount.x
                        accY += dragAmount.y

                        if (abs(accX) >= thresholdPx) {
                            if (accX > 0) onKey(RemoteKeyCode.KEYCODE_DPAD_RIGHT)
                            else onKey(RemoteKeyCode.KEYCODE_DPAD_LEFT)
                            accX = 0f
                            accY = 0f
                        } else if (abs(accY) >= thresholdPx) {
                            if (accY > 0) onKey(RemoteKeyCode.KEYCODE_DPAD_DOWN)
                            else onKey(RemoteKeyCode.KEYCODE_DPAD_UP)
                            accX = 0f
                            accY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Rounded.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    "DeslizÃ¡ para mover â€¢ Tocar = OK",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Doble toque = AtrÃ¡s â€¢ Mantener = MenÃº",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Sensibilidad", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = 145f - thresholdPx,
                onValueChange = { onThresholdChange(145f - it) },
                valueRange = 25f..120f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KeyboardNumpadPanel(
    onSendText: (String) -> Unit,
    onSendEnter: () -> Unit,
    onKey: (RemoteKeyCode) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Escribir en la TV") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = {
                    if (text.isNotEmpty()) {
                        onSendText(text)
                        text = ""
                    } else {
                        onSendEnter()
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar")
            }
            FilledTonalIconButton(onClick = { onKey(RemoteKeyCode.KEYCODE_DEL) }) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Borrar")
            }
        }

        val rows = listOf(
            listOf(
                "1" to RemoteKeyCode.KEYCODE_1,
                "2" to RemoteKeyCode.KEYCODE_2,
                "3" to RemoteKeyCode.KEYCODE_3
            ),
            listOf(
                "4" to RemoteKeyCode.KEYCODE_4,
                "5" to RemoteKeyCode.KEYCODE_5,
                "6" to RemoteKeyCode.KEYCODE_6
            ),
            listOf(
                "7" to RemoteKeyCode.KEYCODE_7,
                "8" to RemoteKeyCode.KEYCODE_8,
                "9" to RemoteKeyCode.KEYCODE_9
            ),
            listOf(
                "CH-" to RemoteKeyCode.KEYCODE_CHANNEL_DOWN,
                "0" to RemoteKeyCode.KEYCODE_0,
                "CH+" to RemoteKeyCode.KEYCODE_CHANNEL_UP
            )
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, code) ->
                    FilledTonalButton(
                        onClick = { onKey(code) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsPanel(
    apps: List<QuickApp>,
    onLaunchApp: (String) -> Unit,
    onOpenTvSettings: () -> Unit
) {
    var customLink by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps) { app ->
                FilledTonalButton(
                    onClick = { onLaunchApp(app.deepLink) },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(app.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item {
                OutlinedButton(
                    onClick = onOpenTvSettings,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ajustes TV")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customLink,
                onValueChange = { customLink = it },
                label = { Text("Deep link o URL (ej. kodi://)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                enabled = customLink.isNotBlank(),
                onClick = { onLaunchApp(customLink.trim()) }
            ) {
                Text("Abrir")
            }
        }
    }
}

@Composable
private fun IrSectionPanel(
    savedRemotes: List<SavedIrRemote>,
    activeRemote: SavedIrRemote?,
    activeFileButtons: List<FlipperIrEntry>,
    irFallback: Boolean,
    wizardStep: IrWizardStep,
    wizardCategory: IrDeviceCategory,
    wizardBrands: List<String>,
    wizardSelectedBrand: String?,
    wizardVariants: List<IrVariant>,
    wizardIndex: Int,
    onStartWizard: () -> Unit,
    onWizardBack: () -> Unit,
    onCancelWizard: () -> Unit,
    onSelectCategory: (IrDeviceCategory) -> Unit,
    onSelectBrand: (String?) -> Unit,
    onTestCurrentVariant: () -> Unit,
    onNextVariant: () -> Unit,
    onConfirmVariant: () -> Unit,
    onPickVariantDirectly: (Int) -> Unit,
    onSelectSavedRemote: (SavedIrRemote) -> Unit,
    onDeleteSavedRemote: (SavedIrRemote) -> Unit,
    onToggleFallback: (Boolean) -> Unit,
    onSendPreset: (IrButton) -> Unit,
    onSendFileEntry: (FlipperIrEntry) -> Unit,
    onSendCustomCode: (String) -> Unit
) {
    when (wizardStep) {
        IrWizardStep.PICK_CATEGORY -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(onClick = onWizardBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Text("1. Elegí el tipo de aparato", style = MaterialTheme.typography.titleMedium)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(IrDeviceCategory.entries) { cat ->
                        FilledTonalButton(
                            onClick = { onSelectCategory(cat) },
                            modifier = Modifier.height(58.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(cat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        IrWizardStep.PICK_BRAND -> {
            var brandQuery by remember { mutableStateOf("") }
            val filtered = remember(wizardBrands, brandQuery) {
                val q = brandQuery.trim().lowercase()
                if (q.isEmpty()) wizardBrands else wizardBrands.filter { it.lowercase().contains(q) }
            }

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(onClick = onWizardBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                    Text("2. Marca (${wizardCategory.title})", style = MaterialTheme.typography.titleMedium)
                }

                OutlinedTextField(
                    value = brandQuery,
                    onValueChange = { brandQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("Buscar marca (ej. RCA, Samsung, Midea...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onSelectBrand(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Universal (probar todas las marcas de ${wizardCategory.title})")
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { brand ->
                        ListItem(
                            headlineContent = { Text(brand) },
                            leadingContent = { Icon(Icons.Rounded.SettingsRemote, contentDescription = null) },
                            modifier = Modifier.clickable { onSelectBrand(brand) }
                        )
                    }
                }
            }
        }

        IrWizardStep.PAIR_TEST -> {
            val currentVariant = wizardVariants.getOrNull(wizardIndex)
            var showManualList by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = onWizardBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                        }
                        Text(
                            "3. Vincular ${wizardSelectedBrand ?: "Universal"}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onCancelWizard) { Text("Cancelar") }
                }

                if (currentVariant == null) {
                    Text(
                        "No se encontraron perfiles para esta selección.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Variante ${wizardIndex + 1} de ${wizardVariants.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                currentVariant.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = onTestCurrentVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Probar Power")
                            }
                            Text(
                                "¿Respondió el aparato?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onNextVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("No, siguiente")
                                }
                                Button(
                                    onClick = onConfirmVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("¡Sí, guardar!")
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showManualList = !showManualList },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (showManualList) "Ocultar lista manual" else "Elegir modelo manualmente (${wizardVariants.size})")
                    }

                    if (showManualList) {
                        wizardVariants.forEachIndexed { idx, v ->
                            ListItem(
                                headlineContent = { Text(v.label) },
                                supportingContent = { Text(v.brandName) },
                                modifier = Modifier.clickable { onPickVariantDirectly(idx) }
                            )
                        }
                    }
                }
            }
        }

        IrWizardStep.NONE -> {
            if (savedRemotes.isEmpty() || activeRemote == null) {
                var showCustom by remember { mutableStateOf(false) }
                var customCode by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.SettingsRemote,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Sin controles IR vinculados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Elegí la categoría (TV, Aire, Audio, etc.), la marca y probá el encendido automático o manual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onStartWizard,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar control IR")
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { showCustom = !showCustom }) {
                        Text(if (showCustom) "Ocultar código manual" else "Enviar código manual (Pronto Hex / .ir)")
                    }
                    if (showCustom) {
                        OutlinedTextField(
                            value = customCode,
                            onValueChange = { customCode = it },
                            label = { Text("Pronto Hex o bloque .ir") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            enabled = customCode.isNotBlank(),
                            onClick = { onSendCustomCode(customCode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Transmitir")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Saved remotes selector + Add button
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(savedRemotes) { remote ->
                            FilterChip(
                                selected = remote.id == activeRemote.id,
                                onClick = { onSelectSavedRemote(remote) },
                                label = { Text(remote.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                        item {
                            FilledTonalButton(onClick = onStartWizard) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Agregar")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto IR (respaldo Wi-Fi)", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(6.dp))
                            Switch(checked = irFallback, onCheckedChange = onToggleFallback)
                        }
                        FilledTonalIconButton(onClick = { onDeleteSavedRemote(activeRemote) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Eliminar control")
                        }
                    }

                    if (activeRemote.presetBrandName != null) {
                        val irRows = listOf(
                            listOf(IrButton.POWER, IrButton.INPUT, IrButton.SETTINGS),
                            listOf(IrButton.VOL_UP, IrButton.MUTE, IrButton.VOL_DOWN),
                            listOf(IrButton.CH_UP, IrButton.HOME, IrButton.CH_DOWN),
                            listOf(IrButton.UP, IrButton.OK, IrButton.BACK),
                            listOf(IrButton.LEFT, IrButton.DOWN, IrButton.RIGHT)
                        )
                        irRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { btn ->
                                    FilledTonalButton(
                                        onClick = { onSendPreset(btn) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(btn.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    } else {
                        if (activeFileButtons.isEmpty()) {
                            Text(
                                "Cargando botones...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            activeFileButtons.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { entry ->
                                        FilledTonalButton(
                                            onClick = { onSendFileEntry(entry) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                entry.name.replace('_', ' '),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun BottomMediaBar(
    volumeState: Triple<Int, Int, Boolean>?,
    onKey: (RemoteKeyCode, IrButton?) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Volume row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onKey(RemoteKeyCode.KEYCODE_VOLUME_DOWN, IrButton.VOL_DOWN) },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Vol -")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { onKey(RemoteKeyCode.KEYCODE_MUTE, IrButton.MUTE) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                ) {
                    val muted = volumeState?.third == true
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Mute"
                        )
                        if (volumeState != null) {
                            Spacer(Modifier.width(4.dp))
                            Text("${volumeState.first}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { onKey(RemoteKeyCode.KEYCODE_VOLUME_UP, IrButton.VOL_UP) },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Vol +")
                }
            }

            // Back + Media + Home row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = { onKey(RemoteKeyCode.KEYCODE_BACK, IrButton.BACK) }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "AtrÃ¡s")
                }
                FilledTonalIconButton(onClick = { onKey(RemoteKeyCode.KEYCODE_MEDIA_REWIND, null) }) {
                    Icon(Icons.Rounded.FastRewind, contentDescription = "Retroceder")
                }
                FilledTonalIconButton(onClick = { onKey(RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE, null) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play/Pausa")
                }
                FilledTonalIconButton(onClick = { onKey(RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD, null) }) {
                    Icon(Icons.Rounded.FastForward, contentDescription = "Adelantar")
                }
                FilledTonalIconButton(onClick = { onKey(RemoteKeyCode.KEYCODE_HOME, IrButton.HOME) }) {
                    Icon(Icons.Rounded.Home, contentDescription = "Home")
                }
            }
        }
    }
}
