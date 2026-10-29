package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t
import com.tuvin.pixelautomations.setup.dayLetter
import com.tuvin.pixelautomations.setup.weekOrder

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.AppRef
import com.tuvin.pixelautomations.model.BtDevice
import com.tuvin.pixelautomations.model.Internet
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.Condition
import com.tuvin.pixelautomations.model.MediaCommand
import com.tuvin.pixelautomations.model.SendMode
import com.tuvin.pixelautomations.model.SendVia
import com.tuvin.pixelautomations.model.MenuOption
import com.tuvin.pixelautomations.model.Radio
import com.tuvin.pixelautomations.model.RingerMode
import com.tuvin.pixelautomations.model.Toggle
import com.tuvin.pixelautomations.model.VolumeStream
import com.tuvin.pixelautomations.model.label
import kotlin.math.roundToInt

/** A new action of each kind, grouped like the iOS action picker. */
private val catalog: List<Pair<String, List<Pair<String, () -> Action>>>> = listOf(
    "Notify & speak" to listOf(
        "Show Notification" to { Action.ShowNotification("") },
        "Speak Text" to { Action.Speak("") },
        "Vibrate" to { Action.Vibrate() },
        "Send SMS" to { Action.SendSms() },
    ),
    "Apps & web" to listOf(
        "Open App" to { Action.OpenApp(AppRef("", "")) },
        "Open URL" to { Action.OpenUrl("https://") },
        "Webhook (Get Contents of URL)" to { Action.Webhook("http://") },
        "Copy to Clipboard" to { Action.CopyToClipboard("") },
    ),
    "Home" to listOf(
        "Home Assistant service" to { Action.HomeAssistant("light.turn_on") },
    ),
    "Sound & screen" to listOf(
        "Set Volume" to { Action.SetVolume() },
        "Set Ringer" to { Action.SetRinger() },
        "Do Not Disturb" to { Action.SetDnd() },
        "Flashlight" to { Action.Flashlight() },
        "Brightness" to { Action.Brightness() },
        "Media Play/Pause/Skip" to { Action.Media() },
    ),
    "Connections (Shizuku)" to listOf(
        "Wi-Fi / Bluetooth / Data / Airplane" to { Action.SetRadio(Radio.WIFI) },
        "Force-stop App" to { Action.ForceStop(AppRef("", "")) },
    ),
    "Location & camera" to listOf(
        "Send Location" to { Action.SendLocation() },
        "Capture Photo" to { Action.CapturePhoto() },
        "Record Audio" to { Action.RecordAudio() },
    ),
    "Device" to listOf(
        "Lock Screen" to { Action.LockScreen },
        "Take Screenshot" to { Action.TakeScreenshot },
        "Set Alarm" to { Action.SetAlarm() },
        "Start Timer" to { Action.StartTimer() },
    ),
    "Flow" to listOf(
        "If / Otherwise" to { Action.If() },
        "Choose from Menu" to { Action.ChooseFromMenu("", listOf(MenuOption("Yes"), MenuOption("No"))) },
        "Wait" to { Action.Wait() },
        "Run Another Automation" to { Action.RunAutomation(0, "") },
    ),
)

/** Conditions offered in If, grouped like the action catalog, with a fresh default for each. */
private val conditionCatalog: List<Pair<String, List<Pair<String, () -> Condition>>>> = listOf(
    "Power & device" to listOf(
        "Battery above" to { Condition.BatteryAbove() },
        "Battery below" to { Condition.BatteryBelow() },
        "Charging" to { Condition.Charging() },
        "Battery saver" to { Condition.BatterySaver() },
        "Airplane mode" to { Condition.Airplane() },
        "Dark mode" to { Condition.DarkMode() },
    ),
    "Connections" to listOf(
        "On Wi-Fi network" to { Condition.WifiIs() },
        "Internet" to { Condition.InternetVia() },
        "Bluetooth device" to { Condition.BluetoothConnected() },
        "Headphones" to { Condition.Headphones() },
    ),
    "Sound & calls" to listOf(
        "Do Not Disturb" to { Condition.DndOn() },
        "Ringer" to { Condition.Ringer() },
        "Volume" to { Condition.Volume() },
        "Music playing" to { Condition.MusicPlaying() },
        "On a call" to { Condition.OnCall() },
    ),
    "Place & screen" to listOf(
        "At a place" to { Condition.AtPlace() },
        "Phone locked" to { Condition.Locked() },
        "App in front" to { Condition.AppInFront() },
    ),
    "Time" to listOf(
        "Time between" to { Condition.TimeBetween() },
        "Day of week" to { Condition.Weekday() },
    ),
    "Chance" to listOf(
        "Random chance" to { Condition.Chance() },
    ),
)

private val conditionChoices: List<Pair<String, () -> Condition>> get() = conditionCatalog.flatMap { it.second }

/** The catalog name of a condition (by type). */
private fun conditionName(c: Condition): String = conditionChoices.first { it.second()::class == c::class }.first

private fun conditionIcon(name: String): ImageVector = when (name) {
    "Battery above" -> Icons.Default.BatteryFull
    "Battery below" -> Icons.Default.BatteryAlert
    "Charging" -> Icons.Default.BatteryChargingFull
    "Battery saver" -> Icons.Default.BatterySaver
    "Airplane mode" -> Icons.Default.AirplanemodeActive
    "Dark mode" -> Icons.Default.DarkMode
    "On Wi-Fi network" -> Icons.Default.Wifi
    "Internet" -> Icons.Default.Public
    "Bluetooth device" -> Icons.Default.Bluetooth
    "Headphones" -> Icons.Default.Headphones
    "Do Not Disturb" -> Icons.Default.DoNotDisturbOn
    "Ringer" -> Icons.Default.Vibration
    "Volume" -> Icons.Default.VolumeUp
    "Music playing" -> Icons.Default.MusicNote
    "On a call" -> Icons.Default.Call
    "At a place" -> Icons.Default.Place
    "Phone locked" -> Icons.Default.Lock
    "App in front" -> Icons.Default.Apps
    "Time between" -> Icons.Default.Schedule
    "Day of week" -> Icons.Default.CalendarToday
    "Random chance" -> Icons.Default.Casino
    else -> Icons.Default.CallSplit
}

/** Whether an action has what it needs to run (used to enable Done). */
fun Action.isComplete(): Boolean = when (this) {
    is Action.ShowNotification -> title.isNotBlank()
    is Action.OpenApp -> app.pkg.isNotBlank()
    is Action.OpenUrl -> url.contains("://") && url.substringAfter("://").isNotBlank()
    is Action.Webhook -> url.startsWith("http") && url.substringAfter("://").isNotBlank()
    is Action.HomeAssistant -> service.contains('.')
    is Action.Speak -> text.isNotBlank()
    is Action.CopyToClipboard -> text.isNotEmpty()
    is Action.RunAutomation -> automationId != 0L
    is Action.ForceStop -> app.pkg.isNotBlank()
    is Action.If -> condition.isComplete() && (then + otherwise).all { it.isComplete() }
    is Action.ChooseFromMenu -> options.size in 1..3 && options.all { o -> o.label.isNotBlank() && o.actions.all { it.isComplete() } }
    is Action.CapturePhoto -> front || back
    is Action.SendLocation -> via == SendVia.SMS || to.isNotBlank()
    else -> true
}

/** Whether a condition has what it needs: a network name, a place or an app. */
fun Condition.isComplete(): Boolean = when (this) {
    is Condition.WifiIs -> ssid.isNotBlank()
    is Condition.AtPlace -> place != null
    is Condition.AppInFront -> app.pkg.isNotBlank()
    else -> true
}

private fun sendViaLabel(via: SendVia): String = when (via) {
    SendVia.SMS -> "SMS"
    SendVia.WHATSAPP -> "WhatsApp"
    SendVia.TELEGRAM -> "Telegram"
}

private fun sendModeLabel(mode: SendMode): String = when (mode) {
    SendMode.PREFILLED -> "Open, I tap send"
    SendMode.AUTO -> "Auto-send"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionsEditor(
    actions: List<Action>,
    others: List<Automation>,
    topLevel: Boolean = true,
    onChange: (List<Action>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEachIndexed { i, action ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}. " + t(titleOf(action)), style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(enabled = i > 0, onClick = { onChange(actions.swap(i, i - 1)) }) { Icon(Icons.Default.ArrowUpward, t("Move up")) }
                        IconButton(enabled = i < actions.lastIndex, onClick = { onChange(actions.swap(i, i + 1)) }) {
                            Icon(Icons.Default.ArrowDownward, t("Move down"))
                        }
                        IconButton(onClick = { onChange(actions.filterIndexed { j, _ -> j != i }) }) { Icon(Icons.Default.Close, t("Remove")) }
                    }
                    ActionFields(action, others) { updated -> onChange(actions.mapIndexed { j, a -> if (j == i) updated else a }) }
                }
            }
        }
        AddActionButton(allowMenu = topLevel) { onChange(actions + it) }
    }
}

private fun List<Action>.swap(a: Int, b: Int) = toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }

private fun titleOf(a: Action): String = catalog.flatMap { it.second }
    .firstOrNull { (_, make) -> make()::class == a::class }?.first ?: a.label()

/** Each action group gets a tile colour, so the picker reads by category at a glance. */
private fun groupColor(group: String): Color = when (group) {
    "Notify & speak" -> Color(0xFF0D9488)        // teal
    "Apps & web" -> Color(0xFF4F46E5)            // indigo
    "Home" -> Color(0xFF0891B2)                  // cyan
    "Sound & screen" -> Color(0xFF7C3AED)        // violet
    "Connections (Shizuku)" -> Color(0xFFEA580C) // orange
    "Location & camera" -> Color(0xFF059669)     // emerald
    "Device" -> Color(0xFF475569)                // slate
    // Condition groups
    "Power & device" -> Color(0xFF475569)        // slate
    "Connections" -> Color(0xFF4F46E5)           // indigo
    "Sound & calls" -> Color(0xFF7C3AED)         // violet
    "Place & screen" -> Color(0xFF059669)        // emerald
    "Time" -> Color(0xFF0891B2)                  // cyan
    "Chance" -> Color(0xFFDB2777)                // pink
    else -> Color(0xFFD97706)                    // amber — Flow
}

private fun actionIcon(name: String): ImageVector = when (name) {
    "Show Notification" -> Icons.Default.Notifications
    "Speak Text" -> Icons.Default.RecordVoiceOver
    "Vibrate" -> Icons.Default.Vibration
    "Open App" -> Icons.Default.Launch
    "Open URL" -> Icons.Default.Link
    "Webhook (Get Contents of URL)" -> Icons.Default.Http
    "Copy to Clipboard" -> Icons.Default.ContentCopy
    "Home Assistant service" -> Icons.Default.Home
    "Set Volume" -> Icons.Default.VolumeUp
    "Set Ringer" -> Icons.Default.NotificationsActive
    "Do Not Disturb" -> Icons.Default.DoNotDisturbOn
    "Flashlight" -> Icons.Default.FlashlightOn
    "Brightness" -> Icons.Default.BrightnessMedium
    "Media Play/Pause/Skip" -> Icons.Default.PlayArrow
    "Wi-Fi / Bluetooth / Data / Airplane" -> Icons.Default.SettingsInputAntenna
    "Force-stop App" -> Icons.Default.Block
    "Send SMS" -> Icons.Default.Sms
    "Send Location" -> Icons.Default.LocationOn
    "Capture Photo" -> Icons.Default.PhotoCamera
    "Record Audio" -> Icons.Default.Mic
    "Lock Screen" -> Icons.Default.Lock
    "Take Screenshot" -> Icons.Default.Screenshot
    "Set Alarm" -> Icons.Default.Alarm
    "Start Timer" -> Icons.Default.Timer
    "If / Otherwise" -> Icons.Default.CallSplit
    "Choose from Menu" -> Icons.Default.Menu
    "Wait" -> Icons.Default.HourglassEmpty
    "Run Another Automation" -> Icons.Default.PlayCircle
    else -> Icons.Default.Add
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddActionButton(allowMenu: Boolean, onAdd: (Action) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) {
        Icon(Icons.Default.Add, null)
        Text(t("Add Action"))
    }
    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheetState) {
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "__title") {
                    Text(
                        t("Add Action"), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                }
                catalog.forEach { (group, items) ->
                    // "Choose from Menu" waits for a tap, which only works at the top level of an automation.
                    val rows = items.filter { (name, _) -> allowMenu || name != "Choose from Menu" }
                    if (rows.isEmpty()) return@forEach
                    val color = groupColor(group)
                    item(key = "h_$group") {
                        Text(
                            t(group).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            color = color, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
                        )
                    }
                    itemsIndexed(rows, key = { _, r -> "$group:${r.first}" }) { i, (name, make) ->
                        val shape = RoundedCornerShape(
                            topStart = if (i == 0) 18.dp else 4.dp, topEnd = if (i == 0) 18.dp else 4.dp,
                            bottomStart = if (i == rows.lastIndex) 18.dp else 4.dp, bottomEnd = if (i == rows.lastIndex) 18.dp else 4.dp,
                        )
                        Row(
                            Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { onAdd(make()); open = false }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(color),
                                contentAlignment = Alignment.Center,
                            ) { Icon(actionIcon(name), null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                            Text(
                                t(name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A labelled branch of an If: an accent dot + caps label, with a matching vertical rail down its content. */
@Composable
private fun Branch(label: String, color: Color, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(
                t(label).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = color, modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.45f)))
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onPick(o) }, label = { Text(t(label(o))) }) }
    }
}

@Composable
private fun Field(value: String, label: String, singleLine: Boolean = true, onValue: (String) -> Unit) =
    OutlinedTextField(value, onValue, label = { Text(t(label)) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth())

@Composable
private fun PercentSlider(percent: Int, onValue: (Int) -> Unit) {
    Text("$percent%")
    Slider(value = percent.toFloat(), onValueChange = { onValue(it.roundToInt()) }, valueRange = 0f..100f)
}

private fun toggleLabel(t: Toggle) = when (t) {
    Toggle.ON -> "Turn on"
    Toggle.OFF -> "Turn off"
    Toggle.TOGGLE -> "Toggle"
}

@Composable
private fun ActionFields(a: Action, others: List<Automation>, update: (Action) -> Unit) {
    when (a) {
        is Action.ShowNotification -> {
            Field(a.title, "Title") { update(a.copy(title = it)) }
            Field(a.body, "Body", singleLine = false) { update(a.copy(body = it)) }
        }
        is Action.Speak -> Field(a.text, "Text to speak", singleLine = false) { update(a.copy(text = it)) }
        is Action.Vibrate -> Text(t("Vibrates for half a second."), style = MaterialTheme.typography.bodySmall)
        is Action.OpenApp -> SingleAppPicker(a.app) { update(a.copy(app = it)) }
        is Action.ForceStop -> SingleAppPicker(a.app) { update(a.copy(app = it)) }
        is Action.OpenUrl -> Field(a.url, "URL") { update(a.copy(url = it.trim())) }
        is Action.CopyToClipboard -> Field(a.text, "Text", singleLine = false) { update(a.copy(text = it)) }
        is Action.Webhook -> {
            Chips(listOf("GET", "POST", "PUT", "DELETE"), a.method, { it }) { update(a.copy(method = it)) }
            Field(a.url, "URL") { update(a.copy(url = it.trim())) }
            Field(a.headers, "Headers (one “Name: value” per line)", singleLine = false) { update(a.copy(headers = it)) }
            if (a.method != "GET") Field(a.body, "Body (JSON)", singleLine = false) { update(a.copy(body = it)) }
        }
        is Action.HomeAssistant -> {
            Field(a.service, "Service (e.g. light.turn_on)") { update(a.copy(service = it.trim())) }
            Field(a.entityId, "Entity (e.g. light.living_room)") { update(a.copy(entityId = it.trim())) }
            Field(a.extraJson, "Extra data JSON (optional), e.g. {\"brightness_pct\": 40}", singleLine = false) {
                update(a.copy(extraJson = it))
            }
        }
        is Action.SetVolume -> {
            Chips(VolumeStream.entries, a.stream, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { update(a.copy(stream = it)) }
            PercentSlider(a.percent) { update(a.copy(percent = it)) }
        }
        is Action.SetRinger -> Chips(RingerMode.entries, a.mode, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) {
            update(a.copy(mode = it))
        }
        is Action.SetDnd -> Chips(listOf(true, false), a.on, { if (it) "Turn on" else "Turn off" }) { update(a.copy(on = it)) }
        is Action.Flashlight -> Chips(Toggle.entries, a.mode, ::toggleLabel) { update(a.copy(mode = it)) }
        is Action.Brightness -> PercentSlider(a.percent) { update(a.copy(percent = it.coerceAtLeast(1))) }
        is Action.Media -> Chips(MediaCommand.entries, a.command, {
            when (it) {
                MediaCommand.PLAY_PAUSE -> "Play/Pause"
                MediaCommand.PLAY -> "Play"
                MediaCommand.PAUSE -> "Pause"
                MediaCommand.NEXT -> "Next"
                MediaCommand.PREVIOUS -> "Previous"
            }
        }) { update(a.copy(command = it)) }
        is Action.Wait -> {
            Text("${a.seconds} s")
            Slider(value = a.seconds.toFloat(), onValueChange = { update(a.copy(seconds = it.roundToInt())) }, valueRange = 1f..300f)
        }
        is Action.RunAutomation -> {
            if (others.isEmpty()) Text(t("No other automations yet."), style = MaterialTheme.typography.bodySmall)
            Chips(others.map { it.id to it.title }, a.automationId to a.name, { it.second }) { (id, name) ->
                update(a.copy(automationId = id, name = name))
            }
        }
        is Action.SendSms -> {
            Field(a.to, "To (phone number)") { update(a.copy(to = it.trim())) }
            Field(a.body, "Message (optional)", singleLine = false) { update(a.copy(body = it)) }
            Row(Modifier.fillMaxWidth().toggleable(a.includeLocation, role = Role.Checkbox) { update(a.copy(includeLocation = it)) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = a.includeLocation, onCheckedChange = null); Text(t("Append my location (Maps link)"))
            }
            Text(t("Texts silently, no unlock. SMS is text only — use Capture Photo for photos."), style = MaterialTheme.typography.bodySmall)
        }
        is Action.SendLocation -> {
            val ctx = LocalContext.current
            val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                val uri = res.data?.data ?: return@rememberLauncherForActivityResult
                runCatching {
                    ctx.contentResolver.query(
                        uri,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                        null, null, null,
                    )?.use { c ->
                        if (c.moveToFirst()) {
                            val number = (c.getString(0) ?: "").filter { it.isDigit() || it == '+' }
                            update(a.copy(to = number, toName = c.getString(1) ?: ""))
                        }
                    }
                }
            }
            Text(t("Send via"), style = MaterialTheme.typography.labelLarge)
            Chips(SendVia.entries.toList(), a.via, { sendViaLabel(it) }) { update(a.copy(via = it)) }

            OutlinedButton(onClick = {
                pickContact.launch(Intent(Intent.ACTION_PICK).apply { type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE })
            }) {
                Icon(Icons.Default.Person, null)
                Text(if (a.toName.isNotBlank() || a.to.isNotBlank()) t("Change contact") else t("Pick a contact"))
            }
            if (a.to.isNotBlank()) {
                Text(
                    listOf(a.toName, a.to).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (a.via == SendVia.SMS) {
                Text(t("Texts a Maps link silently — no unlock, no tap."), style = MaterialTheme.typography.bodySmall)
            } else {
                Chips(SendMode.entries.toList(), a.mode, { sendModeLabel(it) }) { update(a.copy(mode = it)) }
                if (a.mode == SendMode.AUTO) {
                    Text(
                        t("Auto-send taps the send button for you using the App-trigger accessibility service. It can break when the app changes its screen, and the phone must be unlocked and awake."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(t("Opens the chat with the Maps link ready; you tap send."), style = MaterialTheme.typography.bodySmall)
                }
            }
            Field(a.note, "Note (optional)") { update(a.copy(note = it)) }
        }
        is Action.CapturePhoto -> {
            Row(Modifier.fillMaxWidth().toggleable(a.front, role = Role.Checkbox) { update(a.copy(front = it)) }, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(checked = a.front, onCheckedChange = null); Text(t("Front camera"))
            }
            Row(Modifier.fillMaxWidth().toggleable(a.back, role = Role.Checkbox) { update(a.copy(back = it)) }, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(checked = a.back, onCheckedChange = null); Text(t("Back camera"))
            }
            Text(t("Takes photos silently in the background and saves them to your gallery."), style = MaterialTheme.typography.bodySmall)
        }
        is Action.RecordAudio -> {
            Text("${a.seconds} ${t("seconds")}")
            Slider(value = a.seconds.toFloat(), onValueChange = { update(a.copy(seconds = it.roundToInt())) }, valueRange = 3f..60f)
            Text(t("Records from the mic and saves the file on the phone."), style = MaterialTheme.typography.bodySmall)
        }
        is Action.LockScreen -> Text(t("Locks the phone. Needs the App-trigger accessibility service on."), style = MaterialTheme.typography.bodySmall)
        is Action.TakeScreenshot -> Text(t("Needs the App-trigger accessibility service on."), style = MaterialTheme.typography.bodySmall)
        is Action.SetAlarm -> {
            TimeField("Time", a.hour * 60 + a.minute) { update(a.copy(hour = it / 60, minute = it % 60)) }
            Field(a.label, "Label (optional)") { update(a.copy(label = it)) }
        }
        is Action.StartTimer -> {
            Text("${a.seconds} ${t("seconds")}")
            Slider(value = a.seconds.toFloat(), onValueChange = { update(a.copy(seconds = it.roundToInt())) }, valueRange = 10f..3600f)
            Field(a.label, "Label (optional)") { update(a.copy(label = it)) }
        }
        is Action.If -> {
            // If / Then / Otherwise, as distinct colour-railed branches so the logic reads top to bottom.
            Branch("If", Color(0xFFD97706)) {
                ConditionPickerButton(a.condition) { update(a.copy(condition = it)) }
                ConditionFields(a.condition) { update(a.copy(condition = it)) }
            }
            Branch("Then", Color(0xFF16A34A)) {
                ActionsEditor(a.then, others, topLevel = false) { update(a.copy(then = it)) }
            }
            Branch("Otherwise", Color(0xFFEA580C)) {
                ActionsEditor(a.otherwise, others, topLevel = false) { update(a.copy(otherwise = it)) }
            }
        }
        is Action.ChooseFromMenu -> {
            Field(a.prompt, "Prompt (e.g. “Heading out?”)") { update(a.copy(prompt = it)) }
            a.options.forEachIndexed { i, o ->
                // Each menu choice is its own indigo-railed branch: its button label, then the actions it runs.
                Branch(t("Option %d", i + 1), Color(0xFF4F46E5)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            o.label, { label -> update(a.copy(options = a.options.mapIndexed { j, x -> if (j == i) x.copy(label = label) else x })) },
                            label = { Text("Button ${i + 1}") }, singleLine = true, modifier = Modifier.weight(1f),
                        )
                        IconButton(enabled = a.options.size > 1, onClick = { update(a.copy(options = a.options.filterIndexed { j, _ -> j != i })) }) {
                            Icon(Icons.Default.Close, t("Remove option"))
                        }
                    }
                    ActionsEditor(o.actions, others, topLevel = false) { acts ->
                        update(a.copy(options = a.options.mapIndexed { j, x -> if (j == i) x.copy(actions = acts) else x }))
                    }
                }
            }
            if (a.options.size < 3) {
                OutlinedButton(onClick = { update(a.copy(options = a.options + MenuOption(t("Option %d", a.options.size + 1)))) }) {
                    Text(t("Add option"))
                }
            }
            Text(
                t("Shows a notification with up to 3 buttons (Android's limit). The chosen one runs, then the actions below."),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        is Action.SetRadio -> {
            Chips(Radio.entries, a.radio, { it.label }) { update(a.copy(radio = it)) }
            Chips(Toggle.entries, a.mode, ::toggleLabel) { update(a.copy(mode = it)) }
            Text(
                t("With Shizuku running this switches by itself. Without it (Shizuku doesn't run on the Android 17 beta yet), the switch opens for you to tap."),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionFields(c: Condition, update: (Condition) -> Unit) {
    when (c) {
        is Condition.BatteryAbove -> PercentSlider(c.percent) { update(c.copy(percent = it)) }
        is Condition.BatteryBelow -> PercentSlider(c.percent) { update(c.copy(percent = it)) }
        is Condition.Charging -> Chips(listOf(true, false), c.yes, { if (it) "Is charging" else "Isn't charging" }) { update(c.copy(yes = it)) }
        is Condition.WifiIs -> Field(c.ssid, "Network name") { update(c.copy(ssid = it)) }
        is Condition.TimeBetween -> {
            TimeField("From", c.window.fromMinute) { update(c.copy(window = c.window.copy(fromMinute = it))) }
            TimeField("To", c.window.toMinute) { update(c.copy(window = c.window.copy(toMinute = it))) }
        }
        is Condition.DndOn -> Chips(listOf(true, false), c.yes, { if (it) "Is on" else "Is off" }) { update(c.copy(yes = it)) }
        is Condition.Weekday -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekOrder().forEach { d ->
                    FilterChip(selected = d in c.days, onClick = {
                        update(c.copy(days = if (d in c.days) c.days - d else c.days + d))
                    }, label = { Text(dayLetter(d)) })
                }
            }
        }
        is Condition.BatterySaver -> Chips(listOf(true, false), c.on, { if (it) "Is on" else "Is off" }) { update(c.copy(on = it)) }
        is Condition.Airplane -> Chips(listOf(true, false), c.on, { if (it) "Is on" else "Is off" }) { update(c.copy(on = it)) }
        is Condition.DarkMode -> Chips(listOf(true, false), c.on, { if (it) "Is on" else "Is off" }) { update(c.copy(on = it)) }
        is Condition.InternetVia -> Chips(Internet.entries, c.via, { when (it) { Internet.WIFI -> "Wi-Fi"; Internet.MOBILE -> "Mobile data"; Internet.NONE -> "No internet" } }) { update(c.copy(via = it)) }
        is Condition.BluetoothConnected -> {
            Chips(listOf(true, false), c.connected, { if (it) "Is connected" else "Isn't connected" }) { update(c.copy(connected = it)) }
            BluetoothDeviceChoice(c.device) { update(c.copy(device = it)) }
        }
        is Condition.Headphones -> Chips(listOf(true, false), c.connected, { if (it) "Are connected" else "Aren't connected" }) { update(c.copy(connected = it)) }
        is Condition.Ringer -> Chips(RingerMode.entries, c.mode, { it.name.lowercase() }) { update(c.copy(mode = it)) }
        is Condition.Volume -> {
            Chips(listOf(true, false), c.above, { if (it) "Above" else "Below" }) { update(c.copy(above = it)) }
            PercentSlider(c.percent) { update(c.copy(percent = it)) }
        }
        is Condition.MusicPlaying -> Chips(listOf(true, false), c.yes, { if (it) "Is playing" else "Isn't playing" }) { update(c.copy(yes = it)) }
        is Condition.OnCall -> Chips(listOf(true, false), c.yes, { if (it) "On a call" else "Not on a call" }) { update(c.copy(yes = it)) }
        is Condition.AtPlace -> {
            Chips(listOf(true, false), c.there, { if (it) "I'm there" else "I'm not there" }) { update(c.copy(there = it)) }
            PlacePicker("Place", c.place) { update(c.copy(place = it)) }
        }
        is Condition.Locked -> Chips(listOf(true, false), c.yes, { if (it) "Is locked" else "Is unlocked" }) { update(c.copy(yes = it)) }
        is Condition.AppInFront -> {
            Chips(listOf(true, false), c.yes, { if (it) "Is in front" else "Isn't in front" }) { update(c.copy(yes = it)) }
            SingleAppPicker(c.app) { update(c.copy(app = it)) }
            Text(t("Needs the App-trigger accessibility service on."), style = MaterialTheme.typography.bodySmall)
        }
        is Condition.Chance -> PercentSlider(c.percent) { update(c.copy(percent = it)) }
    }
}

/** The current condition's name; tapping it opens the grouped condition catalog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionPickerButton(current: Condition, onPick: (Condition) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val currentName = conditionName(current)
    OutlinedButton(onClick = { open = true }) {
        Icon(conditionIcon(currentName), null)
        Text(t(currentName), modifier = Modifier.padding(start = 8.dp))
        Icon(Icons.Default.ExpandMore, null)
    }
    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheetState) {
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "__title") {
                    Text(
                        t("Pick a condition"), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                }
                conditionCatalog.forEach { (group, rows) ->
                    val color = groupColor(group)
                    item(key = "h_$group") {
                        Text(
                            t(group).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            color = color, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
                        )
                    }
                    itemsIndexed(rows, key = { _, r -> "$group:${r.first}" }) { i, (name, make) ->
                        val shape = RoundedCornerShape(
                            topStart = if (i == 0) 18.dp else 4.dp, topEnd = if (i == 0) 18.dp else 4.dp,
                            bottomStart = if (i == rows.lastIndex) 18.dp else 4.dp, bottomEnd = if (i == rows.lastIndex) 18.dp else 4.dp,
                        )
                        val selected = name == currentName
                        Row(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { if (!selected) onPick(make()); open = false }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(color),
                                contentAlignment = Alignment.Center,
                            ) { Icon(conditionIcon(name), null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                            Text(
                                t(name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Any device" or one paired device; needs the Nearby devices permission to list them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BluetoothDeviceChoice(device: BtDevice?, onChange: (BtDevice?) -> Unit) {
    val ctx = LocalContext.current
    val paired = remember { pairedDevices(ctx) }
    if (paired == null) {
        Text(t("Allow Nearby devices to pick a device. Until then, any device counts."), style = MaterialTheme.typography.bodySmall)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = device == null, onClick = { onChange(null) }, label = { Text(t("Any device")) })
        paired.forEach { d ->
            FilterChip(selected = device?.address == d.address, onClick = { onChange(d) }, label = { Text(d.name) })
        }
    }
}

/** Pick one app (reuses the multi-app picker, keeping the newest choice). */
@Composable
private fun SingleAppPicker(app: AppRef, onPick: (AppRef) -> Unit) {
    if (app.pkg.isNotBlank()) Text(t("App: %s", app.name), style = MaterialTheme.typography.bodyMedium)
    AppPicker(if (app.pkg.isBlank()) emptyList() else listOf(app)) { picked -> picked.lastOrNull()?.let(onPick) }
}
