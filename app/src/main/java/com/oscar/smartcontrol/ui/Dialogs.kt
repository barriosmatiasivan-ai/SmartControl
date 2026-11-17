package com.oscar.smartcontrol.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.oscar.smartcontrol.tv.TvDevice

@Composable
fun DevicePickerDialog(
    devices: List<TvDevice>,
    isScanning: Boolean,
    lastHost: String?,
    onScanAgain: () -> Unit,
    onSelectDevice: (TvDevice) -> Unit,
    onConnectManual: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var manualIp by remember { mutableStateOf(lastHost ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Conectar a TV")
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onScanAgain) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Buscar")
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (devices.isEmpty()) {
                    Text(
                        text = if (isScanning) "Buscando Google TV / Android TV en la red..."
                        else "No se encontraron TVs automáticamente. Ingresá la IP manualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(devices) { device ->
                            ListItem(
                                headlineContent = { Text(device.name) },
                                supportingContent = { Text(device.host) },
                                leadingContent = {
                                    Icon(Icons.Rounded.Tv, contentDescription = null)
                                },
                                modifier = Modifier.clickable { onSelectDevice(device) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text("IP manual (ej. 192.168.1.50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = manualIp.isNotBlank(),
                    onClick = { onConnectManual(manualIp.trim(), true) }
                ) {
                    Text("Emparejar PIN")
                }
                TextButton(
                    enabled = manualIp.isNotBlank(),
                    onClick = { onConnectManual(manualIp.trim(), false) }
                ) {
                    Text("Conectar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun PairingPinDialog(
    host: String,
    onSubmitPin: (String) -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Código de emparejamiento") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Ingresá el código de 6 caracteres que aparece en la pantalla de tu TV ($host):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.uppercase() },
                    label = { Text("Código PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.length >= 4,
                onClick = { onSubmitPin(pin) }
            ) {
                Text("Emparejar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    )
}
