package com.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices

data class LocationPreset(
    val name: String,
    val lat: Double,
    val lng: Double
)

val PRESET_LOCATIONS = listOf(
    LocationPreset("上海陆家嘴金融中心", 31.2397, 121.4998),
    LocationPreset("北京中关村科技园", 39.9832, 116.3153),
    LocationPreset("深圳高新科技园南区", 22.5329, 113.9531),
    LocationPreset("杭州网易/阿里西溪园区", 30.2741, 120.0232),
    LocationPreset("成都天府软件园A区", 30.5398, 104.0682)
)

@Composable
fun LocationPickerModal(
    currentLat: Double,
    currentLng: Double,
    currentName: String,
    onConfirm: (name: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(currentName) }
    var latInput by remember { mutableStateOf(currentLat.toString()) }
    var lngInput by remember { mutableStateOf(currentLng.toString()) }

    var selectedPresetName by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置公司位置坐标") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("公司/工作地名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("纬度 (Latitude)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it },
                        label = { Text("经度 (Longitude)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                @SuppressLint("MissingPermission")
                OutlinedButton(
                    onClick = {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                latInput = loc.latitude.toString()
                                lngInput = loc.longitude.toString()
                                nameInput = "当前设备GPS点"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("读取当前设备GPS坐标")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "快捷选择常见科技园区预设:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(PRESET_LOCATIONS) { preset ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPresetName == preset.name)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    selectedPresetName = preset.name
                                    nameInput = preset.name
                                    latInput = preset.lat.toString()
                                    lngInput = preset.lng.toString()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(preset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${preset.lat}, ${preset.lng}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latInput.toDoubleOrNull() ?: currentLat
                    val lng = lngInput.toDoubleOrNull() ?: currentLng
                    onConfirm(nameInput, lat, lng)
                    onDismiss()
                }
            ) {
                Text("保存设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
