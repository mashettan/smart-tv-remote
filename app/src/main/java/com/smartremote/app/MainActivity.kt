package com.smartremote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.smartremote.app.ui.RemoteScreen
import com.smartremote.app.ui.theme.SmartTvRemoteTheme
import com.smartremote.app.viewmodel.RemoteViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartTvRemoteTheme {
                RemoteScreen(viewModel = viewModel)
            }
        }
    }
}
