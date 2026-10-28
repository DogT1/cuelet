package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.ui.TileColors
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.model.label
import com.tuvin.pixelautomations.setup.Permissions
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

fun iconFor(t: Trigger): ImageVector = when (t) {
    is Trigger.TimeOfDay -> when (t.solar) {
        null -> Icons.Default.Schedule
        com.tuvin.pixelautomations.model.SolarEvent.SUNRISE -> Icons.Default.WbSunny
        com.tuvin.pixelautomations.model.SolarEvent.SUNSET -> Icons.Default.WbTwilight
    }
    is Trigger.Arrive -> Icons.Default.Place
    is Trigger.Leave -> Icons.Default.DirectionsWalk
    is Trigger.BeforeCommute -> Icons.Default.Commute
    is Trigger.Email -> Icons.Default.Email
    is Trigger.Message -> Icons.Default.Chat
    is Trigger.Alarm -> Icons.Default.Alarm
    is Trigger.Sleep -> Icons.Default.Bedtime
    is Trigger.Focus -> Icons.Default.DoNotDisturbOn
    is Trigger.Transaction -> Icons.Default.Payment
    is Trigger.SoundRecognition -> Icons.Default.Hearing
    is Trigger.App -> Icons.Default.Apps
    is Trigger.Nfc -> Icons.Default.Nfc
    is Trigger.Workout -> Icons.Default.FitnessCenter
    is Trigger.Charger -> Icons.Default.BatteryChargingFull
    is Trigger.BatteryLevel -> Icons.Default.Battery3Bar
    is Trigger.LowPowerMode -> Icons.Default.BatterySaver
    is Trigger.AirplaneMode -> Icons.Default.AirplanemodeActive
    is Trigger.Bluetooth -> Icons.Default.Bluetooth
    is Trigger.WiFi -> Icons.Default.Wifi
    is Trigger.AndroidAuto -> Icons.Default.DirectionsCar
}

/** The colour of an automation's icon tile, grouped so the list reads at a glance. */
fun tileColorFor(t: Trigger): Color = when (t) {
    is Trigger.TimeOfDay, is Trigger.Alarm, is Trigger.Sleep, is Trigger.Workout -> TileColors.Time
    is Trigger.Arrive, is Trigger.Leave, is Trigger.BeforeCommute, is Trigger.AndroidAuto -> TileColors.Place
    is Trigger.Email, is Trigger.Message -> TileColors.Comms
    is Trigger.WiFi, is Trigger.Bluetooth, is Trigger.AirplaneMode, is Trigger.Charger,
    is Trigger.BatteryLevel, is Trigger.LowPowerMode, is Trigger.Focus -> TileColors.Connect
    is Trigger.App, is Trigger.Nfc, is Trigger.SoundRecognition, is Trigger.Transaction -> TileColors.Device
}

/** A rounded, colour-filled tile with the trigger's glyph. */
@Composable
private fun TriggerTile(trigger: Trigger, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(tileColorFor(trigger)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(iconFor(trigger), contentDescription = null, tint = Color.White, modifier = Modifier.size((size * 0.56f).dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    automations: List<Automation>,
    batteryRestricted: Boolean,
    onNew: () -> Unit,
    onOpen: (Automation) -> Unit,
    onToggle: (Automation, Boolean) -> Unit,
    onLog: () -> Unit,
    onFixBattery: () -> Unit,
    refreshKey: Int = 0,
    onSettings: () -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
) {
    var menuOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
    val ctx = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Cuelet") },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, t("Settings")) }
                    IconButton(onClick = onLog) { Icon(Icons.Default.History, t("Run log")) }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, t("More")) }
                    androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text(t("Export automations…")) }, onClick = { menuOpen = false; onExport() })
                        androidx.compose.material3.DropdownMenuItem(text = { Text(t("Import automations…")) }, onClick = { menuOpen = false; onImport() })
                    }
                },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(t("New Automation")) },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        ) {
            if (batteryRestricted) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(t("Let automations run in the background"), style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text(t("Android may delay triggers while battery optimisation is on for this app."),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            OutlinedButton(onClick = onFixBattery) { Text(t("Allow")) }
                        }
                    }
                }
            }
            if (automations.isEmpty()) item { EmptyState() }
            items(automations, key = { it.id }) { a ->
                val missing = remember(a, refreshKey) { Permissions.missing(ctx, a) }
                AutomationRow(a, missing, onOpen = { onOpen(a) }, onToggle = { onToggle(a, it) })
            }
        }
    }
}

@Composable
private fun AutomationRow(a: Automation, missing: List<String>, onOpen: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TriggerTile(a.trigger)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    a.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (a.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (a.actions.isEmpty()) t("No actions") else a.actions.joinToString { it.label() },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (missing.isNotEmpty()) {
                    Row(
                        Modifier.padding(top = 6.dp).clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Text(
                            t("Needs setup: %s", missing.joinToString { Permissions.describe(it) }),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(start = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Switch(checked = a.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Bolt, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            t("No automations yet"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            t("Have your phone react to changes in conditions. Tap New Automation to start."),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
        )
    }
}
