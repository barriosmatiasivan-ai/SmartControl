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
import com.oscar.smartcontrol.ir.IrDbItem
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
    val irScanBrand by viewModel.irScanBrand.collectAsState()
    val flipperButtons by viewModel.flipperButtons.collectAsState()
    val irdbCategories by viewModel.irdbCategories.collectAsState()
    val irdbResults by viewModel.irdbResults.collectAsState()
    val selectedIrDbItem by viewModel.selectedIrDbItem.collectAsState()
    val selectedIrDbButtons by viewModel.selectedIrDbButtons.collectAsState()
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

            // Mode Tabs (IR sections only shown on devices with hardware IR blaster)
            val availableTabs = remember(viewModel.hasIrBlaster) {
                if (viewModel.hasIrBlaster) RemoteTab.entries
                else RemoteTab.entries.filter { it != RemoteTab.IR && it != RemoteTab.IRDB }
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

                    RemoteTab.IR -> IrUniversalPanel(
                        hasIr = viewModel.hasIrBlaster,
                        selectedBrand = irBrand,
                        irFallback = irFallback,
                        irScanBrand = irScanBrand,
                        flipperButtons = flipperButtons,
                        savedCustomCode = viewModel.connectionManager.customIrCode,
                        onSelectBrand = { viewModel.setIrBrand(it) },
                        onToggleFallback = { viewModel.setIrFallback(it) },
                        onStartScan = { viewModel.startIrBrandScan() },
                        onStopScan = { confirm -> viewModel.stopIrBrandScan(confirm) },
                        onSendIr = { btn ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendIrButton(btn)
                        },
                        onSendCustomIr = { code ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.sendCustomIr(code)
                        },
                        onImportFlipper = { content ->
                            viewModel.importFlipperFile(content)
                        },
                        onSendFlipperEntry = { entry ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendFlipperEntry(entry)
                        }
                    )

                    RemoteTab.IRDB -> IrDbBrowserPanel(
                        hasIr = viewModel.hasIrBlaster,
                        categories = irdbCategories,
                        results = irdbResults,
                        selectedItem = selectedIrDbItem,
                        selectedButtons = selectedIrDbButtons,
                        onSearch = { q, cat -> viewModel.searchIrDb(q, cat) },
                        onSelectItem = { item -> viewModel.openIrDbItem(item) },
                        onCloseItem = { viewModel.closeIrDbItem() },
                        onSendEntry = { entry ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.sendFlipperEntry(entry)
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
        is ConnectionState.Error -> Color(0xFFFF6B6B) to "Sin conexión"
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
                Icon(Icons.Rounded.Settings, contentDescription = "Configuración", modifier = Modifier.size(16.dp))
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
                Icon(Icons.Rounded.Menu, contentDescription = "Menú")
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
                    "Deslizá para mover • Tocar = OK",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Doble toque = Atrás • Mantener = Menú",
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
private fun IrUniversalPanel(
    hasIr: Boolean,
    selectedBrand: TvBrand,
    irFallback: Boolean,
    irScanBrand: TvBrand?,
    flipperButtons: List<FlipperIrEntry>,
    savedCustomCode: String,
    onSelectBrand: (TvBrand) -> Unit,
    onToggleFallback: (Boolean) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: (Boolean) -> Unit,
    onSendIr: (IrButton) -> Unit,
    onSendCustomIr: (String) -> Unit,
    onImportFlipper: (String) -> Unit,
    onSendFlipperEntry: (FlipperIrEntry) -> Unit
) {
    var brandMenuOpen by remember { mutableStateOf(false) }
    var customCode by remember { mutableStateOf(savedCustomCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hasIr) {
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Este celular no tiene emisor IR de hardware. Los botones IR solo funcionarán en teléfonos con blaster IR (Xiaomi, Poco, Redmi, Huawei, etc.).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { brandMenuOpen = true }) {
                    Icon(Icons.Rounded.SettingsRemote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        selectedBrand.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = brandMenuOpen,
                    onDismissRequest = { brandMenuOpen = false }
                ) {
                    TvBrand.entries.forEach { brand ->
                        DropdownMenuItem(
                            text = { Text(brand.displayName) },
                            onClick = {
                                onSelectBrand(brand)
                                brandMenuOpen = false
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto IR", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(6.dp))
                Switch(checked = irFallback, onCheckedChange = onToggleFallback)
            }
        }

        // Auto-detect TV brand scanner
        if (irScanBrand != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Probando: ${irScanBrand.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { onStopScan(true) }) {
                        Text("¡Funcionó!")
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Auto-detectar TV (probar Power en todas las marcas)")
            }
        }

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
                        onClick = { onSendIr(btn) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(btn.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        if (flipperButtons.isNotEmpty()) {
            Text(
                "Botones importados (.ir)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            flipperButtons.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { entry ->
                        FilledTonalButton(
                            onClick = { onSendFlipperEntry(entry) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = customCode,
            onValueChange = { customCode = it },
            label = { Text("Pronto Hex o archivo .ir de Flipper Zero") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                enabled = customCode.isNotBlank(),
                onClick = { onSendCustomIr(customCode) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Transmitir 1")
            }
            FilledTonalButton(
                enabled = customCode.isNotBlank(),
                onClick = { onImportFlipper(customCode) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Importar .ir")
            }
        }
    }
}

@Composable
private fun IrDbBrowserPanel(
    hasIr: Boolean,
    categories: List<String>,
    results: List<IrDbItem>,
    selectedItem: IrDbItem?,
    selectedButtons: List<FlipperIrEntry>,
    onSearch: (String, String?) -> Unit,
    onSelectItem: (IrDbItem) -> Unit,
    onCloseItem: () -> Unit,
    onSendEntry: (FlipperIrEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!hasIr) {
            Text(
                "Sin emisor IR en este celular (podés explorar los 1.957 controles igualmente).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (selectedItem != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(onClick = onCloseItem) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selectedItem.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${selectedItem.category} • ${selectedButtons.size} botones",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedButtons.isEmpty()) {
                Text(
                    "No se encontraron comandos compatibles en este archivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedButtons) { entry ->
                        FilledTonalButton(
                            onClick = { onSendEntry(entry) },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp)
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
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it, selectedCategory)
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                label = { Text("Buscar en 1.957 controles (RCA, AC, Epson, LED...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = {
                            selectedCategory = null
                            onSearch(query, null)
                        },
                        label = { Text("Todos") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            val next = if (selectedCategory == cat) null else cat
                            selectedCategory = next
                            onSearch(query, next)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { item ->
                    ListItem(
                        headlineContent = {
                            Text(item.displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = { Text(item.category) },
                        leadingContent = { Icon(Icons.Rounded.SettingsRemote, contentDescription = null) },
                        modifier = Modifier.clickable { onSelectItem(item) }
                    )
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
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
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
