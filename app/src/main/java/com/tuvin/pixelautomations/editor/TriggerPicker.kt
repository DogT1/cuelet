package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.tuvin.pixelautomations.ui.TileColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** The iOS "New Automation" list, in iOS's groups. */
private val groups: List<Pair<String, List<Kind>>> = listOf(
    "Event" to listOf(Kind.TIME, Kind.ALARM, Kind.SLEEP, Kind.WORKOUT),
    "Travel" to listOf(Kind.ARRIVE, Kind.LEAVE, Kind.COMMUTE, Kind.AUTO),
    "Communication" to listOf(Kind.EMAIL, Kind.MESSAGE),
    "Transaction" to listOf(Kind.TRANSACTION),
    "Setting" to listOf(
        Kind.AIRPLANE, Kind.WIFI, Kind.BLUETOOTH, Kind.FOCUS, Kind.LOW_POWER, Kind.BATTERY,
        Kind.CHARGER, Kind.NFC, Kind.APP, Kind.SOUND,
    ),
)

/** One-line explanation under each trigger, like iOS's subtitles. */
private fun hint(k: Kind): String = when (k) {
    Kind.TIME -> "At a time of day, sunrise or sunset"
    Kind.ALARM -> "When an alarm goes off, is snoozed or stopped"
    Kind.SLEEP -> "When Bedtime begins or you wake up"
    Kind.ARRIVE -> "When you arrive somewhere"
    Kind.LEAVE -> "When you leave somewhere"
    Kind.COMMUTE -> "Before your usual departure"
    Kind.AUTO -> "When Android Auto connects or disconnects"
    Kind.EMAIL -> "When you get an email"
    Kind.MESSAGE -> "When you get a message"
    Kind.TRANSACTION -> "When you pay with Google Wallet"
    Kind.AIRPLANE -> "When Airplane Mode turns on or off"
    Kind.WIFI -> "When the phone joins or leaves Wi-Fi"
    Kind.BLUETOOTH -> "When a Bluetooth device connects or disconnects"
    Kind.FOCUS -> "When Do Not Disturb turns on or off"
    Kind.LOW_POWER -> "When Battery Saver turns on or off"
    Kind.BATTERY -> "When the battery reaches a level"
    Kind.CHARGER -> "When a charger connects or disconnects"
    Kind.NFC -> "When you tap an NFC tag"
    Kind.APP -> "When an app is opened or closed"
    Kind.SOUND -> "When the phone recognises a sound"
    Kind.WORKOUT -> "When a workout starts or ends on your watch"
}

fun kindIcon(k: Kind): ImageVector = when (k) {
    Kind.TIME -> Icons.Default.Schedule
    Kind.ALARM -> Icons.Default.Alarm
    Kind.SLEEP -> Icons.Default.Bedtime
    Kind.ARRIVE -> Icons.Default.Place
    Kind.LEAVE -> Icons.Default.DirectionsWalk
    Kind.COMMUTE -> Icons.Default.Commute
    Kind.AUTO -> Icons.Default.DirectionsCar
    Kind.EMAIL -> Icons.Default.Email
    Kind.MESSAGE -> Icons.Default.Chat
    Kind.TRANSACTION -> Icons.Default.Payment
    Kind.AIRPLANE -> Icons.Default.AirplanemodeActive
    Kind.WIFI -> Icons.Default.Wifi
    Kind.BLUETOOTH -> Icons.Default.Bluetooth
    Kind.FOCUS -> Icons.Default.DoNotDisturbOn
    Kind.LOW_POWER -> Icons.Default.BatterySaver
    Kind.BATTERY -> Icons.Default.Battery3Bar
    Kind.CHARGER -> Icons.Default.BatteryChargingFull
    Kind.NFC -> Icons.Default.Nfc
    Kind.APP -> Icons.Default.Apps
    Kind.SOUND -> Icons.Default.Hearing
    Kind.WORKOUT -> Icons.Default.FitnessCenter
}

/** Same colour grouping as the automation list, so a trigger looks the same here and there. */
fun tileColorForKind(k: Kind): Color = when (k) {
    Kind.TIME, Kind.ALARM, Kind.SLEEP, Kind.WORKOUT -> TileColors.Time
    Kind.ARRIVE, Kind.LEAVE, Kind.COMMUTE, Kind.AUTO -> TileColors.Place
    Kind.EMAIL, Kind.MESSAGE -> TileColors.Comms
    Kind.AIRPLANE, Kind.WIFI, Kind.BLUETOOTH, Kind.FOCUS, Kind.LOW_POWER, Kind.BATTERY, Kind.CHARGER -> TileColors.Connect
    Kind.TRANSACTION, Kind.NFC, Kind.APP, Kind.SOUND -> TileColors.Device
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerPicker(onPick: (Kind) -> Unit, onCancel: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(t("New Automation")) },
            navigationIcon = { TextButton(onClick = onCancel) { Text(t("Cancel")) } },
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            groups.forEach { (group, kinds) ->
                item(key = group) {
                    Text(
                        t(group).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(kinds, key = { _, k -> k.name }) { i, k ->
                    // Each group is one rounded container; rows join with matching corners (iOS-style inset list).
                    val shape = RoundedCornerShape(
                        topStart = if (i == 0) 18.dp else 4.dp, topEnd = if (i == 0) 18.dp else 4.dp,
                        bottomStart = if (i == kinds.lastIndex) 18.dp else 4.dp, bottomEnd = if (i == kinds.lastIndex) 18.dp else 4.dp,
                    )
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { onPick(k) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tileColorForKind(k)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(kindIcon(k), null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(t(k.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(t(hint(k)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
