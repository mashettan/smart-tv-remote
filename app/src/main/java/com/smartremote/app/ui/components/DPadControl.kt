package com.smartremote.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartremote.app.model.RemoteKey
import com.smartremote.app.ui.theme.PrimaryAccent
import com.smartremote.app.ui.theme.SurfaceDark
import com.smartremote.app.ui.theme.SurfaceDarkElevated
import com.smartremote.app.ui.theme.SurfaceDarkHover
import com.smartremote.app.ui.theme.TextPrimary

@Composable
fun DPadControl(
    onKeyPress: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticClick = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(SurfaceDarkHover, SurfaceDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // UP
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    hapticClick()
                    onKeyPress(RemoteKey.UP)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // DOWN
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-8).dp)
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    hapticClick()
                    onKeyPress(RemoteKey.DOWN)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // LEFT
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 8.dp)
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    hapticClick()
                    onKeyPress(RemoteKey.LEFT)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // RIGHT
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp)
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    hapticClick()
                    onKeyPress(RemoteKey.RIGHT)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Right",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        // CENTER OK BUTTON
        Box(
            modifier = Modifier
                .size(86.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryAccent, PrimaryAccent.copy(alpha = 0.85f))
                    )
                )
                .clickable {
                    hapticClick()
                    onKeyPress(RemoteKey.SELECT)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
