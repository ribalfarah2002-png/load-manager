package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entities.DeviceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LoadManagerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LoadManagerViewModel,
    devices: List<DeviceEntity>,
    onNavigateToDevices: (Int) -> Unit
) {
    val settingsState by viewModel.settings.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Calculate aggregated metrics
    val totalWh = devices.sumOf { it.energyWh }
    val totalKwh = totalWh / 1000.0
    val totalCost = devices.sumOf { it.costSyp }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التحكم الذكي بالأحمال",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "PZEM-004T v4 Monitor",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CyberSlate)
                        .border(1.dp, BorderDark, CircleShape)
                        .testTag("esp_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "إعدادات الربط اللاسلكي",
                        tint = ElectricCyan
                    )
                }
            }
        }

        // Master Summary Panel (KWh and SYP Price)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("total_consumption_panel"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = " الاستهلاك الإجمالي ",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = ElectricCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (settingsState.isSimulatorEnabled) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElectricTeal.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = " وضع المحاكاة ",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = ElectricTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = " متصل بـ ESP8266 ",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = NeonGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Aggregated Cost in Syrian Pounds
                    Text(
                        text = String.format(Locale.US, "%,.0f ل.س", totalCost),
                        color = NeonGold,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "التكلفة التقديرية الحالية",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Divider(color = BorderDark, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // KWh Reading
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "الكيلو واط الساعي",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "%.3f", totalKwh),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "kW.h",
                            color = ElectricCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section Title: Active Device States
        item {
            Text(
                text = "مراقبة أحمال التغذية المنزلية (${devices.size}):",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Empty State Check
        if (devices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DeviceUnknown,
                            contentDescription = "لا تتوفر أجهزة",
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد أجهزة مضافة حاليًا. اذهب لعلامة الأجهزة للإضافة.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Lazy load of device cards
        items(devices, key = { it.id }) { device ->
            DeviceGridCard(
                device = device,
                tariff = settingsState.tariffPerKWh,
                onToggle = { viewModel.toggleDeviceSwitch(device) },
                onClickCard = {
                    onNavigateToDevices(device.id)
                }
            )
        }
    }

    // Settings Dialog for custom configuring
    if (showSettingsDialog) {
        val isScanningEsp by viewModel.isScanningEsp.collectAsState()
        val scanResult by viewModel.scanResult.collectAsState()
        
        var ipAddress by remember { mutableStateOf(settingsState.espIpAddress) }
        var isSimMode by remember { mutableStateOf(settingsState.isSimulatorEnabled) }
        var showManualIpInput by remember { mutableStateOf(false) }

        // Sync local ipAddress when automatic scan succeeds
        LaunchedEffect(scanResult, settingsState.espIpAddress) {
            if (scanResult == "success") {
                ipAddress = settingsState.espIpAddress
            }
        }

        AlertDialog(
            onDismissRequest = { 
                viewModel.resetScanResult()
                showSettingsDialog = false 
            },
            title = {
                Text(
                    text = "إعدادات الربط وقيمة التعرفة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Connect Mode selection
                    Text(
                        text = "نمط الاتصال بدارة المستشعر:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val simColor = ButtonDefaults.buttonColors(containerColor = if (isSimMode) ElectricTeal else CyberSlate)
                        val liveColor = ButtonDefaults.buttonColors(containerColor = if (!isSimMode) ElectricCyan else CyberSlate)
                        
                        Button(
                            onClick = { isSimMode = true },
                            modifier = Modifier.weight(1f),
                            colors = simColor
                        ) {
                            Text("وضع المحاكاة", color = if (isSimMode) CyberNavy else Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { isSimMode = false },
                            modifier = Modifier.weight(1f),
                            colors = liveColor
                        ) {
                            Text("ربط ESP8266/32", color = if (!isSimMode) CyberNavy else Color.White, fontSize = 12.sp)
                        }
                    }

                    if (!isSimMode) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberNavy),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الاقتران والربط التلقائي",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (scanResult == "success") ElectricTeal.copy(alpha = 0.15f) else BorderDark
                                    ) {
                                        Text(
                                            text = if (scanResult == "success") "متصل تلقائياً" else "مستعد للاكتشاف",
                                            color = if (scanResult == "success") ElectricTeal else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (isScanningEsp) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = ElectricCyan,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "جاري مسح الشبكة المحلية والبحث عن الدارة...",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    when (scanResult) {
                                        "success" -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(ElectricTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, ElectricTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "نجاح",
                                                    tint = ElectricTeal,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "تم الاقتران بالدارة الذكية بنجاح!",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "عنوان الـ IP المكتشف: ${settingsState.espIpAddress}",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                        "failed" -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(EnergyRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, EnergyRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = "ملاحظة",
                                                        tint = EnergyRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "لم يتم اكتشاف الدارة تلقائياً",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = "تأكد من تشغيل الدارة واتصال هاتفك بالـ Wi-Fi، أو اتصل بنقطة البث والاقتران الخاصة بدارة ESP مباشرة.",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.startAutomaticEspDiscovery() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Wifi,
                                                contentDescription = null,
                                                tint = CyberNavy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "🔎 ابحث واقترن تلقائياً الآن",
                                                color = CyberNavy,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showManualIpInput = !showManualIpInput }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showManualIpInput) "إخفاء عنوان IP اليدوي" else "إدخال IP يدوياً بشكل احتياطي؟",
                                        color = ElectricCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = if (showManualIpInput) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (showManualIpInput) {
                                    OutlinedTextField(
                                        value = ipAddress,
                                        onValueChange = { ipAddress = it },
                                        label = { Text("عنوان IP يدوي احتياطي") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = ElectricCyan,
                                            unfocusedBorderColor = BorderDark
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberNavy.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "تسعير الاستهلاك والتعرفة الحالية:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• أول شريحة (0.01 kWh الأولى) بتعادل 600 ل.س.\n• الشريحة التالية (ما بعد 0.01 kWh) بتعادل 1400 ل.س لكل 0.01 kWh إضافية.\n\nتتم هذه العملية بالكامل بدقة متناهية.",
                                color = ElectricTeal,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSystemSettings(
                            ipAddress,
                            isSimMode,
                            140000.0, // tariff price per kWh for reference
                            0.01,     // limit1 is 0.01 kWh
                            60000.0,  // price1 is 60,000 SYP/kWh (600 per 0.01 kWh)
                            140000.0  // price2 is 140,000 SYP/kWh (1400 per 0.01 kWh)
                        )
                        viewModel.resetScanResult()
                        showSettingsDialog = false
                    },
                    colors = ButtonColors(ElectricCyan, CyberNavy, ElectricCyan, CyberNavy)
                ) {
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.resetScanResult()
                    showSettingsDialog = false 
                }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSlate
        )
    }
}

@Composable
fun DeviceGridCard(
    device: DeviceEntity,
    tariff: Double,
    onToggle: () -> Unit,
    onClickCard: () -> Unit
) {
    val cost = device.costSyp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickCard() }
            .testTag("device_card_${device.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        border = BorderStroke(1.dp, if (device.isOn) ElectricCyan.copy(alpha = 0.5f) else BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Indicator Light
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (device.isOn) ElectricTeal else Color.Gray)
                    )

                    Text(
                        text = device.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Switch Slider to Toggle
                Switch(
                    checked = device.isOn,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberNavy,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = BorderDark
                    ),
                    modifier = Modifier.testTag("device_switch_${device.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberNavy.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cost
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "السعر",
                        tint = NeonGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "%,.1f ل.س", cost),
                        color = NeonGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // KW.h
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "قراءة",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "%.3f kWh", device.energyWh / 1000.0),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Real-time Power Output indicator when device is running
            if (device.isOn) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الاستهلاك المباشر:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f W (%.2f A)", device.activePower, device.current),
                        color = ElectricTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
