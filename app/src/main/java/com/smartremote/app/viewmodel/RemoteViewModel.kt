package com.smartremote.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartremote.app.controller.TvController
import com.smartremote.app.discovery.TvDiscoveryManager
import com.smartremote.app.model.AppShortcut
import com.smartremote.app.model.RemoteKey
import com.smartremote.app.model.TvBrand
import com.smartremote.app.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val discoveryManager = TvDiscoveryManager(application)
    private var activeController: TvController? = null

    private val _selectedDevice = MutableStateFlow<TvDevice?>(null)
    val selectedDevice: StateFlow<TvDevice?> = _selectedDevice.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("Scan for your TV or enter IP to connect")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isDevicePickerOpen = MutableStateFlow(false)
    val isDevicePickerOpen: StateFlow<Boolean> = _isDevicePickerOpen.asStateFlow()

    private val _isManualIpDialogOpen = MutableStateFlow(false)
    val isManualIpDialogOpen: StateFlow<Boolean> = _isManualIpDialogOpen.asStateFlow()

    init {
        // Auto scan on startup
        scanForDevices()
    }

    fun scanForDevices() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _statusMessage.value = "Scanning local Wi-Fi for Smart TVs..."

            try {
                val devices = discoveryManager.discoverDevices()
                _discoveredDevices.value = devices

                if (devices.isNotEmpty()) {
                    if (_selectedDevice.value == null) {
                        selectDevice(devices.first())
                    }
                    _statusMessage.value = "Found ${devices.size} TV(s) on Wi-Fi"
                } else {
                    _statusMessage.value = "No TVs auto-detected. Tap + to add IP directly."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Scan error: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun selectDevice(device: TvDevice) {
        viewModelScope.launch {
            activeController?.disconnect()
            activeController = TvController.create(device)
            _selectedDevice.value = device
            _isDevicePickerOpen.value = false
            _statusMessage.value = "Connected to ${device.displayTitle}"
        }
    }

    fun addManualDevice(name: String, ip: String, brand: TvBrand) {
        val defaultPort = when (brand) {
            TvBrand.ROKU -> 8060
            TvBrand.ANDROID_TV -> 8008
            TvBrand.LG_WEBOS -> 3000
            TvBrand.SAMSUNG_TIZEN -> 8001
            TvBrand.UNIVERSAL_DIAL -> 8008
        }
        val device = TvDevice(
            name = name.ifBlank { "${brand.displayName} ($ip)" },
            ipAddress = ip.trim(),
            port = defaultPort,
            brand = brand
        )
        val list = _discoveredDevices.value.toMutableList()
        list.removeAll { it.ipAddress == device.ipAddress }
        list.add(0, device)
        _discoveredDevices.value = list
        selectDevice(device)
        _isManualIpDialogOpen.value = false
    }

    fun sendKey(key: RemoteKey) {
        val controller = activeController
        if (controller == null) {
            _statusMessage.value = "Please select or add a TV first"
            _isDevicePickerOpen.value = true
            return
        }

        viewModelScope.launch {
            val result = controller.sendKey(key)
            result.fold(
                onSuccess = {
                    _statusMessage.value = "Sent: ${key.name}"
                },
                onFailure = { error ->
                    _statusMessage.value = "Error: ${error.message}"
                }
            )
        }
    }

    fun launchApp(app: AppShortcut) {
        val controller = activeController
        if (controller == null) {
            _statusMessage.value = "Please select or add a TV first"
            _isDevicePickerOpen.value = true
            return
        }

        viewModelScope.launch {
            _statusMessage.value = "Launching ${app.displayName}..."
            val result = controller.launchApp(app)
            result.fold(
                onSuccess = {
                    _statusMessage.value = "${app.displayName} launched!"
                },
                onFailure = { error ->
                    _statusMessage.value = "Launch error: ${error.message}"
                }
            )
        }
    }

    fun openDevicePicker() {
        _isDevicePickerOpen.value = true
    }

    fun closeDevicePicker() {
        _isDevicePickerOpen.value = false
    }

    fun openManualIpDialog() {
        _isManualIpDialogOpen.value = true
    }

    fun closeManualIpDialog() {
        _isManualIpDialogOpen.value = false
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            activeController?.disconnect()
        }
    }
}
