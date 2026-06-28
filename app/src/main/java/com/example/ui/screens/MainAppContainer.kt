package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.LoadManagerViewModel
import com.example.ui.viewmodel.valTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: LoadManagerViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val logs by viewModel.logs.collectAsState()

    // Dialog state collectors
    val showMutualExclusion by viewModel.showMutualExclusionDialog.collectAsState()
    val showManualOverride by viewModel.showManualOverrideDialog.collectAsState()
    val showMaxCostDeactivation by viewModel.showMaxCostDeactivationDialog.collectAsState()
    val showManualModeTransitionWarning by viewModel.showManualModeTransitionWarningDialog.collectAsState()
    
    val showProtectionBlocked by viewModel.showProtectionBlockedDialog.collectAsState()
    val protectionBlockedMsg by viewModel.protectionBlockedMessage.collectAsState()
    val showStateChangeConfirm by viewModel.showStateChangeConfirmDialog.collectAsState()
    val stateChangeConfirmMsg by viewModel.stateChangeConfirmMessage.collectAsState()
    val showSecondsPlanBlocked by viewModel.showSecondsPlanBlockedDialog.collectAsState()

    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()
    val currentDevice = devices.find { it.id == selectedDeviceId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberNavy,
        bottomBar = {
            // Elegant integrated bottom navigation bar
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .testTag("app_bottom_bar"),
                containerColor = CyberSlate,
                tonalElevation = 8.dp
            ) {
                // TAB 1: HOME
                NavigationBarItem(
                    selected = currentTab == valTab.HOME,
                    onClick = { viewModel.selectTab(valTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberNavy,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_home_tab")
                )

                // TAB 2: DEVICES
                NavigationBarItem(
                    selected = currentTab == valTab.DEVICES,
                    onClick = { viewModel.selectTab(valTab.DEVICES) },
                    icon = { Icon(Icons.Default.Layers, contentDescription = "الأجهزة") },
                    label = { Text("الأجهزة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberNavy,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_devices_tab")
                )

                // TAB 3: ANALYTICS
                NavigationBarItem(
                    selected = currentTab == valTab.ANALYTICS,
                    onClick = { viewModel.selectTab(valTab.ANALYTICS) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "التحليلات") },
                    label = { Text("التحليلات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberNavy,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_analytics_tab")
                )

                // TAB 4: ALERTS
                NavigationBarItem(
                    selected = currentTab == valTab.ALERTS,
                    onClick = { viewModel.selectTab(valTab.ALERTS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (alerts.isNotEmpty()) {
                                    Badge(containerColor = EnergyRed) {
                                        Text(alerts.size.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "التنبيهات")
                        }
                    },
                    label = { Text("التنبيهات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberNavy,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_alerts_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                valTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    devices = devices,
                    onNavigateToDevices = { deviceId ->
                        viewModel.selectDevice(deviceId)
                        viewModel.selectTab(valTab.DEVICES)
                    }
                )
                valTab.DEVICES -> DevicesScreen(
                    viewModel = viewModel,
                    devices = devices
                )
                valTab.ANALYTICS -> AnalyticsScreen(
                    viewModel = viewModel,
                    devices = devices,
                    logs = logs
                )
                valTab.ALERTS -> AlertsScreen(
                    viewModel = viewModel,
                    alerts = alerts
                )
            }
        }
    }

    // ----------------- DIALOG OVERLAYS -----------------

    // 1. Time Plans Mutual Exclusion Dialog
    if (showMutualExclusion) {
        AlertDialog(
            onDismissRequest = { viewModel.showMutualExclusionDialog.value = false },
            title = {
                Text(
                    text = "خطأ في تشغيل المهام الزمنية",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "لا يمكن عمل الخطتين الزمنيتين معًا (جدول الساعات المجدول ومؤقت الثواني النشط). يرجى إلغاء إحدى الخطط أولاً لضمان عدم حدوث تعارض في الدورة الزمنية.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.showMutualExclusionDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("تم", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CyberSlate
        )
    }

    // 2. Manual Switch Override Dialog
    if (showManualOverride) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelManualOverride() },
            title = {
                Text(
                    text = "تحذير إلغاء جدولة الأتمتة النشطة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "سيتم الانتقال للوضع اليدوي في حال تغيير حالة الجهاز يدويًا. سيؤدي هذا لتجميد وإيقاف كافة خطط الأتمتة النشطة (مثل الحد المالي والمؤقتات الساعية أو الثواني) لهذا الجهاز مؤقتًا.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmManualOverride() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("أوافق", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelManualOverride() }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSlate
        )
    }

    // 3. Max Cost Deactivation Warning Dialog
    if (showMaxCostDeactivation && currentDevice != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelMaxCostDeactivation() },
            title = {
                Text(
                    text = "تعارض مع الحد الأقصى للتكلفة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "سيتم ايقاف الحد الاقصى للتكلفة",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmMaxCostDeactivationAndStartPlan(currentDevice) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("موافق", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelMaxCostDeactivation() }) {
                    Text("الغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSlate
        )
    }

    // 4. Manual Mode Transition Warning Dialog
    if (showManualModeTransitionWarning && currentDevice != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelManualModeTransition() },
            title = {
                Text(
                    text = "تنبيه الانتقال للوضع اليدوي",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "سيتم ايقاف جميع الخطط الزمنية مؤقتا في حال الانتقال للوضع اليدوي",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmManualModeTransition() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("موافق", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelManualModeTransition() }) {
                    Text("الغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSlate
        )
    }

    // 5. Protection Blocked Dialog
    if (showProtectionBlocked) {
        AlertDialog(
            onDismissRequest = { viewModel.showProtectionBlockedDialog.value = false },
            title = {
                Text(
                    text = "حماية التجهيزة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = protectionBlockedMsg,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.showProtectionBlockedDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("تم", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CyberSlate
        )
    }

    // 6. State Change Confirmation Dialog
    if (showStateChangeConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.showStateChangeConfirmDialog.value = false },
            title = {
                Text(
                    text = "تأكيد الإجراء",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stateChangeConfirmMsg,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deviceToToggle?.let { viewModel.executeToggleDevice(it) }
                        viewModel.showStateChangeConfirmDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("تأكيد", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showStateChangeConfirmDialog.value = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CyberSlate
        )
    }

    // 7. Seconds Plan Blocked Warning Dialog
    if (showSecondsPlanBlocked) {
        AlertDialog(
            onDismissRequest = { viewModel.showSecondsPlanBlockedDialog.value = false },
            title = {
                Text(
                    text = "تنبيه حماية الأجهزة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "لا يمكن تفعيل خطة الثواني على هذا النوع من التجهيزات لاغراض الحماية",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.showSecondsPlanBlockedDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("تم", color = CyberNavy, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CyberSlate
        )
    }
}
