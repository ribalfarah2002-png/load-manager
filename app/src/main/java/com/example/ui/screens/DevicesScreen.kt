package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entities.DeviceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LoadManagerViewModel
import com.example.ui.viewmodel.TimeRangeModel
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: LoadManagerViewModel,
    devices: List<DeviceEntity>
) {
    val selectedId by viewModel.selectedDeviceId.collectAsState()
    val isEditClock by viewModel.isEditClockMode.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    val currentDevice = devices.find { it.id == selectedId }

    // Dropdown state
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deviceToEdit by remember { mutableStateOf<DeviceEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp)
    ) {
        
        Spacer(modifier = Modifier.height(16.dp))

        // Device Selection Selector Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSlate),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dropdown to choose device
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { dropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNavy),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth().testTag("device_dropdown_trigger")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "قائمة منسدلة")
                            Text(
                                text = currentDevice?.name ?: "اختر جهازًا...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(CyberSlate).border(1.dp, BorderDark)
                    ) {
                        devices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text(dev.name, color = Color.White, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                onClick = {
                                    viewModel.selectDevice(dev.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Plus button to add device
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricTeal.copy(alpha = 0.15f))
                        .border(1.dp, ElectricTeal.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("add_device_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة جهاز", tint = ElectricTeal)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete button
                IconButton(
                    onClick = {
                        currentDevice?.let { viewModel.deleteCurrentDevice(it) }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EnergyRed.copy(alpha = 0.15f))
                        .border(1.dp, EnergyRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("delete_device_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف جهاز", tint = EnergyRed)
                }
            }
        }

        if (currentDevice == null) {
            // Placeholder Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PrecisionManufacturing,
                        contentDescription = "جهاز فارغ",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "يرجى اختيار جهاز أو إضافة جهاز ذكي جديد لإدارته.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Main Controls Panel for selected device
            Spacer(modifier = Modifier.height(16.dp))

            // Display title and core active switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentDevice.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { deviceToEdit = currentDevice },
                                    modifier = Modifier.size(28.dp).testTag("edit_device_icon_${currentDevice.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "تعديل معلومات الجهاز",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (currentDevice.isOn) "مغلق الدفوع (تشغيل)" else "مفتوح الدفوع (إيقاف)",
                                color = if (currentDevice.isOn) ElectricTeal else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            var remainingSec by remember(currentDevice.id, currentDevice.lastStateChangeTime, currentDevice.isOn) {
                                mutableStateOf(0)
                            }
                            LaunchedEffect(currentDevice.id, currentDevice.lastStateChangeTime, currentDevice.isOn) {
                                while (true) {
                                    if (currentDevice.deviceType == "انارة" || currentDevice.lastStateChangeTime == 0L) {
                                        remainingSec = 0
                                    } else {
                                        val elapsedSec = (System.currentTimeMillis() - currentDevice.lastStateChangeTime) / 1000
                                        val limit = if (currentDevice.isOn) currentDevice.timeToOn else currentDevice.timeToOff
                                        val remaining = limit - elapsedSec
                                        remainingSec = if (remaining > 0) remaining.toInt() else 0
                                    }
                                    if (remainingSec == 0) break
                                    kotlinx.coroutines.delay(1000)
                                }
                            }

                            if (remainingSec > 0) {
                                Text(
                                    text = "متبقي للحماية: $remainingSec ثانية",
                                    color = NeonGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Switch(
                        checked = currentDevice.isOn,
                        onCheckedChange = { viewModel.toggleDeviceSwitch(currentDevice) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberNavy,
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = BorderDark
                        ),
                        modifier = Modifier.testTag("device_main_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual / Auto Selection Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSlate, RoundedCornerShape(12.dp))
                    .padding(4.dp)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            ) {
                val isManual = currentDevice.isManualMode
                
                Button(
                    onClick = { viewModel.requestSetDeviceManualMode(currentDevice, false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isManual) ElectricCyan else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("auto_mode_tab"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "الوضع التلقائي (Auto)",
                        color = if (!isManual) CyberNavy else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { viewModel.requestSetDeviceManualMode(currentDevice, true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isManual) ElectricCyan else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_mode_tab"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "الوضع اليدوي (Manual)",
                        color = if (isManual) CyberNavy else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggleable Modes Content
            if (currentDevice.isManualMode) {
                // Manual Mode Details UI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("التحكم اليدوي النشط", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(
                            text = "في الوضع اليدوي، يتم التحكم بتشغيل وإيقاف تلامس الـ Relay للجهاز مباشرة وبدون تدخل الخطط الذكية. قم بالضغط على المبدل العلوي للتبديل السريع.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // ------------------ AUTO MODE ------------------
                var autoTabSelected by remember { mutableStateOf(0) } // 0 = cost, 1 = hours, 2 = seconds
                
                TabRow(
                    selectedTabIndex = autoTabSelected,
                    containerColor = CyberNavy,
                    contentColor = ElectricCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            color = ElectricCyan,
                            modifier = Modifier.tabIndicatorOffset(tabPositions[autoTabSelected])
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = autoTabSelected == 0,
                        onClick = { autoTabSelected = 0 },
                        text = { Text("أقصى تكلفة", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = autoTabSelected == 1,
                        onClick = { autoTabSelected = 1 },
                        text = { Text("جدول الساعات", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = autoTabSelected == 2,
                        onClick = { autoTabSelected = 2 },
                        text = { Text("مؤقت الثواني", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AUTO PANEL 1: MAX COST BUDGET LIMIT
                if (autoTabSelected == 0) {
                    AutoModeBudgetCost(
                        device = currentDevice,
                        viewModel = viewModel
                    )
                }

                // AUTO PANEL 2: HOURS DIAL TIMELINE SCHEDULER
                if (autoTabSelected == 1) {
                    AutoModeClockScheduler(
                        device = currentDevice,
                        viewModel = viewModel,
                        isEditClock = isEditClock
                    )
                }

                // AUTO PANEL 3: SECONDS REPETITIVE WORK TIMER
                if (autoTabSelected == 2) {
                    AutoModeSecondsIntervalPanel(
                        device = currentDevice,
                        viewModel = viewModel
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // real-time numerical readings at the very bottom
            PzemTelemetryFooter(device = currentDevice)
        }
    }

    // Modal adding new device
    if (showAddDialog) {
        var newDeviceName by remember { mutableStateOf("") }
        val deviceTypes = listOf(
            "ضاغط (مكيف - براد - ثلاجة...)" to "ضاغط",
            "احمال حرارية (مدفئة كهربائية - مكواة - سخان مياه...)" to "حمل حراري",
            "اجهزة كهربائية متنوعة (مروحة - خلاط - جلاية - غسالة - شواحن...)" to "اجهزة كهربائية",
            "انارة" to "انارة"
        )
        var selectedTypeIndex by remember { mutableStateOf(3) } // Default to "انارة"
        var isDropdownExpanded by remember { mutableStateOf(false) }

        var timeToOnStr by remember { mutableStateOf("") }
        var timeToOffStr by remember { mutableStateOf("") }

        val onTypeSelected = { index: Int ->
            selectedTypeIndex = index
            when (deviceTypes[index].second) {
                "ضاغط" -> {
                    timeToOnStr = "15"
                    timeToOffStr = "15"
                }
                "حمل حراري" -> {
                    timeToOnStr = "10"
                    timeToOffStr = "10"
                }
                "اجهزة كهربائية" -> {
                    timeToOnStr = "5"
                    timeToOffStr = "5"
                }
                "انارة" -> {
                    timeToOnStr = ""
                    timeToOffStr = ""
                }
            }
        }

        val sheetColor = CyberSlate

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "إضافة جهاز تحكم ذكي جديد",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "أدخل اسم الجهاز المرتبط بدارة ESP8266 والـ PZEM لحساب القراءات والتحكم بالتشغيل:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDeviceName,
                        onValueChange = { newDeviceName = it },
                        label = { Text("اسم الجهاز (مثل: المكيف)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_device_name_field")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "نوع الجهاز:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown menu container
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricCyan)
                                Text(
                                    text = deviceTypes[selectedTypeIndex].first,
                                    textAlign = TextAlign.Right,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth().background(CyberSlate)
                        ) {
                            deviceTypes.forEachIndexed { idx, pair ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = pair.first,
                                            color = Color.White,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        onTypeSelected(idx)
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = timeToOffStr,
                            onValueChange = { timeToOffStr = it },
                            label = { Text("الوقت للاطفاء (ثانية)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderDark
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = timeToOnStr,
                            onValueChange = { timeToOnStr = it },
                            label = { Text("الوقت للتشغيل (ثانية)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderDark
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDeviceName.isNotBlank()) {
                            val type = deviceTypes[selectedTypeIndex].second
                            val tOn = timeToOnStr.toIntOrNull() ?: 0
                            val tOff = timeToOffStr.toIntOrNull() ?: 0
                            viewModel.createNewDevice(
                                name = newDeviceName,
                                deviceType = type,
                                timeToOn = tOn,
                                timeToOff = tOff
                            )
                            showAddDialog = false
                            newDeviceName = ""
                            timeToOnStr = ""
                            timeToOffStr = ""
                            selectedTypeIndex = 3
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("إضافة الآن", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = sheetColor
        )
    }

    // Modal editing existing device
    val editingDevice = deviceToEdit
    if (editingDevice != null) {
        var editDeviceName by remember(editingDevice.id) { mutableStateOf(editingDevice.name) }
        val deviceTypes = listOf(
            "ضاغط (مكيف - براد - ثلاجة...)" to "ضاغط",
            "احمال حرارية (مدفئة كهربائية - مكواة - سخان مياه...)" to "حمل حراري",
            "اجهزة كهربائية متنوعة (مروحة - خلاط - جلاية - غسالة - شواحن...)" to "اجهزة كهربائية",
            "انارة" to "انارة"
        )
        // Find current type index
        val initialIdx = deviceTypes.indexOfFirst { it.second == editingDevice.deviceType }.let { if (it == -1) 3 else it }
        var selectedTypeIndex by remember(editingDevice.id) { mutableStateOf(initialIdx) }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        var timeToOnStr by remember(editingDevice.id) { mutableStateOf(editingDevice.timeToOn.let { if (it == 0) "" else it.toString() }) }
        var timeToOffStr by remember(editingDevice.id) { mutableStateOf(editingDevice.timeToOff.let { if (it == 0) "" else it.toString() }) }

        val onTypeSelected = { index: Int ->
            selectedTypeIndex = index
            when (deviceTypes[index].second) {
                "ضاغط" -> {
                    timeToOnStr = "15"
                    timeToOffStr = "15"
                }
                "حمل حراري" -> {
                    timeToOnStr = "10"
                    timeToOffStr = "10"
                }
                "اجهزة كهربائية" -> {
                    timeToOnStr = "5"
                    timeToOffStr = "5"
                }
                "انارة" -> {
                    timeToOnStr = ""
                    timeToOffStr = ""
                }
            }
        }

        AlertDialog(
            onDismissRequest = { deviceToEdit = null },
            title = {
                Text(
                    text = "تعديل جهاز التحكم الذكي",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "قم بتعديل معلومات الجهاز المرتبط بدارة ESP8266 والـ PZEM لحساب القراءات والتحكم بالتشغيل:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDeviceName,
                        onValueChange = { editDeviceName = it },
                        label = { Text("اسم الجهاز (مثل: المكيف)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_device_name_field")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "نوع الجهاز:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown menu container
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ElectricCyan)
                                Text(
                                    text = deviceTypes[selectedTypeIndex].first,
                                    textAlign = TextAlign.Right,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth().background(CyberSlate)
                        ) {
                            deviceTypes.forEachIndexed { idx, pair ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = pair.first,
                                            color = Color.White,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        onTypeSelected(idx)
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = timeToOffStr,
                            onValueChange = { timeToOffStr = it },
                            label = { Text("الوقت للاطفاء (ثانية)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderDark
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = timeToOnStr,
                            onValueChange = { timeToOnStr = it },
                            label = { Text("الوقت للتشغيل (ثانية)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderDark
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDeviceName.isNotBlank()) {
                            val type = deviceTypes[selectedTypeIndex].second
                            val tOn = timeToOnStr.toIntOrNull() ?: 0
                            val tOff = timeToOffStr.toIntOrNull() ?: 0
                            viewModel.updateDeviceProperties(
                                device = editingDevice,
                                name = editDeviceName,
                                deviceType = type,
                                timeToOn = tOn,
                                timeToOff = tOff
                            )
                            deviceToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("حفظ التعديلات", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToEdit = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSlate
        )
    }
}

// 1. AUTO PLAN 1: BUDGET PROGRESS CARD WITH THE LARGE FILL CIRCLE
@Composable
fun AutoModeBudgetCost(
    device: DeviceEntity,
    viewModel: LoadManagerViewModel
) {
    var costSliderVal by remember(device.id, device.pendingMaxCostLimit) { mutableStateOf(device.pendingMaxCostLimit.toFloat()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. خطة الحد الأقصى للتكلفة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                
                // Active Badge Status
                AutoIndicatorBadge(status = device.maxCostStatus)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cost Budget Limit Slider: up to 500,000 SYP, step 500
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الأقصى: 500,000 ل.س", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = String.format(Locale.US, "الحد المالي: %,.0f ل.س", costSliderVal),
                    color = NeonGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = costSliderVal,
                onValueChange = { costSliderVal = (it / 500).toInt() * 500f },
                onValueChangeFinished = { viewModel.updatePendingMaxCostLimit(device, costSliderVal.toDouble()) },
                valueRange = 0f..500000f,
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonGold,
                    inactiveTrackColor = BorderDark,
                    thumbColor = NeonGold
                ),
                modifier = Modifier.fillMaxWidth().testTag("cost_slider")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // THE LARGE COLORFUL CIRCLE
            val currentCost = device.costSyp
            val targetLimit = if (device.maxCostStatus != 0) device.maxCostLimit.coerceAtLeast(1.0) else costSliderVal.toDouble().coerceAtLeast(1.0)
            val fillRatio = if (device.maxCostStatus != 0) {
                (currentCost / targetLimit).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }
            val costRemaining = if (device.maxCostStatus != 0) {
                (targetLimit - currentCost).coerceAtLeast(0.0)
            } else {
                targetLimit
            }

            Box(
                modifier = Modifier
                    .size(170.dp)
                    .background(Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2, size.height / 2)

                     // Draw Background Gray Circle
                     drawCircle(
                         color = BorderDark,
                         radius = radius - 15.dp.toPx(),
                         style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                     )
 
                     // Draw Filled Arc
                     drawArc(
                         color = NeonGold,
                         startAngle = -90f,
                         sweepAngle = fillRatio * 360f,
                         useCenter = false,
                         size = Size((radius - 15.dp.toPx()) * 2f, (radius - 15.dp.toPx()) * 2f),
                         topLeft = Offset(center.x - radius + 15.dp.toPx(), center.y - radius + 15.dp.toPx()),
                         style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                     )
                }

                // Info Inside Circle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المستهلك", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format(Locale.US, "%,.0f ل.س", currentCost),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("المتبقي", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format(Locale.US, "%,.0f ل.س", costRemaining),
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Three Control Action Buttons
            AutoDualActionButtons(
                status = device.maxCostStatus,
                onStart = { viewModel.startMaxCostLimit(device, costSliderVal.toDouble()) },
                onPause = { viewModel.pauseMaxCostLimit(device) },
                onCancel = { viewModel.cancelMaxCostLimit(device) }
            )
        }
    }
}

// 2. AUTO PLAN 2: HOURS DRAG TO PAINT SCHEDULER
@Composable
fun AutoModeClockScheduler(
    device: DeviceEntity,
    viewModel: LoadManagerViewModel,
    isEditClock: Boolean
) {
    // Current selected indices from comma separated slots (using pending/draft slots)
    // Seed pending with selected if pending is empty to prevent lost configuration initially
    val initialSlots = remember(device.id, device.pendingSlotsCsv, device.selectedSlotsCsv) {
        val slots = viewModel.getSlotsSet(device.pendingSlotsCsv)
        if (slots.isEmpty() && device.selectedSlotsCsv.isNotEmpty() && !isEditClock) {
            viewModel.getSlotsSet(device.selectedSlotsCsv)
        } else {
            slots
        }
    }
    var isDragging by remember { mutableStateOf(false) }
    val tempSlots = remember { mutableStateOf(initialSlots) }
    LaunchedEffect(initialSlots) {
        if (!isDragging) {
            tempSlots.value = initialSlots
        }
    }
    var hoveredTimeStr by remember { mutableStateOf<String?>(null) }

    val activeSlotsSet = remember(device.selectedSlotsCsv) {
        viewModel.getSlotsSet(device.selectedSlotsCsv)
    }
    val pendingSlotsSet = remember(device.pendingSlotsCsv) {
        viewModel.getSlotsSet(device.pendingSlotsCsv)
    }
    
    // Display intervals for pending/draft slots or active running slots, optimized during active dragging
    var lastIntervals by remember { mutableStateOf(emptyList<com.example.ui.viewmodel.TimeRangeModel>()) }
    val timeIntervals = remember(isDragging, tempSlots.value, isEditClock, device.scheduledHoursStatus, device.selectedSlotsCsv, device.pendingSlotsCsv) {
        if (!isDragging) {
            val list = viewModel.getIntervalsList(
                if (isEditClock) {
                    viewModel.makeCsvFromSlots(tempSlots.value)
                } else {
                    if (device.scheduledHoursStatus != 0) {
                        device.selectedSlotsCsv
                    } else {
                        device.pendingSlotsCsv
                    }
                }
            )
            lastIntervals = list
            list
        } else {
            lastIntervals
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. جدول ساعات التشغيل",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                AutoIndicatorBadge(status = device.scheduledHoursStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "اسحب فوق قرص الساعة لتلوين ساعات العمل. المربعات الملونة تمثل ساعات التشغيل الذكي والبيضاء ساعات الإطفاء.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Edit button controlling gesture scroll lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditClock) {
                    Button(
                        onClick = {
                            tempSlots.value = emptySet()
                            viewModel.updatePendingSlots(device, emptySet())
                            // Do not cancel the running plan immediately! The user must click "بدء الخطة" to apply the empty plan.
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyRed.copy(alpha = 0.2f), contentColor = EnergyRed),
                        border = BorderStroke(1.dp, EnergyRed.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تفريغ كامل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EnergyRed)
                    }
                }

                Button(
                    onClick = {
                        if (isEditClock) {
                            viewModel.isEditClockMode.value = false
                            viewModel.updatePendingSlots(device, tempSlots.value)
                        } else {
                            viewModel.isEditClockMode.value = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isEditClock) ElectricTeal else CyberNavy),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.weight(if (isEditClock) 1f else 2f).testTag("clock_edit_toggle")
                ) {
                    Icon(
                        imageVector = if (isEditClock) Icons.Default.Done else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isEditClock) CyberNavy else ElectricTeal
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditClock) "إيقاف التعديل" else "تعديل الجدول (Modify)",
                        color = if (isEditClock) CyberNavy else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isEditClock) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val prevSlots = viewModel.getSlotsSet(device.selectedSlotsCsv)
                        tempSlots.value = prevSlots
                        viewModel.updatePendingSlots(device, prevSlots)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNavy, contentColor = ElectricTeal),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("revert_to_prev_plan_btn")
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricTeal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("العودة للخطة السابقة (Restore Previous)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricTeal)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // THE 24-HOUR RADIAL WHEEL DIAL
            val dragHistory = remember { ArrayList<Int>() }
            var dragStartSlots by remember { mutableStateOf(emptySet<Int>()) }
            var isPaintingMode by remember { mutableStateOf(true) }
            var currentFingerOffset by remember { mutableStateOf<Offset?>(null) }

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .pointerInput(isEditClock, device.id) {
                        if (isEditClock) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    currentFingerOffset = offset
                                    val slot = calculateClockSlot(offset, size.width.toFloat(), size.height.toFloat())
                                    if (slot in 0..287) {
                                        isPaintingMode = !tempSlots.value.contains(slot)
                                        dragStartSlots = tempSlots.value.toSet()
                                        dragHistory.clear()
                                        dragHistory.add(slot)
                                        
                                        val nextSlots = tempSlots.value.toMutableSet()
                                        if (isPaintingMode) {
                                            nextSlots.add(slot)
                                        } else {
                                            nextSlots.remove(slot)
                                        }
                                        tempSlots.value = nextSlots
                                        
                                        val totalMin = slot * 5
                                        val h = totalMin / 60
                                        val m = totalMin % 60
                                        hoveredTimeStr = String.format(Locale.US, "%02d:%02d", h, m)
                                    }
                                },
                                onDrag = { change, _ ->
                                    val offset = change.position
                                    currentFingerOffset = offset
                                    val slot = calculateClockSlot(offset, size.width.toFloat(), size.height.toFloat())
                                    if (slot in 0..287) {
                                        val totalMin = slot * 5
                                        val h = totalMin / 60
                                        val m = totalMin % 60
                                        hoveredTimeStr = String.format(Locale.US, "%02d:%02d", h, m)

                                        if (dragHistory.isNotEmpty()) {
                                            val lastSlot = dragHistory.last()
                                            if (slot != lastSlot) {
                                                val index = dragHistory.indexOf(slot)
                                                if (index != -1 && index < dragHistory.size - 1) {
                                                    // BACKTRACKING - undo painting/erasing back to this point step-by-step
                                                    val nextSlots = tempSlots.value.toMutableSet()
                                                    for (i in (dragHistory.size - 1) downTo (index + 1)) {
                                                        val poppedSlot = dragHistory[i]
                                                        if (dragStartSlots.contains(poppedSlot)) {
                                                            nextSlots.add(poppedSlot)
                                                        } else {
                                                            nextSlots.remove(poppedSlot)
                                                        }
                                                        dragHistory.removeAt(i)
                                                    }
                                                    tempSlots.value = nextSlots
                                                } else if (index == -1) {
                                                    // Forward drag step - interpolate path from lastSlot to slot
                                                    val nextSlots = tempSlots.value.toMutableSet()
                                                    val diff = (slot - lastSlot + 288) % 288
                                                    val step = if (diff <= 144) 1 else -1
                                                    var temp = lastSlot
                                                    while (temp != slot) {
                                                        temp = (temp + step + 288) % 288
                                                        if (!dragHistory.contains(temp)) {
                                                            dragHistory.add(temp)
                                                            if (isPaintingMode) {
                                                                nextSlots.add(temp)
                                                            } else {
                                                                nextSlots.remove(temp)
                                                            }
                                                        }
                                                    }
                                                    tempSlots.value = nextSlots
                                                }
                                            }
                                        } else {
                                            dragHistory.add(slot)
                                        }
                                    }
                                    change.consume()
                                },
                                onDragEnd = {
                                    isDragging = false
                                    dragHistory.clear()
                                    currentFingerOffset = null
                                    hoveredTimeStr = null
                                    viewModel.updatePendingSlots(device, tempSlots.value)
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragHistory.clear()
                                    currentFingerOffset = null
                                    hoveredTimeStr = null
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw background grey ring representing unselected slots (all 288 segments in one go)
                    drawArc(
                        color = BorderDark,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = true,
                        size = Size((radius - 20.dp.toPx()) * 2f, (radius - 20.dp.toPx()) * 2f),
                        topLeft = Offset(center.x - radius + 20.dp.toPx(), center.y - radius + 20.dp.toPx())
                    )

                    // Draw selected slots on top depending on active plan state or edit mode
                    val slotsToDraw = if (isEditClock) {
                        tempSlots.value
                    } else {
                        if (device.scheduledHoursStatus != 0) {
                            activeSlotsSet
                        } else {
                            pendingSlotsSet
                        }
                    }

                    if (slotsToDraw.size == 288) {
                        // Entire circle is selected
                        drawArc(
                            color = ElectricCyan,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = true,
                            size = Size((radius - 20.dp.toPx()) * 2f, (radius - 20.dp.toPx()) * 2f),
                            topLeft = Offset(center.x - radius + 20.dp.toPx(), center.y - radius + 20.dp.toPx())
                        )
                    } else if (slotsToDraw.isNotEmpty()) {
                        val hasSlot = BooleanArray(288) { it in slotsToDraw }
                        // Find an index where hasSlot is false to start our scan (guaranteed to exist since size < 288)
                        var startScan = 0
                        while (startScan < 288 && hasSlot[startScan]) {
                            startScan++
                        }
                        
                        var i = (startScan + 1) % 288
                        var currentStart = -1
                        var currentCount = 0
                        
                        repeat(288) {
                            if (hasSlot[i]) {
                                if (currentStart == -1) {
                                    currentStart = i
                                }
                                currentCount++
                            } else {
                                if (currentStart != -1) {
                                    val angle = (currentStart * 360f / 288f) - 90f
                                    val sweep = currentCount * 360f / 288f
                                    drawArc(
                                        color = ElectricCyan,
                                        startAngle = angle,
                                        sweepAngle = sweep,
                                        useCenter = true,
                                        size = Size((radius - 20.dp.toPx()) * 2f, (radius - 20.dp.toPx()) * 2f),
                                        topLeft = Offset(center.x - radius + 20.dp.toPx(), center.y - radius + 20.dp.toPx())
                                    )
                                    currentStart = -1
                                    currentCount = 0
                                }
                            }
                            i = (i + 1) % 288
                        }
                        // Handle final open interval if any
                        if (currentStart != -1) {
                            val angle = (currentStart * 360f / 288f) - 90f
                            val sweep = currentCount * 360f / 288f
                            drawArc(
                                color = ElectricCyan,
                                startAngle = angle,
                                sweepAngle = sweep,
                                useCenter = true,
                                size = Size((radius - 20.dp.toPx()) * 2f, (radius - 20.dp.toPx()) * 2f),
                                topLeft = Offset(center.x - radius + 20.dp.toPx(), center.y - radius + 20.dp.toPx())
                            )
                        }
                    }

                    // Inner mask to turn it into a hollow ring/donut
                    drawCircle(
                        color = CyberSlate,
                        radius = radius - 35.dp.toPx(),
                        center = center
                    )

                    // Draw ticking reference scales for hours
                    for (hour in 0..23) {
                        val angleRad = Math.toRadians((hour * 15.0) - 90.0).toFloat()
                        val tickStart = Offset(
                            center.x + (radius - 12.dp.toPx()) * cos(angleRad),
                            center.y + (radius - 12.dp.toPx()) * sin(angleRad)
                        )
                        val tickEnd = Offset(
                            center.x + (radius - 3.dp.toPx()) * cos(angleRad),
                            center.y + (radius - 3.dp.toPx()) * sin(angleRad)
                        )
                        val weight = if (hour % 6 == 0) 3.dp.toPx() else 1.dp.toPx()
                        
                        drawLine(
                            color = if (hour % 6 == 0) ElectricTeal else TextSecondary.copy(alpha = 0.5f),
                            start = tickStart,
                            end = tickEnd,
                            strokeWidth = weight
                        )

                        // Hour labels
                        if (hour % 6 == 0) {
                            val labelX = center.x + (radius - 28.dp.toPx()) * cos(angleRad)
                            val labelY = center.y + (radius - 28.dp.toPx()) * sin(angleRad)

                            drawContext.canvas.nativeCanvas.drawText(
                                String.format(Locale.US, "%02d", hour),
                                labelX,
                                labelY + 4.dp.toPx(),
                                Paint().apply {
                                    color = Color.White.toArgb()
                                    textSize = 10.sp.toPx()
                                    textAlign = Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                            )
                        }
                    }

                    // Draw finger pointer feedback if dragging
                    if (isEditClock && currentFingerOffset != null) {
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.15f),
                            radius = 24.dp.toPx(),
                            center = currentFingerOffset!!
                        )
                        drawCircle(
                            color = ElectricCyan,
                            radius = 6.dp.toPx(),
                            center = currentFingerOffset!!
                        )
                    }
                }
                
                // Clock label inside
                val displaySlotsCount = if (isEditClock) {
                    tempSlots.value.size
                } else {
                    if (device.scheduledHoursStatus != 0) {
                        activeSlotsSet.size
                    } else {
                        pendingSlotsSet.size
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (hoveredTimeStr != null) {
                        Text("التوقيت الحالي", color = NeonGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(hoveredTimeStr!!, color = NeonGold, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("تحديد دقيق", color = TextSecondary, fontSize = 10.sp)
                    } else {
                        Text("الساعات", color = TextSecondary, fontSize = 11.sp)
                        Text("00:00 - 24:00", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatMinutesToArabic(displaySlotsCount * 5),
                            color = ElectricTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time range ranges display list under the clock dial
            if (timeIntervals.isEmpty()) {
                Text(
                    text = "لم يتم تحديد أي فترات تشغيل بعد.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                Text("فترات العمل المحددة:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Spacer(modifier = Modifier.height(6.dp))
                
                timeIntervals.forEach { range ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(CyberNavy, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.deleteTimeRange(device, range.startSlot, range.endSlot) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف النطاق", tint = EnergyRed, modifier = Modifier.size(16.dp))
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(range.text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Actions Buttons
            AutoDualActionButtons(
                status = device.scheduledHoursStatus,
                onStart = { viewModel.requestStartScheduledHoursPlan(device) },
                onPause = { viewModel.pauseScheduledHoursPlan(device) },
                onCancel = { viewModel.cancelScheduledHoursPlan(device) }
            )
        }
    }
}

private fun calculateClockSlot(offset: Offset, width: Float, height: Float): Int {
    val dx = offset.x - (width / 2)
    val dy = offset.y - (height / 2)
    var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
    if (angle < 0f) angle += 360f
    return ((angle / 360f) * 288).toInt() % 288
}

// 3. AUTO PLAN 3: SECONDS DIAL PLAN CONTROLLER
@Composable
fun AutoModeSecondsIntervalPanel(
    device: DeviceEntity,
    viewModel: LoadManagerViewModel
) {
    // Sliders state (mapped with non linear scale, using draft pending values)
    var sliderOnVal by remember(device.id, device.pendingSecondsOn) { mutableStateOf(mapSecondsToSlider(device.pendingSecondsOn)) }
    var sliderOffVal by remember(device.id, device.pendingSecondsOff) { mutableStateOf(mapSecondsToSlider(device.pendingSecondsOff)) }

    val mappedSecondsOn = mapSliderToSeconds(sliderOnVal)
    val mappedSecondsOff = mapSliderToSeconds(sliderOffVal)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3. خطة ومؤقت الثواني المكررة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                AutoIndicatorBadge(status = device.secondsPlanStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "يتم تنفيذ ومراقبة هذه الخطة بالكامل محليًا على التطبيق لضمان تواصل فوري بدون تأخير. ترسل فقط الأوامر لـ ESP.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SLIDER 1 : Seconds ON
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الحد الاقصى: ساعتين", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = "ثواني التشغيل: ${formatDurationFromSeconds(mappedSecondsOn)}",
                        color = ElectricTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = sliderOnVal,
                    onValueChange = {
                        sliderOnVal = it
                    },
                    onValueChangeFinished = {
                        viewModel.updatePendingSecondsPlanTiming(device, mapSliderToSeconds(sliderOnVal), mappedSecondsOff)
                    },
                    valueRange = 0f..430f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = ElectricTeal,
                        inactiveTrackColor = BorderDark,
                        thumbColor = ElectricTeal
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("on_seconds_slider")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SLIDER 2 : Seconds OFF
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الحد الاقصى: ساعتين", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = "ثواني الإطفاء: ${formatDurationFromSeconds(mappedSecondsOff)}",
                        color = EnergyRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = sliderOffVal,
                    onValueChange = {
                        sliderOffVal = it
                    },
                    onValueChangeFinished = {
                        viewModel.updatePendingSecondsPlanTiming(device, mappedSecondsOn, mapSliderToSeconds(sliderOffVal))
                    },
                    valueRange = 0f..430f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = EnergyRed,
                        inactiveTrackColor = BorderDark,
                        thumbColor = EnergyRed
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("off_seconds_slider")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ACTIVE TICKING COUNTDOWN SCREEN VIEW (Visible if Running or Paused)
            if (device.secondsPlanStatus == 1 || device.secondsPlanStatus == 2) { 
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberNavy, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (device.secondsPlanStatus == 2) NeonGold.copy(alpha = 0.15f) else if (device.currentSecondsPhaseIsOn) ElectricTeal.copy(alpha = 0.15f) else EnergyRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (device.secondsPlanStatus == 2) {
                                    " موقوف مؤقتًا PAUSED "
                                } else if (device.currentSecondsPhaseIsOn) {
                                    " فئة التشغيل النشطة ON "
                                } else {
                                    " فئة الإيقاف المؤقتة OFF "
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (device.secondsPlanStatus == 2) NeonGold else if (device.currentSecondsPhaseIsOn) ElectricTeal else EnergyRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = formatDurationFromSeconds(device.secondsTimeLeftInPhase),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        
                        Text(
                            text = if (device.secondsPlanStatus == 2) "العد التنازلي متوقف مؤقتًا" else "العد التنازلي للمرحلة الحالية", 
                            color = TextSecondary, 
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Three Control Action Buttons
            AutoDualActionButtons(
                status = device.secondsPlanStatus,
                onStart = { viewModel.requestStartSecondsPlan(device) },
                onPause = { viewModel.pauseSecondsPlan(device) },
                onCancel = { viewModel.cancelSecondsPlan(device) }
            )
        }
    }
}

// Map continuous slider float range 0..430 to target non-linear seconds steps values
fun mapSliderToSeconds(value: Float): Int {
    val x = value.toInt()
    return when {
        x <= 300 -> x // Level 1 (0 to 5 mins): seconds tick one by one [0s to 300s]
        x <= 330 -> 300 + (x - 300) * 30 // Level 2 (5 to 20 mins): 30s step [300s to 1200s]
        else -> 1200 + (x - 330) * 60 // Level 3 (20 mins to 2 hours): 60s step [1200s to 7200s]
    }.coerceIn(0, 7200)
}

// Reverse mapping from target seconds values back to slider float steps (0..430)
fun mapSecondsToSlider(seconds: Int): Float {
    return when {
        seconds <= 300 -> seconds.toFloat()
        seconds <= 1200 -> (300f + (seconds - 300) / 30f)
        else -> (330f + (seconds - 1200) / 60f)
    }.coerceIn(0f, 430f)
}

fun formatDurationFromSeconds(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
}


// Shared widgets utilities
@Composable
fun AutoIndicatorBadge(status: Int) {
    val (text, color) = when (status) {
        1 -> Pair("قيد العمل", ElectricTeal)
        2 -> Pair("موقوف مؤقتًا", NeonGold)
        else -> Pair("غير نشط", TextSecondary)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AutoDualActionButtons(
    status: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    if (status == 0) {
        // Only show start plan button when not running
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("بدء الخطة", color = CyberNavy, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EnergyRed),
                border = BorderStroke(1.dp, EnergyRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إلغاء الخطة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Pause/Resume button
            Button(
                onClick = onPause,
                modifier = Modifier.weight(1.1f),
                colors = ButtonDefaults.buttonColors(containerColor = CyberNavy),
                border = BorderStroke(1.dp, NeonGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (status == 2) "استمرار" else "إيقاف مؤقت",
                    color = NeonGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Enabled start plan button to let users start a new active plan or apply modified settings
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1.1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricTeal,
                    contentColor = CyberNavy
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("بدء الخطة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// REALTIME TELEMETRY PANEL AT FOOTER
@Composable
fun PzemTelemetryFooter(device: DeviceEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("telemetry_footer"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "قراءات مستشعر PZEM-004T v4 الحالية",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid Layout 3 columns
            Row(modifier = Modifier.fillMaxWidth()) {
                TelemetryItem(label = "الجهد (Voltage)", value = String.format(Locale.US, "%.1f V", device.voltage), modifier = Modifier.weight(1f))
                TelemetryItem(label = "التيار (Current)", value = String.format(Locale.US, "%.2f A", device.current), modifier = Modifier.weight(1f))
                TelemetryItem(label = "الاستطاعة (Power)", value = String.format(Locale.US, "%.1f W", device.activePower), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TelemetryItem(label = "الاستهلاك (Energy)", value = String.format(Locale.US, "%.3f kWh", device.energyWh / 1000.0), modifier = Modifier.weight(1f))
                TelemetryItem(label = "التردد (Frequency)", value = String.format(Locale.US, "%.1f Hz", device.frequency), modifier = Modifier.weight(1f))
                TelemetryItem(label = "عامل الاستطاعة (PF)", value = String.format(Locale.US, "%.2f", device.powerFactor), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .background(CyberNavy, RoundedCornerShape(8.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

fun formatMinutesToArabic(totalMinutes: Int): String {
    if (totalMinutes == 0) return "0 دقيقة"
    if (totalMinutes < 60) {
        return "$totalMinutes دقيقة"
    }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    
    val hoursStr = when (hours) {
        1 -> "ساعة"
        2 -> "ساعتين"
        in 3..10 -> "$hours ساعات"
        else -> "$hours ساعة"
    }
    
    val minutesStr = when (minutes) {
        0 -> ""
        1 -> "ودقيقة"
        2 -> "ودقيقتين"
        in 3..10 -> "و$minutes دقائق"
        else -> "و$minutes دقيقة"
    }
    
    return if (minutesStr.isEmpty()) hoursStr else "$hoursStr $minutesStr"
}
