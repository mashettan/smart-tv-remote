package com.smartremote.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.smartremote.app.model.RemoteKey
import com.smartremote.app.ui.theme.DangerRed
import com.smartremote.app.ui.theme.SurfaceDarkElevated
import com.smartremote.app.ui.theme.TextPrimary

@Composable
fun TopUtilityBar(
    onKeyPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticClick = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back
        ControlButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Back",
            onClick = {
                hapticClick()
                onKeyPress(RemoteKey.BACK)
            }
        )

        // Home
        ControlButton(
            icon = Icons.Default.Home,
            contentDescription = "Home",
            onClick = {
                hapticClick()
                onKeyPress(RemoteKey.HOME)
            }
        )

        // Play / Pause
        ControlButton(
            icon = Icons.Default.PlayArrow,
            contentDescription = "Play/Pause",
            onClick = {
                hapticClick()
                onKeyPress(RemoteKey.PLAY_PAUSE)
            }
        )

        // Power (Red Accent)
        ControlButton(
            icon = Icons.Default.PowerSettingsNew,
            contentDescription = "Power",
            tint = DangerRed,
            onClick = {
                hapticClick()
                onKeyPress(RemoteKey.POWER)
            }
        )
    }
}

@Composable
fun VolumeControlBar(
    onKeyPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticClick = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDarkElevated)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vol Down
        ControlButton(
            icon = Icons.Default.VolumeDown,
            contentDescription = "Volume Down",
            onClick = {
                hapticClick()
                onKeyPress(RemoteKey.VOL_DOWN)
            }
        )

        // Mute
        ControlButton(
            icon = Icons.Default.VolumeMute,
            contentDescription = "Mute",
            onClick = {
                hapticClick()
                onKeyPress(RemoteKey.MUTE)
            }
        )

        // Vol Up
        ControlButton(
            icon = Icons.Default.VolumeUp,
            contentDescription = "Volume Up",
            onClick = {
                hapticClick()
                onKeyPress(RemoteKey.VOL_UP)
            }
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(SurfaceDarkElevated)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
