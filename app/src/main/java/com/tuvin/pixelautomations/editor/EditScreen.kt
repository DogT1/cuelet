package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t
import com.tuvin.pixelautomations.setup.dayLetter
import com.tuvin.pixelautomations.setup.weekOrder

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.BtDevice
import com.tuvin.pixelautomations.model.LevelCondition
import com.tuvin.pixelautomations.model.AlarmEvent
import com.tuvin.pixelautomations.model.Apps
import com.tuvin.pixelautomations.model.SleepPhase
import com.tuvin.pixelautomations.model.Place
import com.tuvin.pixelautomations.model.SolarEvent
import com.tuvin.pixelautomations.model.TimeWindow
import com.tuvin.pixelautomations.model.offsetLabel
import com.tuvin.pixelautomations.model.solarOffsets
import com.tuvin.pixelautomations.triggers.Locations
import com.tuvin.pixelautomations.actions.ShizukuShell
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.tuvin.pixelautomations.model.RunMode
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.setup.Permissions
import com.tuvin.pixelautomations.triggers.WifiTrigger
import kotlin.math.roundToInt

/** The trigger types offered in the editor, with their iOS names. */
enum class Kind(val label: String) {
    TIME("Time of Day"), CHARGER("Charger"), BATTERY("Battery Level"), LOW_POWER("Battery Saver"),
    AIRPLANE("Airplane Mode"), BLUETOOTH("Bluetooth"), WIFI("Wi-Fi"), AUTO("Android Auto"),
    ARRIVE("Arrive"), LEAVE("Leave"), COMMUTE("Before I Commute"),
    EMAIL("Email"), MESSAGE("Message"), ALARM("Alarm"), SLEEP("Sleep"), FOCUS("Do Not Disturb"),
    TRANSACTION("Transaction"), SOUND("Sound Recognition"), APP("App"), NFC("NFC"), WORKOUT("Workout");

    companion object {
        fun of(t: Trigger?): Kind = when (t) {
            null, is Trigger.TimeOfDay -> TIME
            is Trigger.Charger -> CHARGER
            is Trigger.BatteryLevel -> BATTERY
            is Trigger.LowPowerMode -> LOW_POWER
            is Trigger.AirplaneMode -> AIRPLANE
            is Trigger.Bluetooth -> BLUETOOTH
            is Trigger.WiFi -> WIFI
            is Trigger.AndroidAuto -> AUTO
            is Trigger.Arrive -> ARRIVE
            is Trigger.Leave -> LEAVE
            is Trigger.BeforeCommute -> COMMUTE
            is Trigger.Email -> EMAIL
            is Trigger.Message -> MESSAGE
            is Trigger.Alarm -> ALARM
            is Trigger.Sleep -> SLEEP
            is Trigger.Focus -> FOCUS
            is Trigger.Transaction -> TRANSACTION
            is Trigger.SoundRecognition -> SOUND
            is Trigger.App -> APP
            is Trigger.Nfc -> NFC
            is Trigger.Workout -> WORKOUT
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    initial: Automation?,
    onCancel: () -> Unit,
    onDone: (Automation) -> Unit,
    onDelete: (Automation) -> Unit,
    others: List<Automation> = emptyList(),
    onOpenConnections: () -> Unit = {},
    resumeTick: Int = 0,
    onRunNow: (Automation) -> Unit = {},
) {
    val ctx = LocalContext.current
    // The phone's own default texting app (Samsung Messages on Galaxy, Google Messages on Pixel, etc.).
    val defaultSms = remember { android.provider.Telephony.Sms.getDefaultSmsPackage(ctx) ?: Apps.MESSAGES }
    val start = initial?.trigger
    var kind by remember { mutableStateOf(Kind.of(start)) }
    // New automations start with the iOS trigger list; existing ones open straight in the editor.
    var pickingTrigger by remember { mutableStateOf(initial == null) }
    var pickedOnce by remember { mutableStateOf(initial != null) }

    // Per-trigger option state, seeded from the automation being edited.
    val time = rememberTimePickerState(
        initialHour = (start as? Trigger.TimeOfDay)?.hour ?: 8,
        initialMinute = (start as? Trigger.TimeOfDay)?.minute ?: 0,
        is24Hour = true,
    )
    var days by remember { mutableStateOf((start as? Trigger.TimeOfDay)?.days ?: emptySet()) }
    var chargerConnected by remember { mutableStateOf((start as? Trigger.Charger)?.connected ?: true) }
    var levelCondition by remember { mutableStateOf((start as? Trigger.BatteryLevel)?.condition ?: LevelCondition.FALLS_BELOW) }
    var levelPercent by remember { mutableIntStateOf((start as? Trigger.BatteryLevel)?.percent ?: 20) }
    var lowPowerOn by remember { mutableStateOf((start as? Trigger.LowPowerMode)?.on ?: true) }
    var airplaneOn by remember { mutableStateOf((start as? Trigger.AirplaneMode)?.on ?: true) }
    var btDevices by remember { mutableStateOf((start as? Trigger.Bluetooth)?.devices ?: emptyList()) }
    var btConnected by remember { mutableStateOf((start as? Trigger.Bluetooth)?.connected ?: true) }
    var wifiNetworks by remember { mutableStateOf((start as? Trigger.WiFi)?.networks ?: emptyList()) }
    var wifiJoins by remember { mutableStateOf((start as? Trigger.WiFi)?.joins ?: true) }
    var wifiEntry by remember { mutableStateOf("") }
    var autoConnects by remember { mutableStateOf((start as? Trigger.AndroidAuto)?.connects ?: true) }

    // Time of Day: fixed time, sunrise or sunset.
    val startTime = start as? Trigger.TimeOfDay
    var solar by remember { mutableStateOf(startTime?.solar) }
    var solarOffset by remember { mutableIntStateOf(startTime?.offsetMinutes ?: 0) }
    var solarPlace by remember {
        mutableStateOf(startTime?.lat?.let { lat -> Place("Here", lat, startTime.lng ?: 0.0) })
    }

    // Arrive / Leave.
    var place by remember {
        mutableStateOf((start as? Trigger.Arrive)?.place ?: (start as? Trigger.Leave)?.place)
    }
    val startWindow = (start as? Trigger.Arrive)?.window ?: (start as? Trigger.Leave)?.window
    var windowOn by remember { mutableStateOf(startWindow != null) }
    val windowFrom = rememberTimePickerState((startWindow?.fromMinute ?: 480) / 60, (startWindow?.fromMinute ?: 480) % 60, true)
    val windowTo = rememberTimePickerState((startWindow?.toMinute ?: 1200) / 60, (startWindow?.toMinute ?: 1200) % 60, true)

    // Before I Commute.
    val startCommute = start as? Trigger.BeforeCommute
    var origin by remember { mutableStateOf(startCommute?.origin) }
    var destination by remember { mutableStateOf(startCommute?.destination) }
    val depart = rememberTimePickerState(startCommute?.departHour ?: 8, startCommute?.departMinute ?: 0, true)
    var minutesBefore by remember { mutableIntStateOf(startCommute?.minutesBefore ?: 0) }
    var commuteDays by remember { mutableStateOf(startCommute?.days ?: emptySet()) }

    // Notification-based triggers.
    val startEmail = start as? Trigger.Email
    val startMessage = start as? Trigger.Message
    var senders by remember { mutableStateOf((startEmail?.senders ?: startMessage?.senders ?: emptyList()).joinToString(", ")) }
    var containsText by remember { mutableStateOf(startEmail?.subjectContains ?: startMessage?.contains ?: "") }
    var mailApps by remember { mutableStateOf(startEmail?.apps ?: listOf(Apps.GMAIL)) }
    var chatApps by remember { mutableStateOf(startMessage?.apps ?: listOf(defaultSms, Apps.WHATSAPP)) }
    var alarmEvent by remember { mutableStateOf((start as? Trigger.Alarm)?.event ?: AlarmEvent.STOPPED) }
    var sleepPhase by remember { mutableStateOf((start as? Trigger.Sleep)?.phase ?: SleepPhase.BEDTIME) }
    val sleepHours = (start as? Trigger.Sleep)?.hours ?: TimeWindow(21 * 60, 9 * 60)
    val sleepFrom = rememberTimePickerState(sleepHours.fromMinute / 60, sleepHours.fromMinute % 60, true)
    val sleepTo = rememberTimePickerState(sleepHours.toMinute / 60, sleepHours.toMinute % 60, true)
    var focusOn by remember { mutableStateOf((start as? Trigger.Focus)?.on ?: true) }
    var merchant by remember { mutableStateOf((start as? Trigger.Transaction)?.merchantContains ?: "") }
    var sounds by remember { mutableStateOf((start as? Trigger.SoundRecognition)?.sounds ?: emptyList()) }
    var soundEntry by remember { mutableStateOf("") }
    fun senderList() = senders.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    // App and NFC.
    val startApp = start as? Trigger.App
    var pickedApps by remember { mutableStateOf(startApp?.apps ?: emptyList()) }
    var appOpened by remember { mutableStateOf(startApp?.opened ?: true) }
    var appClosed by remember { mutableStateOf(startApp?.closed ?: false) }
    var tagId by remember { mutableStateOf((start as? Trigger.Nfc)?.tagId) }
    var tagName by remember { mutableStateOf((start as? Trigger.Nfc)?.name ?: "") }
    var writeTag by remember { mutableStateOf(false) }

    // Galaxy Watch workout.
    var workoutTypes by remember { mutableStateOf((start as? Trigger.Workout)?.types ?: emptyList()) }
    var workoutStarts by remember { mutableStateOf((start as? Trigger.Workout)?.starts ?: true) }

    var actions by remember { mutableStateOf(initial?.actions ?: listOf<Action>(Action.ShowNotification(""))) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var runMode by remember { mutableStateOf(initial?.runMode ?: RunMode.AFTER_CONFIRMATION) }
    var notify by remember { mutableStateOf(initial?.notifyWhenRun ?: true) }

    // Bumped after a permission answer so the "needs" card and device list refresh.
    var permissionTick by remember { mutableIntStateOf(0) }
    val askPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionTick++ }

    fun window(): TimeWindow? =
        if (!windowOn) null else TimeWindow(windowFrom.hour * 60 + windowFrom.minute, windowTo.hour * 60 + windowTo.minute)

    /** The trigger as edited, or null while a required choice (a place) is still missing. */
    fun trigger(): Trigger? = when (kind) {
        Kind.TIME -> Trigger.TimeOfDay(
            time.hour, time.minute, days,
            solar = solar,
            offsetMinutes = if (solar == null) 0 else solarOffset,
            lat = if (solar == null) null else solarPlace?.lat,
            lng = if (solar == null) null else solarPlace?.lng,
        ).takeIf { solar == null || solarPlace != null }
        Kind.ARRIVE -> place?.let { Trigger.Arrive(it, window()) }
        Kind.LEAVE -> place?.let { Trigger.Leave(it, window()) }
        Kind.COMMUTE -> {
            val o = origin
            val d = destination
            if (o == null || d == null) null
            else Trigger.BeforeCommute(o, d, depart.hour, depart.minute, minutesBefore, commuteDays)
        }
        Kind.EMAIL -> Trigger.Email(senderList(), containsText.trim(), mailApps).takeIf { mailApps.isNotEmpty() }
        Kind.MESSAGE -> Trigger.Message(senderList(), containsText.trim(), chatApps).takeIf { chatApps.isNotEmpty() }
        Kind.ALARM -> Trigger.Alarm(alarmEvent)
        Kind.SLEEP -> Trigger.Sleep(
            sleepPhase,
            TimeWindow(sleepFrom.hour * 60 + sleepFrom.minute, sleepTo.hour * 60 + sleepTo.minute),
        )
        Kind.FOCUS -> Trigger.Focus(focusOn)
        Kind.TRANSACTION -> Trigger.Transaction(merchant.trim())
        Kind.SOUND -> Trigger.SoundRecognition(sounds)
        Kind.APP -> Trigger.App(pickedApps, appOpened, appClosed)
            .takeIf { pickedApps.isNotEmpty() && (appOpened || appClosed) }
        Kind.WORKOUT -> Trigger.Workout(workoutTypes, workoutStarts)
        Kind.NFC -> tagId?.let { Trigger.Nfc(it, tagName.trim().ifEmpty { "NFC tag" }) }
        Kind.CHARGER -> Trigger.Charger(chargerConnected)
        Kind.BATTERY -> Trigger.BatteryLevel(levelCondition, levelPercent)
        Kind.LOW_POWER -> Trigger.LowPowerMode(lowPowerOn)
        Kind.AIRPLANE -> Trigger.AirplaneMode(airplaneOn)
        Kind.BLUETOOTH -> Trigger.Bluetooth(btDevices, btConnected)
        Kind.WIFI -> Trigger.WiFi(wifiNetworks, wifiJoins)
        Kind.AUTO -> Trigger.AndroidAuto(autoConnects)
    }

    fun build(): Automation {
        val trig = trigger() ?: error("Done is disabled until the trigger is complete")
        return (initial ?: Automation(trigger = trig)).copy(
            name = name.trim(), trigger = trig, actions = actions, runMode = runMode, notifyWhenRun = notify,
        )
    }

    if (pickingTrigger) {
        TriggerPicker(
            onPick = { kind = it; pickingTrigger = false; pickedOnce = true },
            // Cancel on the first pick leaves the new automation; later it just closes the list.
            onCancel = { if (pickedOnce) pickingTrigger = false else onCancel() },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(if (initial == null) "New Automation" else "Edit Automation")) },
                navigationIcon = { TextButton(onClick = onCancel) { Text(t("Cancel")) } },
                actions = {
                    val ready = trigger() != null && actions.isNotEmpty() && actions.all { it.isComplete() }
                    TextButton(onClick = { onDone(build()) }, enabled = ready) { Text(t("Done")) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("When") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                            .background(tileColorForKind(kind)),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(
                            kindIcon(kind), null, tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        t(kind.label), style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 14.dp),
                    )
                    TextButton(onClick = { pickingTrigger = true }) { Text(t("Change")) }
                }
            }

            Section(kind.label) {
                when (kind) {
                    Kind.TIME -> {
                        Choice("Time of Day", solar == null) { solar = null }
                        Choice("Sunrise", solar == SolarEvent.SUNRISE) { solar = SolarEvent.SUNRISE }
                        Choice("Sunset", solar == SolarEvent.SUNSET) { solar = SolarEvent.SUNSET }
                        if (solar == null) {
                            TimeField("Time", time)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                solarOffsets.forEach { m ->
                                    FilterChip(selected = solarOffset == m, onClick = { solarOffset = m }, label = { Text(offsetLabel(m)) })
                                }
                            }
                            val sp = solarPlace
                            Text(
                                if (sp == null) t("Uses your location to work out the times.") else t("Times for %.3f, %.3f", sp.lat, sp.lng),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            val scope = rememberCoroutineScope()
                            OutlinedButton(onClick = {
                                scope.launch {
                                    Locations.current(ctx)?.let { solarPlace = Place("Here", it.latitude, it.longitude) }
                                }
                            }) { Text(t(if (sp == null) "Use current location" else "Update location")) }
                        }
                        DayPicker(days) { days = it }
                    }
                    Kind.ARRIVE, Kind.LEAVE -> {
                        PlacePicker("Location", place) { place = it }
                        Choice("Any Time", !windowOn) { windowOn = false }
                        Choice("Time Range", windowOn) { windowOn = true }
                        if (windowOn) {
                            TimeField("From", windowFrom)
                            TimeField("To", windowTo)
                        }
                    }
                    Kind.COMMUTE -> {
                        Text(
                            t("Android can't predict when you leave, so set the time you usually go."),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        PlacePicker("From (usually Home)", origin) { origin = it }
                        PlacePicker("To (usually Work)", destination) { destination = it }
                        TimeField("I usually leave at", depart)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 5, 10, 15, 30, 60).forEach { m ->
                                FilterChip(
                                    selected = minutesBefore == m,
                                    onClick = { minutesBefore = m },
                                    label = { Text(if (m == 0) t("At departure") else t("%d min before", m)) },
                                )
                            }
                        }
                        DayPicker(commuteDays) { commuteDays = it }
                    }
                    Kind.EMAIL, Kind.MESSAGE -> {
                        val isEmail = kind == Kind.EMAIL
                        OutlinedTextField(
                            senders, { senders = it },
                            label = { Text(t("From (names or addresses, comma separated; empty = anyone)")) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            containsText, { containsText = it },
                            label = { Text(t(if (isEmail) "Subject contains" else "Message contains")) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(t("Apps"), style = MaterialTheme.typography.labelLarge)
                        val options = if (isEmail) listOf(Apps.GMAIL, Apps.OUTLOOK) else listOf(defaultSms, Apps.WHATSAPP, Apps.TELEGRAM).distinct()
                        val chosen = if (isEmail) mailApps else chatApps
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { pkg ->
                                val on = pkg in chosen
                                FilterChip(selected = on, label = { Text(appLabel(ctx, pkg)) }, onClick = {
                                    val next = if (on) chosen - pkg else chosen + pkg
                                    if (isEmail) mailApps = next else chatApps = next
                                })
                            }
                        }
                    }
                    Kind.ALARM -> {
                        Choice("Goes Off", alarmEvent == AlarmEvent.GOES_OFF) { alarmEvent = AlarmEvent.GOES_OFF }
                        Choice("Is Snoozed", alarmEvent == AlarmEvent.SNOOZED) { alarmEvent = AlarmEvent.SNOOZED }
                        Choice("Is Stopped", alarmEvent == AlarmEvent.STOPPED) { alarmEvent = AlarmEvent.STOPPED }
                        Text(t("Any alarm in the Clock app."), style = MaterialTheme.typography.bodySmall)
                    }
                    Kind.SLEEP -> {
                        Choice("Bedtime Begins", sleepPhase == SleepPhase.BEDTIME) { sleepPhase = SleepPhase.BEDTIME }
                        Choice("Waking Up", sleepPhase == SleepPhase.WAKING_UP) { sleepPhase = SleepPhase.WAKING_UP }
                        Text(t("Bedtime hours"), style = MaterialTheme.typography.labelLarge)
                        TimeField("From", sleepFrom)
                        TimeField("To", sleepTo)
                        Text(
                            t("Fires when Bedtime mode (or any Mode / Do Not Disturb) turns on or off within these hours. Android doesn't tell apps which Mode it was. iOS's Wind Down has no Android equivalent."),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Kind.FOCUS -> {
                        Choice("When Turning On", focusOn) { focusOn = true }
                        Choice("When Turning Off", !focusOn) { focusOn = false }
                        Text(
                            t("Do Not Disturb or any Mode. Android doesn't tell apps which Mode it was."),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Kind.TRANSACTION -> {
                        OutlinedTextField(
                            merchant, { merchant = it },
                            label = { Text(t("Merchant contains (empty = any payment)")) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(t("Google Wallet payment notifications."), style = MaterialTheme.typography.bodySmall)
                    }
                    Kind.APP -> {
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = appOpened, role = Role.Checkbox, onClick = { appOpened = !appOpened }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Checkbox(checked = appOpened, onCheckedChange = null)
                            Text(t("Is Opened"))
                        }
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = appClosed, role = Role.Checkbox, onClick = { appClosed = !appClosed }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Checkbox(checked = appClosed, onCheckedChange = null)
                            Text(t("Is Closed"))
                        }
                        Text(
                            t("Opened includes switching to the app; closed includes switching away, as on iPhone."),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        AppPicker(pickedApps) { pickedApps = it }
                    }
                    Kind.WORKOUT -> {
                        Choice("Starts", workoutStarts) { workoutStarts = true }
                        Choice("Ends", !workoutStarts) { workoutStarts = false }
                        Text(t("Workout type"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.tuvin.pixelautomations.model.workoutTypes.forEach { type ->
                                val on = type in workoutTypes
                                FilterChip(
                                    selected = on,
                                    onClick = { workoutTypes = if (on) workoutTypes - type else workoutTypes + type },
                                    label = { Text(t(com.tuvin.pixelautomations.model.workoutName(type))) },
                                )
                            }
                        }
                        if (workoutTypes.isEmpty()) Text(t("None picked: any workout"), style = MaterialTheme.typography.bodySmall)
                        Text(
                            t("Needs the Automations app on your Galaxy Watch (installed from the PC)."),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Kind.NFC -> {
                        NfcScanner(tagId, writeTag, onWriteChange = { writeTag = it }, onScanned = { tagId = it })
                        OutlinedTextField(tagName, { tagName = it }, label = { Text(t("Tag name")) }, modifier = Modifier.fillMaxWidth())
                    }
                    Kind.SOUND -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sounds.forEach { s ->
                                InputChip(selected = true, onClick = { sounds = sounds - s }, label = { Text("$s  ✕") })
                            }
                        }
                        if (sounds.isEmpty()) Text(t("None added: any sound"), style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(soundEntry, { soundEntry = it }, label = { Text(t("Sound word")) }, modifier = Modifier.weight(1f))
                            TextButton(enabled = soundEntry.isNotBlank(), onClick = { sounds = sounds + soundEntry.trim(); soundEntry = "" }) {
                                Text(t("Add"))
                            }
                        }
                        Text(
                            t("Uses Android's Sound Notifications app (turn it on in Accessibility). Type the word as its alerts show it in your phone's language, e.g. “doorbell” or “פעמון”."),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Kind.CHARGER -> {
                        Choice("Is Connected", chargerConnected) { chargerConnected = true }
                        Choice("Is Disconnected", !chargerConnected) { chargerConnected = false }
                    }
                    Kind.BATTERY -> {
                        Choice("Equals", levelCondition == LevelCondition.EQUALS) { levelCondition = LevelCondition.EQUALS }
                        Choice("Rises Above", levelCondition == LevelCondition.RISES_ABOVE) { levelCondition = LevelCondition.RISES_ABOVE }
                        Choice("Falls Below", levelCondition == LevelCondition.FALLS_BELOW) { levelCondition = LevelCondition.FALLS_BELOW }
                        Text("$levelPercent%", style = MaterialTheme.typography.headlineSmall)
                        Slider(
                            value = levelPercent.toFloat(),
                            onValueChange = { levelPercent = it.roundToInt() },
                            valueRange = 1f..99f,
                        )
                    }
                    Kind.LOW_POWER -> {
                        Choice("Is Turned On", lowPowerOn) { lowPowerOn = true }
                        Choice("Is Turned Off", !lowPowerOn) { lowPowerOn = false }
                    }
                    Kind.AIRPLANE -> {
                        Choice("Is Turned On", airplaneOn) { airplaneOn = true }
                        Choice("Is Turned Off", !airplaneOn) { airplaneOn = false }
                    }
                    Kind.BLUETOOTH -> {
                        Choice("Is Connected", btConnected) { btConnected = true }
                        Choice("Is Disconnected", !btConnected) { btConnected = false }
                        Text(t("Device"), style = MaterialTheme.typography.labelLarge)
                        val paired = remember(permissionTick) { pairedDevices(ctx) }
                        if (paired == null) {
                            Text(t("Allow Nearby devices to pick from your paired devices."), style = MaterialTheme.typography.bodySmall)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                paired.forEach { d ->
                                    val on = btDevices.any { it.address == d.address }
                                    FilterChip(
                                        selected = on,
                                        onClick = { btDevices = if (on) btDevices.filterNot { it.address == d.address } else btDevices + d },
                                        label = { Text(d.name) },
                                    )
                                }
                            }
                            if (btDevices.isEmpty()) Text(t("None picked: any device"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Kind.WIFI -> {
                        Choice("Joins", wifiJoins) { wifiJoins = true }
                        Choice("Leaves", !wifiJoins) { wifiJoins = false }
                        Text(t("Network"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            wifiNetworks.forEach { n ->
                                InputChip(selected = true, onClick = { wifiNetworks = wifiNetworks - n }, label = { Text("$n  ✕") })
                            }
                        }
                        if (wifiNetworks.isEmpty()) Text(t("None added: any network"), style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(wifiEntry, { wifiEntry = it }, label = { Text(t("Network name")) }, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { wifiNetworks = wifiNetworks + wifiEntry.trim(); wifiEntry = "" },
                                enabled = wifiEntry.isNotBlank(),
                            ) { Text(t("Add")) }
                        }
                        val currentSsid = remember(permissionTick) { currentSsid(ctx) }
                        if (currentSsid != null && currentSsid !in wifiNetworks) {
                            OutlinedButton(onClick = { wifiNetworks = wifiNetworks + currentSsid }) { Text("Add current: $currentSsid") }
                        }
                    }
                    Kind.AUTO -> {
                        Choice("Connects", autoConnects) { autoConnects = true }
                        Choice("Disconnects", !autoConnects) { autoConnects = false }
                    }
                }
            }

            // "Needs setup": permissions this trigger needs but doesn't have yet.
            val missing = remember(kind, wifiNetworks, solar, solarPlace, actions, permissionTick, resumeTick) {
                // Place-based triggers need location before a place can even be picked, so check the kind itself.
                val probe = trigger() ?: when (kind) {
                    Kind.ARRIVE, Kind.LEAVE, Kind.COMMUTE -> Trigger.Arrive(Place("", 0.0, 0.0))
                    else -> Trigger.Charger() // needs nothing; still lets the actions be checked
                }
                Permissions.missing(ctx, Automation(trigger = probe, actions = actions))
            }
            if (missing.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t("Needs: ") + missing.joinToString { Permissions.describe(it) }, style = MaterialTheme.typography.titleSmall)
                        if (Permissions.ACCESSIBILITY in missing) {
                            Text(
                                t("In Accessibility, open “Automations: App trigger” (under Downloaded apps) and turn it on. If it's greyed out: App info › ⋮ › Allow restricted settings, then try again."),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (Permissions.NOTIFICATION_ACCESS in missing) {
                            Text(
                                t("Turn on Automations in the list that opens. If the switch is greyed out: App info › ⋮ › Allow restricted settings, then try again."),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (Permissions.SHIZUKU in missing && !ShizukuShell.available()) {
                            Text(
                                t("Open the Shizuku app and start it first (needed again after each reboot), then tap Allow."),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(onClick = {
                            missing.firstOrNull(Permissions::isInApp)?.let {
                                onOpenConnections()
                                return@OutlinedButton
                            }
                            if (Permissions.SHIZUKU in missing) {
                                ShizukuShell.requestPermission()
                                return@OutlinedButton
                            }
                            // Special permissions are switched on in Settings, not through a pop-up.
                            missing.firstOrNull(Permissions::isSpecial)?.let { special ->
                                val intent = Permissions.settingsIntent(ctx, special)
                                // Fall back to the main Settings screen rather than crash if a page is refused.
                                runCatching { intent?.let(ctx::startActivity) }
                                    .onFailure { runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)) } }
                                return@OutlinedButton
                            }
                            // Android asks for background location separately, after foreground location.
                            val ask = if (Permissions.FINE_LOCATION in missing) {
                                arrayOf(Permissions.FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            } else {
                                missing.toTypedArray()
                            }
                            askPermissions.launch(ask)
                        }) { Text(t("Allow")) }
                    }
                }
            }

            Section("Do") {
                ActionsEditor(actions, others.filter { it.id != initial?.id }) { actions = it }
            }

            Section("Options") {
                Choice("Run After Confirmation", runMode == RunMode.AFTER_CONFIRMATION) { runMode = RunMode.AFTER_CONFIRMATION }
                Choice("Run Immediately", runMode == RunMode.IMMEDIATELY) { runMode = RunMode.IMMEDIATELY }
                if (runMode == RunMode.IMMEDIATELY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t("Notify When Run"), Modifier.weight(1f))
                        Switch(checked = notify, onCheckedChange = { notify = it })
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text(t("Name (optional)")) }, modifier = Modifier.fillMaxWidth())
            }

            // Run the actions as they are now (saved or not), like tapping ▶ on iOS.
            val runnable = actions.isNotEmpty() && actions.all { it.isComplete() }
            OutlinedButton(
                onClick = {
                    val trig = trigger() ?: Trigger.Charger()
                    onRunNow((initial ?: Automation(trigger = trig)).copy(name = name.trim().ifEmpty { t("Run Now") }, trigger = trig, actions = actions))
                },
                enabled = runnable,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("▶  Run Now")) }

            if (initial != null) {
                TextButton(onClick = { onDelete(initial) }, modifier = Modifier.fillMaxWidth()) {
                    Text(t("Delete Automation"), color = Color(0xFFE5484D))
                }
            }
        }
    }
}

/** Paired Bluetooth devices, or null when the Nearby devices permission is missing. */
@SuppressLint("MissingPermission")
internal fun pairedDevices(ctx: Context): List<BtDevice>? {
    if (!Permissions.granted(ctx, Permissions.NEARBY)) return null
    val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
    return runCatching {
        adapter.bondedDevices.map { BtDevice(it.address, it.name ?: it.address) }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())
}

/** The Wi-Fi network the phone is on now, if location access lets us read its name. */
@Suppress("DEPRECATION")
private fun currentSsid(ctx: Context): String? {
    if (!Permissions.granted(ctx, Permissions.FINE_LOCATION)) return null
    val info = ctx.getSystemService(WifiManager::class.java)?.connectionInfo ?: return null
    return WifiTrigger.normalize(info.ssid)
}

@Composable
private fun DayPicker(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Text(t("Repeat"), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        weekOrder().forEach { d ->
            FilterChip(
                selected = d in days,
                onClick = { onChange(if (d in days) days - d else days + d) },
                label = { Text(dayLetter(d)) },
            )
        }
    }
    if (days.isEmpty()) Text(t("No days picked: runs every day"), style = MaterialTheme.typography.bodySmall)
}

/** A package's display name: the known label, else the installed app's own label, else the package id. */
private fun appLabel(ctx: android.content.Context, pkg: String): String =
    Apps.names[pkg] ?: runCatching {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrNull() ?: pkg

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                t(title).uppercase(), style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    // The whole row is the tap target, as on iOS.
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(t(label), Modifier.padding(start = 8.dp))
    }
}
