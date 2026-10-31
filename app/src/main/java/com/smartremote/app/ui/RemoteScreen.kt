package com.smartremote.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartremote.app.ui.components.AppShortcutDock
import com.smartremote.app.ui.components.DPadControl
import com.smartremote.app.ui.components.DeviceSelectionSheet
import com.smartremote.app.ui.components.ManualIpDialog
import com.smartremote.app.ui.components.TopUtilityBar
import com.smartremote.app.ui.components.VolumeControlBar
import com.smartremote.app.ui.theme.BgDark
import com.smartremote.app.ui.theme.PrimaryAccent
import com.smartremote.app.ui.theme.SuccessGreen
import com.smartremote.app.ui.theme.SurfaceDark
import com.smartremote.app.ui.theme.SurfaceDarkElevated
import com.smartremote.app.ui.theme.TextMuted
import com.smartremote.app.ui.theme.TextPrimary
import com.smartremote.app.ui.theme.TextSecondary
import com.smartremote.app.viewmodel.RemoteViewModel

@Composable
fun RemoteScreen(viewModel: RemoteViewModel) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isDevicePickerOpen by viewModel.isDevicePickerOpen.collectAsState()
    val isManualIpDialogOpen by viewModel.isManualIpDialogOpen.collectAsState()

    Scaffold(
        containerColor = BgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER: TV SELECTOR PILL & STATUS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TV Connection Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(SurfaceDarkElevated)
                        .clickable { viewModel.openDevicePicker() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (selectedDevice != null) SuccessGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = selectedDevice?.displayTitle ?: "Select Smart TV",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select TV",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status message indicator
                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage ?: "",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            // ESSENTIAL CONTROLS BAR: BACK, HOME, PLAY/PAUSE, POWER
            TopUtilityBar(
                onKeyPress = { key -> viewModel.sendKey(key) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // D-PAD CONTROLLER (TACTILE DIRECTIONAL + OK)
            DPadControl(
                onKeyPress = { key -> viewModel.sendKey(key) }
            )

            // VOLUME CONTROLS (VOL -, MUTE, VOL +)
            VolumeControlBar(
                onKeyPress = { key -> viewModel.sendKey(key) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // APP SHORTCUTS DOCK (YOUTUBE, NETFLIX, PRIME VIDEO, SPOTIFY)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "QUICK SHORTCUTS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AppShortcutDock(
                    onLaunchApp = { app -> viewModel.launchApp(app) }
                )
            }
        }
    }

    // BOTTOM SHEET FOR NEARBY TV DISCOVERY
    if (isDevicePickerOpen) {
        DeviceSelectionSheet(
            devices = discoveredDevices,
            selectedDevice = selectedDevice,
            isScanning = isScanning,
            onSelectDevice = { device -> viewModel.selectDevice(device) },
            onScan = { viewModel.scanForDevices() },
            onOpenManualDialog = {
                viewModel.closeDevicePicker()
                viewModel.openManualIpDialog()
            },
            onDismiss = { viewModel.closeDevicePicker() }
        )
    }

    // DIALOG FOR MANUAL IP ENTRY
    if (isManualIpDialogOpen) {
        ManualIpDialog(
            onAddDevice = { name, ip, brand ->
                viewModel.addManualDevice(name, ip, brand)
            },
            onDismiss = { viewModel.closeManualIpDialog() }
        )
    }
}
