package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.entities.AlertEntity
import com.example.data.database.entities.AnalyticsLogEntity
import com.example.data.database.entities.DeviceEntity
import com.example.data.database.entities.SettingsEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class LoadManagerRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val deviceDao = db.deviceDao()
    private val analyticsLogDao = db.analyticsLogDao()
    private val alertDao = db.alertDao()
    private val settingsDao = db.settingsDao()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    // Exposed Flows
    val allDevicesFlow: Flow<List<DeviceEntity>> = deviceDao.getAllDevicesFlow()
    val allLogsFlow: Flow<List<AnalyticsLogEntity>> = analyticsLogDao.getAllLogsFlow()
    val allAlertsFlow: Flow<List<AlertEntity>> = alertDao.getAllAlertsFlow()
    val settingsFlow: Flow<SettingsEntity?> = settingsDao.getSettingsFlow()

    init {
        ioScope.launch {
            initDefaults()
            startSimulationLoop()
        }
    }

    private suspend fun initDefaults() {
        // Initialize settings if empty
        val currentSettings = settingsDao.getSettingsDirect()
        val tariff = currentSettings?.tariffPerKWh ?: 125.0
        if (currentSettings == null) {
            settingsDao.insertOrUpdateSettings(SettingsEntity())
        }

        // Initialize default devices if none exist
        val devices = deviceDao.getAllDevicesDirect()
        if (devices.isEmpty()) {
            val defaults = listOf(
                DeviceEntity(name = "سخان المياه", voltage = 220.0, current = 0.0, activePower = 0.0, energyWh = 45000.0, costSyp = 600.0 + (45.0 - 0.01) * 140000.0),
                DeviceEntity(name = "مكيف الصالون", voltage = 220.0, current = 0.0, activePower = 0.0, energyWh = 85000.0, costSyp = 600.0 + (85.0 - 0.01) * 140000.0),
                DeviceEntity(name = "الغسالة الكهربائية", voltage = 220.0, current = 0.0, activePower = 0.0, energyWh = 12000.0, costSyp = 600.0 + (12.0 - 0.01) * 140000.0),
                DeviceEntity(name = "البراد ليزري", isOn = true, voltage = 220.0, current = 1.1, activePower = 240.0, energyWh = 22000.0, costSyp = 600.0 + (22.0 - 0.01) * 140000.0)
            )
            for (d in defaults) {
                deviceDao.insertDevice(d)
            }
            
            // Add some alerts
            alertDao.insertAlert(
                AlertEntity(
                    deviceId = 1,
                    deviceName = "سخان المياه",
                    timestamp = System.currentTimeMillis() - 3600000,
                    title = "انقطاع مفاجئ",
                    message = "تم فصل السخان يدويًا لحماية الدارة من زيادة الحمولة الكهربائية.",
                    cause = "تجاوز أمبير الدارة الكلي المسموح",
                    severity = "warning"
                )
            )
            alertDao.insertAlert(
                AlertEntity(
                    deviceId = 0,
                    deviceName = "النظام الذكي",
                    timestamp = System.currentTimeMillis() - 7200000,
                    title = "ارتفاع الجهد الكهربائي",
                    message = "تخطى جهد الشبكة 245 فولت لمدة تزيد عن 10 ثوانٍ، تم اتخاذ الإجراءات الوقائية.",
                    cause = "عدم استقرار محطة التوليد المحلية",
                    severity = "danger"
                )
            )
        }

        // Prepopulate bimonthly analytics if none exist
        val logs = analyticsLogDao.getAllLogsFlow().first()
        if (logs.isEmpty()) {
            prepopulateAnalyticsData()
        }
    }

    private suspend fun prepopulateAnalyticsData() {
        val monthGroups = listOf(
            "كانون الثاني - شباط",
            "آذار - نيسان",
            "أيار - حزيران",
            "تموز - آب",
            "أيلول - تشرين الأول",
            "تشرين الثاني - كانون الأول"
        )
        // We will prepopulate stats for:
        // Total (deviceId = 0) and individual default devices (1, 2, 3, 4)
        val devices = deviceDao.getAllDevicesDirect()

        for (mgIndex in monthGroups.indices) {
            val mg = monthGroups[mgIndex]
            val totalDays = 60
            
            for (day in 1..totalDays) {
                // Device 1 (heater)
                val d1Kwh = Random.nextDouble(1.5, 5.0)
                // Device 2 (AC)
                val d2Kwh = if (mgIndex in listOf(0, 3, 4)) Random.nextDouble(4.0, 12.0) else Random.nextDouble(0.2, 1.5)
                // Device 3 (Washing Machine)
                val d3Kwh = Random.nextDouble(0.5, 2.2)
                // Device 4 (Fridge)
                val d4Kwh = Random.nextDouble(1.0, 2.5)

                val calcCostSyp = { k: Double ->
                    if (k <= 0.01) k * 60000.0 else 600.0 + (k - 0.01) * 140000.0
                }
                
                // Save logs for each device
                analyticsLogDao.insertLog(AnalyticsLogEntity(deviceId = 1, deviceName = "سخان المياه", timestamp = System.currentTimeMillis(), monthGroup = mg, dayOfMonthGroup = day, kwh = d1Kwh, costSyp = calcCostSyp(d1Kwh), avgCurrent = d1Kwh * 1000 / (220 * 8), avgPower = d1Kwh * 1000 / 8))
                analyticsLogDao.insertLog(AnalyticsLogEntity(deviceId = 2, deviceName = "مكيف الصالون", timestamp = System.currentTimeMillis(), monthGroup = mg, dayOfMonthGroup = day, kwh = d2Kwh, costSyp = calcCostSyp(d2Kwh), avgCurrent = d2Kwh * 1000 / (220 * 8), avgPower = d2Kwh * 1000 / 8))
                analyticsLogDao.insertLog(AnalyticsLogEntity(deviceId = 3, deviceName = "الغسالة الكهربائية", timestamp = System.currentTimeMillis(), monthGroup = mg, dayOfMonthGroup = day, kwh = d3Kwh, costSyp = calcCostSyp(d3Kwh), avgCurrent = d3Kwh * 1000 / (220 * 8), avgPower = d3Kwh * 1000 / 8))
                analyticsLogDao.insertLog(AnalyticsLogEntity(deviceId = 4, deviceName = "البراد ليزري", timestamp = System.currentTimeMillis(), monthGroup = mg, dayOfMonthGroup = day, kwh = d4Kwh, costSyp = calcCostSyp(d4Kwh), avgCurrent = d4Kwh * 1000 / (220 * 24), avgPower = d4Kwh * 1000 / 24))

                // Accumulate total (deviceId = 0)
                val totalKWh = d1Kwh + d2Kwh + d3Kwh + d4Kwh
                val alertMsg = if (day % 15 == 0) "تنبيه: سجلت الشبكة المنزلية اليوم ارتفاع مؤقت بالاستطاعة المغذية." else null

                analyticsLogDao.insertLog(AnalyticsLogEntity(
                    deviceId = 0,
                    deviceName = "الاستهلاك الكلي",
                    timestamp = System.currentTimeMillis(),
                    monthGroup = mg,
                    dayOfMonthGroup = day,
                    kwh = totalKWh,
                    costSyp = calcCostSyp(totalKWh),
                    avgCurrent = totalKWh * 1000 / (220 * 24),
                    avgPower = totalKWh * 1000 / 24,
                    alertMsg = alertMsg
                ))
            }
        }
    }

    @Volatile private var lastEspVoltage = 220.0
    @Volatile private var lastEspCurrent = 0.0
    @Volatile private var lastEspPower = 0.0
    @Volatile private var lastEspEnergyWh = 45000.0
    @Volatile private var lastEspFrequency = 50.0
    @Volatile private var lastEspPowerFactor = 1.0
    @Volatile var isEspConnected = false
    private var activeWebSocket: okhttp3.WebSocket? = null

    fun connectWebSocket(ip: String) {
        if (activeWebSocket != null) {
            try {
                activeWebSocket?.close(1000, "Reconnecting")
            } catch (e: Exception) {}
            activeWebSocket = null
        }
        val targetIp = if (ip.contains(":")) ip else "$ip:81"
        val request = Request.Builder()
            .url("ws://$targetIp")
            .build()
        
        activeWebSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                isEspConnected = true
                Log.d("ESP_WS", "WebSocket connected successfully to $ip")
                // Instantly sync database state to physical relay and tariff settings
                ioScope.launch {
                    val devices = deviceDao.getAllDevicesDirect()
                    for (device in devices) {
                        try {
                            val payload = JSONObject().apply {
                                put("action", "toggle_relay")
                                put("target_state", if (device.isOn) 1 else 0)
                            }.toString()
                            webSocket.send(payload)
                            Log.d("ESP_WS", "Synced physical relay to: ${device.isOn}")
                        } catch (e: Exception) {
                            Log.e("ESP_WS", "Failed to sync physical relay on connection: ${e.message}")
                        }
                    }

                    val settings = settingsDao.getSettingsDirect()
                    if (settings != null) {
                        try {
                            val tariffPayload = JSONObject().apply {
                                put("action", "set_tariff")
                                put("limit1", settings.tariffBracket1Limit)
                                put("price1", settings.tariffBracket1Price)
                                put("price2", settings.tariffBracket2Price)
                            }.toString()
                            webSocket.send(tariffPayload)
                            Log.d("ESP_WS", "Synced tariff to ESP: limit=${settings.tariffBracket1Limit}, price1=${settings.tariffBracket1Price}, price2=${settings.tariffBracket2Price}")
                        } catch (e: Exception) {
                            Log.e("ESP_WS", "Failed to sync tariff to ESP: ${e.message}")
                        }
                    }
                }
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "telemetry") {
                        lastEspVoltage = json.optDouble("volts", 220.0)
                        lastEspCurrent = json.optDouble("current", 0.0)
                        lastEspPower = json.optDouble("pwr_w", 0.0)
                        val energyKWh = json.optDouble("energy", 0.0)
                        lastEspEnergyWh = energyKWh * 1000.0
                        lastEspFrequency = json.optDouble("freq", 50.0)
                        lastEspPowerFactor = json.optDouble("pf", 1.0)
                    }
                } catch (e: Exception) {
                    Log.e("ESP_WS", "Error parsing WebSocket telemetry: ${e.message}")
                }
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                isEspConnected = false
                Log.e("ESP_WS", "WebSocket failure for $ip: ${t.message}")
                ioScope.launch {
                    delay(4000)
                    val settings = settingsDao.getSettingsDirect() ?: SettingsEntity()
                    if (!settings.isSimulatorEnabled) {
                        connectWebSocket(settings.espIpAddress)
                    }
                }
            }

            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                isEspConnected = false
            }

            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                isEspConnected = false
            }
        })
    }

    @Volatile private var lastDiscoveryTime = 0L

    private suspend fun performBackgroundAutoDiscovery(currentIp: String) {
        val now = System.currentTimeMillis()
        if (now - lastDiscoveryTime < 6000) return // Throttle background discovery to every 6 seconds
        lastDiscoveryTime = now

        withContext(Dispatchers.IO) {
            val candidates = mutableListOf<String>()
            if (currentIp.isNotEmpty() && currentIp != "192.168.4.1" && currentIp != "power-ranger-device.local") {
                candidates.add(currentIp)
            }
            candidates.add("power-ranger-device.local")
            candidates.add("192.168.4.1") // SoftAP mode
            
            // Also add local subnet if available
            val localIp = getLocalIpAddress()
            if (localIp != null) {
                val dotIndex = localIp.lastIndexOf('.')
                if (dotIndex != -1) {
                    val subnet = localIp.substring(0, dotIndex + 1)
                    // Quick ping common IPs inside subnet if needed, but let's stick to base candidates first
                    // so we don't block the loop
                }
            }

            for (ip in candidates) {
                try {
                    val socket = java.net.Socket()
                    // Fast check on port 81 socket
                    socket.connect(java.net.InetSocketAddress(ip, 81), 250)
                    socket.close()

                    // If we reach here, this IP is alive on port 81!
                    Log.d("ESP_AUTO", "Found active ESP8266 at candidate IP: $ip")
                    val settings = settingsDao.getSettingsDirect() ?: SettingsEntity()
                    if (settings.espIpAddress != ip) {
                        settingsDao.insertOrUpdateSettings(settings.copy(espIpAddress = ip))
                    }
                    connectWebSocket(ip)
                    break
                } catch (e: Exception) {
                    // Fail fast and try next
                }
            }
        }
    }

    private fun startSimulationLoop() {
        ioScope.launch {
            while (isActive) {
                try {
                    delay(1000) // Tick once per second
                    val currentSettings = settingsDao.getSettingsDirect() ?: SettingsEntity()
                    val devices = deviceDao.getAllDevicesDirect()

                    if (!currentSettings.isSimulatorEnabled && !isEspConnected) {
                        performBackgroundAutoDiscovery(currentSettings.espIpAddress)
                    }

                    for (device in devices) {
                        var updatedDevice = device

                        // 1. Fetch real or simulated sensor values
                        updatedDevice = if (currentSettings.isSimulatorEnabled) {
                            runSimulationSensor(updatedDevice, currentSettings)
                        } else {
                            runRealEspSensor(updatedDevice, currentSettings.espIpAddress, currentSettings)
                        }

                        // 2. Scheduled Hours Engine
                        updatedDevice = runScheduledHoursEngine(updatedDevice)

                        // 3. Seconds Plan Engine
                        updatedDevice = runSecondsPlanEngine(updatedDevice)

                        // 4. Max Cost Limit Engine (Highest priority constraint)
                        updatedDevice = runMaxCostEngine(updatedDevice)

                        // Save updates
                        if (updatedDevice != device) {
                            deviceDao.updateDevice(updatedDevice)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SimulationLoop", "Error in background tick: ${e.message}")
                }
            }
        }
    }

    private fun runSimulationSensor(device: DeviceEntity, currentSettings: SettingsEntity): DeviceEntity {
        if (!device.isOn) {
            return device.copy(
                voltage = 220.0 + Random.nextDouble(-1.5, 1.5),
                current = 0.0,
                activePower = 0.0,
                frequency = 50.0 + Random.nextDouble(-0.1, 0.1),
                powerFactor = 1.0
            )
        }

        // Get standard nominal active powers for simulation based on device name
        val nominalPower = when {
            device.name.contains("سخان") -> 1800.0
            device.name.contains("مكيف") -> 1400.0
            device.name.contains("غسال") -> 600.0
            device.name.contains("براد") -> 250.0
            else -> 400.0
        }

        val powerFactor = 0.9 + Random.nextDouble(-0.05, 0.05)
        val voltage = 220.0 + Random.nextDouble(-5.0, 5.0)
        val rawPower = nominalPower + Random.nextDouble(-20.0, 20.0)
        val current = rawPower / (voltage * powerFactor)

        // Energy increment per second in Wh
        val deltaWh = rawPower / 3600.0
        val newWh = device.energyWh + deltaWh
        
        // Multi-bracket billing logic: uses dynamic values sync'd from settings
        val kwh = newWh / 1000.0
        val limit1 = currentSettings.tariffBracket1Limit
        val price1 = currentSettings.tariffBracket1Price
        val price2 = currentSettings.tariffBracket2Price
        val calculatedCost = if (kwh <= limit1) {
            kwh * price1
        } else {
            (limit1 * price1) + ((kwh - limit1) * price2)
        }

        return device.copy(
            voltage = voltage,
            current = current,
            activePower = rawPower,
            energyWh = newWh,
            frequency = 50.0 + Random.nextDouble(-0.08, 0.08),
            powerFactor = powerFactor,
            costSyp = calculatedCost
        )
    }

    private suspend fun runRealEspSensor(device: DeviceEntity, ip: String, currentSettings: SettingsEntity): DeviceEntity {
        val v = if (isEspConnected && lastEspVoltage > 0) lastEspVoltage else 220.0
        val c = if (isEspConnected) lastEspCurrent else 0.0
        val p = if (isEspConnected) lastEspPower else 0.0
        val eWh = if (isEspConnected && lastEspEnergyWh > 0) lastEspEnergyWh else device.energyWh
        val freq = if (isEspConnected) lastEspFrequency else 50.0
        val pf = if (isEspConnected) lastEspPowerFactor else 1.0

        // Multi-bracket billing logic: uses dynamic values sync'd from settings
        val kwh = eWh / 1000.0
        val limit1 = currentSettings.tariffBracket1Limit
        val price1 = currentSettings.tariffBracket1Price
        val price2 = currentSettings.tariffBracket2Price
        val calculatedCost = if (kwh <= limit1) {
            kwh * price1
        } else {
            (limit1 * price1) + ((kwh - limit1) * price2)
        }

        return device.copy(
            voltage = v,
            current = c,
            activePower = p,
            energyWh = eWh,
            frequency = freq,
            powerFactor = pf,
            costSyp = calculatedCost
        )
    }

    private suspend fun runScheduledHoursEngine(device: DeviceEntity): DeviceEntity {
        if (device.scheduledHoursStatus != 1) return device // 1 is Running

        // Get current system time of day
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val minutesSinceMidnight = hour * 60 + minute
        val slotIndex = minutesSinceMidnight / 5

        val slots = getSlotsSet(device.selectedSlotsCsv)
        val shouldBeOn = slots.contains(slotIndex)

        return if (device.isOn != shouldBeOn) {
            // Send trigger to relay
            if (!settingsDao.getSettingsDirect()?.isSimulatorEnabled!!) {
                triggerEspRelay(shouldBeOn)
            }
            device.copy(isOn = shouldBeOn)
        } else {
            device
        }
    }

    private suspend fun runSecondsPlanEngine(device: DeviceEntity): DeviceEntity {
        if (device.secondsPlanStatus != 1) return device // 1 is Running

        // Ensure safe non-zero durations to prevent infinite freezing loops
        val secOn = device.secondsOn.coerceAtLeast(5)
        val secOff = device.secondsOff.coerceAtLeast(5)

        var timeLeft = device.secondsTimeLeftInPhase - 1
        var phaseIsOn = device.currentSecondsPhaseIsOn
        var activeOnState = device.isOn

        if (timeLeft <= 0) {
            // Toggle active phase
            phaseIsOn = !phaseIsOn
            timeLeft = if (phaseIsOn) secOn else secOff
            activeOnState = phaseIsOn
            
            // Trigger physical relay
            if (!settingsDao.getSettingsDirect()?.isSimulatorEnabled!!) {
                triggerEspRelay(activeOnState)
            }
        }

        return device.copy(
            isOn = activeOnState,
            currentSecondsPhaseIsOn = phaseIsOn,
            secondsTimeLeftInPhase = timeLeft
        )
    }

    private suspend fun runMaxCostEngine(device: DeviceEntity): DeviceEntity {
        // Only trigger if cost limits are running
        if (device.isMaxCostActive && device.maxCostStatus == 1) {
            if (device.costSyp >= device.maxCostLimit) {
                // Limit achieved! Turn off device immediately.
                if (device.isOn && !settingsDao.getSettingsDirect()?.isSimulatorEnabled!!) {
                    triggerEspRelay(false)
                }

                // Insert high priority alert only once when transition occurs
                if (device.isOn) {
                    val maxLimitAlert = AlertEntity(
                        deviceId = device.id,
                        deviceName = device.name,
                        timestamp = System.currentTimeMillis(),
                        title = "تم بلوغ الحد الأقصى للتكلفة",
                        message = "تم فصل التغذية عن جهاز [${device.name}] لوصول كلفة استهلاكه للحد الأقصى المجدول: ${device.maxCostLimit} ل.س.",
                        cause = "تجاوز حد الميزانية المالي",
                        severity = "danger"
                    )
                    alertDao.insertAlert(maxLimitAlert)
                }

                return device.copy(
                    isOn = false,
                    isManualOn = false,
                    // Keep maxCostStatus = 1 so that the plan stays active and control buttons are preserved
                    maxCostStatus = 1,
                    scheduledHoursStatus = if (device.scheduledHoursStatus == 1) 2 else device.scheduledHoursStatus, // Pause scheduled if running
                    secondsPlanStatus = if (device.secondsPlanStatus == 1) 2 else device.secondsPlanStatus // Pause seconds if running
                )
            }
        }
        return device
    }

    private fun triggerEspRelay(turnOn: Boolean) {
        val settings = runBlocking { settingsDao.getSettingsDirect() } ?: return
        val ip = settings.espIpAddress
        ioScope.launch {
            // WebSocket Instant delivery
            val wsObj = activeWebSocket
            if (wsObj != null && isEspConnected) {
                try {
                    val payload = JSONObject().apply {
                        put("action", "toggle_relay")
                        put("target_state", if (turnOn) 1 else 0)
                    }.toString()
                    if (wsObj.send(payload)) {
                        Log.d("EspRelay", "Toggled physical relay over WebSocket to: $turnOn")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e("EspRelay", "Error triggering relay over WebSocket: ${e.message}")
                }
            }

            // Fallback REST endpoint
            try {
                val url = "http://$ip/relay?state=${if (turnOn) 1 else 0}"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().close()
                Log.d("EspRelay", "Toggled physical relay over REST backup to $turnOn")
            } catch (e: Exception) {
                Log.e("EspRelay", "Error toggling physical relay over REST backup: ${e.message}")
            }
        }
    }

    suspend fun autoDiscoverEsp(): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            val localIp = getLocalIpAddress()
            val subnetsToScan = mutableListOf<String>()
            
            if (localIp != null) {
                val dotIndex = localIp.lastIndexOf('.')
                if (dotIndex != -1) {
                    subnetsToScan.add(localIp.substring(0, dotIndex + 1))
                }
            }
            
            // Try mDNS registered standard host first (from the provided code "power-ranger-device") on port 81 socket check
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("power-ranger-device.local", 81), 600)
                socket.close()
                val settings = settingsDao.getSettingsDirect() ?: SettingsEntity()
                settingsDao.insertOrUpdateSettings(settings.copy(espIpAddress = "power-ranger-device.local", isSimulatorEnabled = false))
                connectWebSocket("power-ranger-device.local")
                return@withContext Pair(true, "power-ranger-device.local")
            } catch (e: Exception) {
                // Ignore and proceed to scan
            }

            if (!subnetsToScan.contains("192.168.4.")) subnetsToScan.add("192.168.4.") // Custom SoftAP ESP mode
            if (!subnetsToScan.contains("192.168.1.")) subnetsToScan.add("192.168.1.")
            if (!subnetsToScan.contains("192.168.0.")) subnetsToScan.add("192.168.0.")
            
            // We perform parallel socket scans on WebSocket port 81 (much faster than HTTP timeouts)
            for (subnet in subnetsToScan) {
                val foundIp = coroutineScope {
                    val deferredList = (1..254).map { host ->
                        async {
                            val ip = "$subnet$host"
                            if (ip == localIp) return@async null
                            try {
                                val socket = java.net.Socket()
                                socket.connect(java.net.InetSocketAddress(ip, 81), 250)
                                socket.close()
                                return@async ip
                            } catch (e: Exception) {
                                // Ignore
                            }
                            null
                        }
                    }
                    deferredList.awaitAll().firstOrNull { it != null }
                }
                if (foundIp != null) {
                    val settings = settingsDao.getSettingsDirect() ?: SettingsEntity()
                    settingsDao.insertOrUpdateSettings(settings.copy(espIpAddress = foundIp, isSimulatorEnabled = false))
                    connectWebSocket(foundIp)
                    return@withContext Pair(true, foundIp)
                }
            }
            
            return@withContext Pair(false, null)
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val ip = address.hostAddress
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("IPDiscovery", "Error finding local IP: ${ex.message}")
        }
        return null
    }

    // Database mutator utility proxies (exposed to ViewModels)
    suspend fun insertDevice(device: DeviceEntity) = deviceDao.insertDevice(device)
    
    suspend fun updateDevice(device: DeviceEntity) {
        val oldDevice = deviceDao.getDeviceByIdDirect(device.id)
        deviceDao.updateDevice(device)
        if (oldDevice != null) {
            val settings = settingsDao.getSettingsDirect()
            if (settings != null && !settings.isSimulatorEnabled) {
                val stateChanged = oldDevice.isOn != device.isOn
                val planChanged = oldDevice.secondsPlanStatus != device.secondsPlanStatus ||
                                  oldDevice.scheduledHoursStatus != device.scheduledHoursStatus ||
                                  oldDevice.maxCostStatus != device.maxCostStatus ||
                                  oldDevice.isManualMode != device.isManualMode
                if (stateChanged || planChanged) {
                    triggerEspRelay(device.isOn)
                }
            }
        }
    }

    suspend fun deleteDevice(device: DeviceEntity) = deviceDao.deleteDevice(device)
    suspend fun deleteDeviceById(id: Int) = deviceDao.deleteDeviceById(id)
    suspend fun insertAlert(alert: AlertEntity) = alertDao.insertAlert(alert)
    suspend fun clearAllAlerts() = alertDao.clearAllAlerts()
    suspend fun clearAllLogs() = analyticsLogDao.clearAllLogs()
    suspend fun updateSettings(settings: SettingsEntity) {
        val oldSettings = settingsDao.getSettingsDirect()
        settingsDao.insertOrUpdateSettings(settings)
        if (oldSettings == null || oldSettings.espIpAddress != settings.espIpAddress || oldSettings.isSimulatorEnabled != settings.isSimulatorEnabled) {
            if (!settings.isSimulatorEnabled) {
                connectWebSocket(settings.espIpAddress)
            } else {
                activeWebSocket?.close(1000, "Simulator Enabled")
                activeWebSocket = null
                isEspConnected = false
            }
        } else {
            // Settings did not toggle connection, but values changed. Sync updated tariffs if connected.
            val activeWs = activeWebSocket
            if (activeWs != null && isEspConnected && !settings.isSimulatorEnabled) {
                ioScope.launch {
                    try {
                        val tariffPayload = JSONObject().apply {
                            put("action", "set_tariff")
                            put("limit1", settings.tariffBracket1Limit)
                            put("price1", settings.tariffBracket1Price)
                            put("price2", settings.tariffBracket2Price)
                        }.toString()
                        activeWs.send(tariffPayload)
                        Log.d("ESP_WS", "Instantly synced updated tariff settings to active ESP: limit=${settings.tariffBracket1Limit}")
                    } catch (e: Exception) {
                        Log.e("ESP_WS", "Failed to instantly sync updated tariff settings to active ESP: ${e.message}")
                    }
                }
            }
        }
    }

    // Slot parsing utility methods
    fun getSlotsSet(csv: String): Set<Int> {
        if (csv.isEmpty()) return emptySet()
        return csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun makeCsvFromSlots(slots: Set<Int>): String {
        return slots.joinToString(",")
    }

    fun close() {
        ioScope.cancel()
    }
}
