package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.entities.AlertEntity
import com.example.data.database.entities.AnalyticsLogEntity
import com.example.data.database.entities.DeviceEntity
import com.example.data.database.entities.SettingsEntity
import com.example.data.repository.LoadManagerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class valTab { HOME, DEVICES, ANALYTICS, ALERTS }

class LoadManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LoadManagerRepository(application)

    // Current Navigation Tab
    val currentTab = MutableStateFlow(valTab.HOME)

    // Exposed Flows from repository
    val devices: StateFlow<List<DeviceEntity>> = repository.allDevicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<AlertEntity>> = repository.allAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<AnalyticsLogEntity>> = repository.allLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity> = repository.settingsFlow
        .map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    // Selected Device in Device Manager tab
    val selectedDeviceId = MutableStateFlow<Int?>(null)

    // Clock paint states
    val isEditClockMode = MutableStateFlow(false)

    // Dialog & overlay trackers
    val showMutualExclusionDialog = MutableStateFlow(false)
    val showManualOverrideDialog = MutableStateFlow(false)
    var pendingOverrideDevice: DeviceEntity? = null

    // Dual-bracket cost warning Dialog states
    val showMaxCostDeactivationDialog = MutableStateFlow(false)
    val pendingPlanToActivate = MutableStateFlow<String?>(null) // "scheduled" or "seconds"

    // Manual mode transition Warning Dialog states
    val showManualModeTransitionWarningDialog = MutableStateFlow(false)
    var pendingDeviceForManualTransition: DeviceEntity? = null

    // New Device protection and seconds plan check dialog states
    val showProtectionBlockedDialog = MutableStateFlow(false)
    val protectionBlockedMessage = MutableStateFlow("")

    val showStateChangeConfirmDialog = MutableStateFlow(false)
    val stateChangeConfirmMessage = MutableStateFlow("")
    var deviceToToggle: DeviceEntity? = null

    val showSecondsPlanBlockedDialog = MutableStateFlow(false)

    // Automatic ESP service exploration flow states
    val isScanningEsp = MutableStateFlow(false)
    val scanResult = MutableStateFlow<String?>(null) // "success", "failed", or null

    fun startAutomaticEspDiscovery() {
        viewModelScope.launch {
            isScanningEsp.value = true
            scanResult.value = null
            
            val (success, foundIp) = repository.autoDiscoverEsp()
            
            isScanningEsp.value = false
            if (success && foundIp != null) {
                scanResult.value = "success"
            } else {
                scanResult.value = "failed"
            }
        }
    }

    fun resetScanResult() {
        scanResult.value = null
    }

    init {
        // Automatically select the first device initially once loaded
        viewModelScope.launch {
            devices.firstOrNull { it.isNotEmpty() }?.let { list ->
                if (selectedDeviceId.value == null) {
                    selectedDeviceId.value = list.firstOrNull()?.id
                }
            }
        }
    }

    fun selectTab(tab: valTab) {
        currentTab.value = tab
    }

    fun selectDevice(id: Int) {
        selectedDeviceId.value = id
        // Reset edit mode when device switches
        isEditClockMode.value = false
    }

    fun getRemainingProtectionTime(device: DeviceEntity): Int {
        if (device.deviceType == "انارة") return 0
        val lastChange = device.lastStateChangeTime
        if (lastChange == 0L) return 0
        val elapsedSec = (System.currentTimeMillis() - lastChange) / 1000
        val limit = if (device.isOn) device.timeToOn else device.timeToOff
        val remaining = limit - elapsedSec
        return if (remaining > 0) remaining.toInt() else 0
    }

    // Toggle Device Switch
    fun toggleDeviceSwitch(device: DeviceEntity) {
        val remaining = getRemainingProtectionTime(device)
        if (remaining > 0) {
            val actionWord = if (device.isOn) "الاطفاء" else "التشغيل"
            protectionBlockedMessage.value = "لا يمكن $actionWord الا بعد مرور المدة المتبقية لحماية التجهيزة ($remaining ثانية)"
            showProtectionBlockedDialog.value = true
        } else {
            val actionWord = if (device.isOn) "اطفاء" else "تشغيل"
            stateChangeConfirmMessage.value = "هل انت متاكد انك تريد $actionWord الجهاز؟"
            deviceToToggle = device
            showStateChangeConfirmDialog.value = true
        }
    }

    fun executeToggleDevice(device: DeviceEntity) {
        viewModelScope.launch {
            val isOnlyMaxCostActiveAndNotReached = (device.maxCostStatus == 1) &&
                    (device.scheduledHoursStatus != 1) &&
                    (device.secondsPlanStatus != 1) &&
                    (device.costSyp < device.maxCostLimit)

            val hasActiveAutoPlan = !device.isManualMode && (
                    device.maxCostStatus == 1 ||
                    device.scheduledHoursStatus == 1 ||
                    device.secondsPlanStatus == 1
            ) && !isOnlyMaxCostActiveAndNotReached

            val nextState = !device.isOn
            val updatedDevice = device.copy(
                isOn = nextState,
                isManualOn = nextState,
                lastStateChangeTime = System.currentTimeMillis()
            )

            if (hasActiveAutoPlan) {
                val finalDevice = updatedDevice.copy(
                    isManualMode = true,
                    maxCostStatus = if (device.maxCostStatus == 1) 2 else device.maxCostStatus,
                    scheduledHoursStatus = if (device.scheduledHoursStatus == 1) 2 else device.scheduledHoursStatus,
                    secondsPlanStatus = if (device.secondsPlanStatus == 1) 2 else device.secondsPlanStatus
                )
                repository.updateDevice(finalDevice)
                
                repository.insertAlert(
                    AlertEntity(
                        deviceId = device.id,
                        deviceName = device.name,
                        timestamp = System.currentTimeMillis(),
                        title = "انتقال للوضع اليدوي",
                        message = "تم تفعيل التحكم اليدوي وإيقاف خطط الأتمتة مؤقتًا لتغيير حالة جهاز [${device.name}] يدويًا.",
                        cause = "تجاوز يدوي من المستخدم",
                        severity = "info"
                    )
                )
            } else {
                repository.updateDevice(updatedDevice)
            }
        }
    }

    // Confirm transition to manual mode (أوافق)
    fun confirmManualOverride() {
        val target = pendingOverrideDevice ?: return
        viewModelScope.launch {
            val nextState = !target.isOn
            repository.updateDevice(
                target.copy(
                    isOn = nextState,
                    isManualOn = nextState,
                    isManualMode = true,
                    // Pause active plans during transition
                    maxCostStatus = if (target.maxCostStatus == 1) 2 else target.maxCostStatus,
                    scheduledHoursStatus = if (target.scheduledHoursStatus == 1) 2 else target.scheduledHoursStatus,
                    secondsPlanStatus = if (target.secondsPlanStatus == 1) 2 else target.secondsPlanStatus
                )
            )
            // Insert audit info alert
            repository.insertAlert(
                AlertEntity(
                    deviceId = target.id,
                    deviceName = target.name,
                    timestamp = System.currentTimeMillis(),
                    title = "انتقال للوضع اليدوي",
                    message = "تم تفعيل التحكم اليدوي وإيقاف خطط الأتمتة مؤقتًا لتغيير حالة جهاز [${target.name}] يدويًا.",
                    cause = "تجاوز يدوي من المستخدم",
                    severity = "info"
                )
            )
            cancelManualOverride()
        }
    }

    fun cancelManualOverride() {
        showManualOverrideDialog.value = false
        pendingOverrideDevice = null
    }

    // Set Mode Manual/Auto
    fun setDeviceManualMode(device: DeviceEntity, isManual: Boolean) {
        viewModelScope.launch {
            repository.updateDevice(device.copy(isManualMode = isManual))
        }
    }

    // AUTO MODE 1: Max Cost Limit controls
    fun updatePendingMaxCostLimit(device: DeviceEntity, limitSyp: Double) {
        viewModelScope.launch {
            repository.updateDevice(device.copy(pendingMaxCostLimit = limitSyp))
        }
    }

    fun startMaxCostLimit(device: DeviceEntity, limitSyp: Double) {
        viewModelScope.launch {
            repository.updateDevice(
                device.copy(
                    pendingMaxCostLimit = limitSyp,
                    maxCostLimit = limitSyp,
                    isMaxCostActive = true,
                    maxCostStatus = 1 // Running
                )
            )
        }
    }

    fun pauseMaxCostLimit(device: DeviceEntity) {
        viewModelScope.launch {
            val nextStatus = if (device.maxCostStatus == 2) 1 else 2
            repository.updateDevice(device.copy(maxCostStatus = nextStatus))
        }
    }

    fun cancelMaxCostLimit(device: DeviceEntity) {
        viewModelScope.launch {
            repository.updateDevice(
                device.copy(
                    isMaxCostActive = false,
                    maxCostStatus = 0 // Stopped (preserve current isOn state)
                )
            )
        }
    }

    // AUTO MODE 2: Scheduled Hours controls
    fun startScheduledHoursPlan(device: DeviceEntity) {
        viewModelScope.launch {
            // Check mutual exclusion with seconds plan
            if (device.secondsPlanStatus == 1) {
                showMutualExclusionDialog.value = true
                return@launch
            }
            // Copy pending slots to active selected slots
            val activeSlotsCsv = device.pendingSlotsCsv
            val slots = getSlotsSet(activeSlotsCsv)
            
            // Determine initial isOn state based on current hour/minute slot to prevent relay flickering
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)
            val minutesSinceMidnight = hour * 60 + minute
            val slotIndex = minutesSinceMidnight / 5

            val isCurrentSlotActive = slots.contains(slotIndex)

            repository.updateDevice(
                device.copy(
                    selectedSlotsCsv = activeSlotsCsv,
                    scheduledHoursStatus = 1, // Running
                    isOn = isCurrentSlotActive
                )
            )
        }
    }

    fun pauseScheduledHoursPlan(device: DeviceEntity) {
        viewModelScope.launch {
            val nextStatus = if (device.scheduledHoursStatus == 2) 1 else 2
            repository.updateDevice(device.copy(scheduledHoursStatus = nextStatus))
        }
    }

    fun cancelScheduledHoursPlan(device: DeviceEntity) {
        viewModelScope.launch {
            repository.updateDevice(
                device.copy(
                    scheduledHoursStatus = 0 // Stopped (preserve current isOn state)
                )
            )
        }
    }

    // AUTO MODE 3: Seconds countdown controls
    fun startSecondsPlan(device: DeviceEntity) {
        viewModelScope.launch {
            // Check mutual exclusion with scheduled hours
            if (device.scheduledHoursStatus == 1) {
                showMutualExclusionDialog.value = true
                return@launch
            }
            // Copy pending to running settings
            val actOn = device.pendingSecondsOn
            val actOff = device.pendingSecondsOff
            repository.updateDevice(
                device.copy(
                    secondsOn = actOn,
                    secondsOff = actOff,
                    secondsPlanStatus = 1, // Running
                    currentSecondsPhaseIsOn = true,
                    secondsTimeLeftInPhase = actOn,
                    isOn = true
                )
            )
        }
    }

    fun pauseSecondsPlan(device: DeviceEntity) {
        viewModelScope.launch {
            val nextStatus = if (device.secondsPlanStatus == 2) 1 else 2
            repository.updateDevice(device.copy(secondsPlanStatus = nextStatus))
        }
    }

    fun cancelSecondsPlan(device: DeviceEntity) {
        viewModelScope.launch {
            repository.updateDevice(
                device.copy(
                    secondsPlanStatus = 0 // Stopped (preserve current isOn state and timing config)
                )
            )
        }
    }

    // Update draft limits for Seconds plan
    fun updatePendingSecondsPlanTiming(device: DeviceEntity, secondsOn: Int, secondsOff: Int) {
        viewModelScope.launch {
            repository.updateDevice(
                device.copy(
                    pendingSecondsOn = secondsOn,
                    pendingSecondsOff = secondsOff
                )
            )
        }
    }

    // Add Slots from drawing on hours clock (updates the draft/pending slots)
    fun updatePendingSlots(device: DeviceEntity, slots: Set<Int>) {
        viewModelScope.launch {
            repository.updateDevice(
                device.copy(
                    pendingSlotsCsv = repository.makeCsvFromSlots(slots)
                )
            )
        }
    }

    fun makeCsvFromSlots(slots: Set<Int>): String {
        return repository.makeCsvFromSlots(slots)
    }

    // Remove specific time ranges from pending/draft slots
    fun deleteTimeRange(device: DeviceEntity, startSlot: Int, endSlot: Int) {
        viewModelScope.launch {
            val originalSlots = repository.getSlotsSet(device.pendingSlotsCsv).toMutableSet()
            for (slot in startSlot..endSlot) {
                originalSlots.remove(slot)
            }
            repository.updateDevice(
                device.copy(
                    pendingSlotsCsv = repository.makeCsvFromSlots(originalSlots)
                )
            )
        }
    }

    // CRUD Device Actions
    fun createNewDevice(name: String, deviceType: String = "انارة", timeToOn: Int = 0, timeToOff: Int = 0) {
        viewModelScope.launch {
            val newDevice = DeviceEntity(
                name = name,
                deviceType = deviceType,
                timeToOn = timeToOn,
                timeToOff = timeToOff,
                activePower = 0.0,
                voltage = 220.0
            )
            val generatedId = repository.insertDevice(newDevice)
            selectedDeviceId.value = generatedId.toInt()
        }
    }

    fun deleteCurrentDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            // Switch selection
            val remaining = devices.value.filter { it.id != device.id }
            if (remaining.isNotEmpty()) {
                selectedDeviceId.value = remaining.first().id
            } else {
                selectedDeviceId.value = null
            }
        }
    }

    fun updateSystemSettings(
        ip: String,
        simulatorEnabled: Boolean,
        tariff: Double,
        bracket1Limit: Double,
        bracket1Price: Double,
        bracket2Price: Double
    ) {
        viewModelScope.launch {
            repository.updateSettings(
                SettingsEntity(
                    espIpAddress = ip,
                    isSimulatorEnabled = simulatorEnabled,
                    tariffPerKWh = tariff,
                    tariffBracket1Limit = bracket1Limit,
                    tariffBracket1Price = bracket1Price,
                    tariffBracket2Price = bracket2Price
                )
            )
        }
    }

    // Safety constraint checks for Max Cost Limits
    fun requestStartScheduledHoursPlan(device: DeviceEntity) {
        val isCostActive = device.isMaxCostActive && (device.maxCostStatus == 1)
        if (isCostActive) {
            pendingPlanToActivate.value = "scheduled"
            showMaxCostDeactivationDialog.value = true
        } else {
            startScheduledHoursPlan(device)
        }
    }

    fun requestStartSecondsPlan(device: DeviceEntity) {
        if (device.deviceType != "انارة") {
            showSecondsPlanBlockedDialog.value = true
            return
        }
        val isCostActive = device.isMaxCostActive && (device.maxCostStatus == 1)
        if (isCostActive) {
            pendingPlanToActivate.value = "seconds"
            showMaxCostDeactivationDialog.value = true
        } else {
            startSecondsPlan(device)
        }
    }

    fun confirmMaxCostDeactivationAndStartPlan(device: DeviceEntity) {
        val plan = pendingPlanToActivate.value ?: return
        viewModelScope.launch {
            val clearedDevice = device.copy(
                isMaxCostActive = false,
                maxCostStatus = 0
            )
            repository.updateDevice(clearedDevice)
            if (plan == "scheduled") {
                startScheduledHoursPlan(clearedDevice)
            } else if (plan == "seconds") {
                startSecondsPlan(clearedDevice)
            }
            showMaxCostDeactivationDialog.value = false
            pendingPlanToActivate.value = null
        }
    }

    fun cancelMaxCostDeactivation() {
        showMaxCostDeactivationDialog.value = false
        pendingPlanToActivate.value = null
    }

    // Safety checks for transitioning to Manual Mode
    fun requestSetDeviceManualMode(device: DeviceEntity, isManual: Boolean) {
        if (isManual) {
            val anyAutoPlanActive = device.maxCostStatus == 1 ||
                    device.scheduledHoursStatus == 1 ||
                    device.secondsPlanStatus == 1
            if (anyAutoPlanActive) {
                pendingDeviceForManualTransition = device
                showManualModeTransitionWarningDialog.value = true
                return
            }
        }
        viewModelScope.launch {
            repository.updateDevice(device.copy(isManualMode = isManual))
        }
    }

    fun confirmManualModeTransition() {
        val device = pendingDeviceForManualTransition ?: return
        viewModelScope.launch {
            repository.updateDevice(
                device.copy(
                    isManualMode = true,
                    maxCostStatus = if (device.maxCostStatus == 1) 2 else device.maxCostStatus,
                    scheduledHoursStatus = if (device.scheduledHoursStatus == 1) 2 else device.scheduledHoursStatus,
                    secondsPlanStatus = if (device.secondsPlanStatus == 1) 2 else device.secondsPlanStatus
                )
            )
            showManualModeTransitionWarningDialog.value = false
            pendingDeviceForManualTransition = null
        }
    }

    fun cancelManualModeTransition() {
        showManualModeTransitionWarningDialog.value = false
        pendingDeviceForManualTransition = null
    }

    // Clock slot conversion helper
    fun getIntervalsList(selectedSlotsCsv: String): List<TimeRangeModel> {
        val slots = repository.getSlotsSet(selectedSlotsCsv).sorted()
        if (slots.isEmpty()) return emptyList()

        val list = mutableListOf<TimeRangeModel>()
        var start = slots[0]
        var prev = slots[0]

        for (i in 1 until slots.size) {
            val curr = slots[i]
            if (curr == prev + 1) {
                prev = curr
            } else {
                list.add(createRangeModel(start, prev))
                start = curr
                prev = curr
            }
        }
        list.add(createRangeModel(start, prev))
        return list
    }

    private fun createRangeModel(start: Int, end: Int): TimeRangeModel {
        val startTotalMinutes = start * 5
        val startH = startTotalMinutes / 60
        val startM = startTotalMinutes % 60

        val endTotalMinutes = (end + 1) * 5
        val endH = endTotalMinutes / 60
        val endM = endTotalMinutes % 60

        val startStr = String.format("%02d:%02d", startH, startM)
        val endStr = String.format("%02d:%02d", endH, endM)
        return TimeRangeModel(startSlot = start, endSlot = end, text = "$startStr → $endStr")
    }

    fun getSlotsSet(csv: String): Set<Int> {
        if (csv.isEmpty()) return emptySet()
        return csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}

data class TimeRangeModel(
    val startSlot: Int,
    val endSlot: Int,
    val text: String
)
