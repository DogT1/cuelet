package com.tuvin.pixelautomations.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.tuvin.pixelautomations.setup.t
import com.tuvin.pixelautomations.setup.weekOrder

/** What starts an automation. One subtype per iOS trigger; more are added phase by phase. */
@Serializable
sealed interface Trigger {
    /**
     * iOS "Time of Day". [days] are ISO day numbers (1 = Monday … 7 = Sunday); empty means every day.
     * With [solar] set it runs at sunrise or sunset (shifted by [offsetMinutes]) for the stored [lat]/[lng],
     * and [hour]/[minute] are ignored.
     */
    @Serializable
    @SerialName("time_of_day")
    data class TimeOfDay(
        val hour: Int,
        val minute: Int,
        val days: Set<Int> = emptySet(),
        val solar: SolarEvent? = null,
        val offsetMinutes: Int = 0,
        val lat: Double? = null,
        val lng: Double? = null,
    ) : Trigger

    /** iOS "Arrive": entering [place], optionally only within [window]. */
    @Serializable
    @SerialName("arrive")
    data class Arrive(val place: Place, val window: TimeWindow? = null) : Trigger

    /** iOS "Leave": exiting [place], optionally only within [window]. */
    @Serializable
    @SerialName("leave")
    data class Leave(val place: Place, val window: TimeWindow? = null) : Trigger

    /**
     * iOS "Before I Commute". iOS predicts the departure; here the user sets their usual departure time.
     * Runs [minutesBefore] before it on [days], only if the phone is at [origin] then.
     */
    @Serializable
    @SerialName("before_commute")
    data class BeforeCommute(
        val origin: Place,
        val destination: Place,
        val departHour: Int,
        val departMinute: Int,
        val minutesBefore: Int = 0,
        val days: Set<Int> = emptySet(),
    ) : Trigger

    /** iOS "Charger": Is Connected / Is Disconnected. */
    @Serializable
    @SerialName("charger")
    data class Charger(val connected: Boolean = true) : Trigger

    /** iOS "Battery Level": Equals / Rises Above / Falls Below a percentage. */
    @Serializable
    @SerialName("battery_level")
    data class BatteryLevel(val condition: LevelCondition = LevelCondition.FALLS_BELOW, val percent: Int = 20) : Trigger

    /** iOS "Low Power Mode" → Android Battery Saver: Is Turned On / Off. */
    @Serializable
    @SerialName("low_power")
    data class LowPowerMode(val on: Boolean = true) : Trigger

    /** iOS "Airplane Mode": Is Turned On / Off. */
    @Serializable
    @SerialName("airplane_mode")
    data class AirplaneMode(val on: Boolean = true) : Trigger

    /** iOS "Bluetooth": chosen device(s) (empty = any) Is Connected / Is Disconnected. */
    @Serializable
    @SerialName("bluetooth")
    data class Bluetooth(val devices: List<BtDevice> = emptyList(), val connected: Boolean = true) : Trigger

    /** iOS "Wi-Fi": chosen network(s) (empty = any) Joins / Leaves. */
    @Serializable
    @SerialName("wifi")
    data class WiFi(val networks: List<String> = emptyList(), val joins: Boolean = true) : Trigger

    /** iOS "CarPlay" → Android Auto: Connects / Disconnects. */
    @Serializable
    @SerialName("android_auto")
    data class AndroidAuto(val connects: Boolean = true) : Trigger

    // Notification-based triggers (stage 4). App package names live in [Apps].

    /** iOS "Email": from sender(s) (empty = anyone), subject contains, in the chosen mail apps. */
    @Serializable
    @SerialName("email")
    data class Email(
        val senders: List<String> = emptyList(),
        val subjectContains: String = "",
        val apps: List<String> = listOf(Apps.GMAIL),
    ) : Trigger

    /** iOS "Message": from sender(s) (empty = anyone), message contains, in the chosen chat apps. */
    @Serializable
    @SerialName("message")
    data class Message(
        val senders: List<String> = emptyList(),
        val contains: String = "",
        val apps: List<String> = listOf(Apps.MESSAGES, Apps.WHATSAPP),
    ) : Trigger

    /** iOS "Alarm": a Clock alarm Goes Off / Is Snoozed / Is Stopped. */
    @Serializable
    @SerialName("alarm")
    data class Alarm(val event: AlarmEvent = AlarmEvent.STOPPED) : Trigger

    /**
     * iOS "Sleep" → Pixel Bedtime mode: Bedtime Begins / Waking Up. On Android 15+ Bedtime is a Mode, and Android
     * doesn't tell apps which Mode turned on, so this fires when Do Not Disturb or a Mode turns on/off inside
     * the [hours] window (plus Digital Wellbeing's bedtime notification where a phone still posts one).
     */
    @Serializable
    @SerialName("sleep")
    data class Sleep(
        val phase: SleepPhase = SleepPhase.BEDTIME,
        val hours: TimeWindow = TimeWindow(21 * 60, 9 * 60),
    ) : Trigger

    /** iOS "Focus" → Do Not Disturb / any Mode turning on or off (Android doesn't say which Mode). */
    @Serializable
    @SerialName("focus")
    data class Focus(val on: Boolean = true) : Trigger

    /** iOS "Transaction" → a Google Wallet payment notification, optionally for a merchant. */
    @Serializable
    @SerialName("transaction")
    data class Transaction(val merchantContains: String = "") : Trigger

    /** iOS "Sound Recognition" → Android Sound Notifications alerts matching the chosen sound words. */
    @Serializable
    @SerialName("sound")
    data class SoundRecognition(val sounds: List<String> = emptyList()) : Trigger

    /** iOS "App": chosen app(s) Is Opened (opened or switched to) and/or Is Closed (closed or switched away from). */
    @Serializable
    @SerialName("app")
    data class App(
        val apps: List<AppRef> = emptyList(),
        val opened: Boolean = true,
        val closed: Boolean = false,
    ) : Trigger

    /**
     * iOS "Apple Watch Workout" → Galaxy Watch (Wear OS): a workout Starts / Ends. [types] are Health Services
     * exercise type names (e.g. "RUNNING"); empty = any workout. Reported by the companion watch app.
     */
    @Serializable
    @SerialName("workout")
    data class Workout(val types: List<String> = emptyList(), val starts: Boolean = true) : Trigger

    /** iOS "NFC": a tag scanned and named in the editor, identified by its hardware ID. */
    @Serializable
    @SerialName("nfc")
    data class Nfc(val tagId: String, val name: String) : Trigger

}

@Serializable
data class AppRef(val pkg: String, val name: String)

/** The workout types offered in the editor (Health Services ExerciseType names → English label). */
val workoutTypes = listOf(
    "RUNNING", "WALKING", "BIKING", "HIKING", "SWIMMING_POOL", "SWIMMING_OPEN_WATER", "STRENGTH_TRAINING",
    "YOGA", "ELLIPTICAL", "ROWING_MACHINE", "TREADMILL", "WORKOUT",
)

fun workoutName(type: String): String = when (type) {
    "RUNNING" -> "Running"
    "WALKING" -> "Walking"
    "BIKING" -> "Cycling"
    "HIKING" -> "Hiking"
    "SWIMMING_POOL" -> "Pool swim"
    "SWIMMING_OPEN_WATER" -> "Open-water swim"
    "STRENGTH_TRAINING" -> "Strength training"
    "YOGA" -> "Yoga"
    "ELLIPTICAL" -> "Elliptical"
    "ROWING_MACHINE" -> "Rowing machine"
    "TREADMILL" -> "Treadmill"
    "WORKOUT" -> "Other workout"
    else -> type.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
}

@Serializable
enum class AlarmEvent { GOES_OFF, SNOOZED, STOPPED }

/** iOS also has "Wind Down Begins"; Android exposes no wind-down signal, so it is not offered. */
@Serializable
enum class SleepPhase { BEDTIME, WAKING_UP }

object Apps {
    const val GMAIL = "com.google.android.gm"
    const val OUTLOOK = "com.microsoft.office.outlook"
    const val SAMSUNG_EMAIL = "com.samsung.android.email.provider"
    const val MESSAGES = "com.google.android.apps.messaging"
    const val SAMSUNG_MESSAGES = "com.samsung.android.messaging"
    const val WHATSAPP = "com.whatsapp"
    const val TELEGRAM = "org.telegram.messenger"
    const val CLOCK = "com.google.android.deskclock"
    const val SAMSUNG_CLOCK = "com.sec.android.app.clockpackage"
    const val WELLBEING = "com.google.android.apps.wellbeing"
    const val SAMSUNG_WELLBEING = "com.samsung.android.forest"
    const val WALLET = "com.google.android.apps.walletnfcrel"
    const val SOUND_NOTIFICATIONS = "com.google.audio.hearing.visualization.accessibility.scribe"

    /** Clock apps whose alarm notifications the Alarm trigger reads (Google Clock + Samsung Clock). */
    val clockApps = setOf(CLOCK, SAMSUNG_CLOCK)

    /** Digital Wellbeing / bedtime providers the Sleep trigger reads (Google + Samsung). */
    val wellbeingApps = setOf(WELLBEING, SAMSUNG_WELLBEING)

    val names = mapOf(
        GMAIL to "Gmail", OUTLOOK to "Outlook", SAMSUNG_EMAIL to "Samsung Email",
        MESSAGES to "Messages", SAMSUNG_MESSAGES to "Samsung Messages",
        WHATSAPP to "WhatsApp", TELEGRAM to "Telegram",
    )
}

@Serializable
enum class LevelCondition { EQUALS, RISES_ABOVE, FALLS_BELOW }

@Serializable
data class BtDevice(val address: String, val name: String)

@Serializable
enum class SolarEvent { SUNRISE, SUNSET }

/** A named circle on the map. [radius] in metres (Android geofences work best from about 100 m). */
@Serializable
data class Place(val name: String, val lat: Double, val lng: Double, val radius: Int = 150)

/** A daily window in minutes since midnight; may wrap past midnight (e.g. 22:00–06:00). */
@Serializable
data class TimeWindow(val fromMinute: Int, val toMinute: Int) {
    fun contains(minuteOfDay: Int): Boolean =
        if (fromMinute <= toMinute) minuteOfDay in fromMinute..toMinute
        else minuteOfDay >= fromMinute || minuteOfDay <= toMinute

    fun label() = "%02d:%02d–%02d:%02d".format(fromMinute / 60, fromMinute % 60, toMinute / 60, toMinute % 60)
}

/** iOS sunrise/sunset offsets: at the event, 15/30/45 minutes or 1 hour before or after. */
val solarOffsets = listOf(-60, -45, -30, -15, 0, 15, 30, 45, 60)

fun offsetLabel(minutes: Int): String {
    val n = kotlin.math.abs(minutes)
    return when {
        minutes == 0 -> t("At")
        minutes % 60 == 0 -> t(if (minutes < 0) "%d h before" else "%d h after", n / 60)
        else -> t(if (minutes < 0) "%d min before" else "%d min after", n)
    }
}

/** What an automation does. */
@Serializable
sealed interface Action {
    @Serializable
    @SerialName("show_notification")
    data class ShowNotification(val title: String, val body: String = "") : Action

    @Serializable
    @SerialName("open_app")
    data class OpenApp(val app: AppRef) : Action

    @Serializable
    @SerialName("open_url")
    data class OpenUrl(val url: String) : Action

    /** iOS "Get Contents of URL". [headers] as "Name: value" lines. */
    @Serializable
    @SerialName("webhook")
    data class Webhook(
        val url: String,
        val method: String = "POST",
        val headers: String = "",
        val body: String = "",
    ) : Action

    /** Call a Home Assistant service, e.g. "light.turn_on" on "light.living_room"; URL and token from Connections. */
    @Serializable
    @SerialName("home_assistant")
    data class HomeAssistant(val service: String, val entityId: String = "", val extraJson: String = "") : Action

    @Serializable
    @SerialName("set_volume")
    data class SetVolume(val stream: VolumeStream = VolumeStream.MEDIA, val percent: Int = 50) : Action

    @Serializable
    @SerialName("set_ringer")
    data class SetRinger(val mode: RingerMode = RingerMode.VIBRATE) : Action

    /** iOS "Set Focus" → Do Not Disturb on/off. */
    @Serializable
    @SerialName("set_dnd")
    data class SetDnd(val on: Boolean = true) : Action

    @Serializable
    @SerialName("flashlight")
    data class Flashlight(val mode: Toggle = Toggle.TOGGLE) : Action

    @Serializable
    @SerialName("brightness")
    data class Brightness(val percent: Int = 50) : Action

    @Serializable
    @SerialName("speak")
    data class Speak(val text: String) : Action

    @Serializable
    @SerialName("clipboard")
    data class CopyToClipboard(val text: String) : Action

    @Serializable
    @SerialName("wait")
    data class Wait(val seconds: Int = 5) : Action

    @Serializable
    @SerialName("media")
    data class Media(val command: MediaCommand = MediaCommand.PLAY_PAUSE) : Action

    @Serializable
    @SerialName("vibrate")
    data class Vibrate(val millis: Int = 500) : Action

    /** iOS "Run Shortcut": run another automation's actions. */
    @Serializable
    @SerialName("run_automation")
    data class RunAutomation(val automationId: Long, val name: String) : Action

    /** Needs Shizuku: switch Wi-Fi, Bluetooth, mobile data or Airplane Mode. */
    @Serializable
    @SerialName("set_radio")
    data class SetRadio(val radio: Radio, val mode: Toggle = Toggle.ON) : Action

    /** Needs Shizuku: force-stop an app. */
    @Serializable
    @SerialName("force_stop")
    data class ForceStop(val app: AppRef) : Action

    /**
     * Send the phone's current location (a Maps link) to a contact over [via].
     * SMS sends silently and automatically. For WhatsApp/Telegram, [mode] is PREFILLED (opens the chat with the
     * link ready, you tap send) or AUTO (the accessibility service taps send for you — fragile; see the editor warning).
     */
    @Serializable
    @SerialName("send_location")
    data class SendLocation(
        val via: SendVia = SendVia.SMS,
        val mode: SendMode = SendMode.PREFILLED,
        val to: String = "",
        val toName: String = "",
        val note: String = "",
    ) : Action

    /** Capture a still from the chosen cameras in the background and save it to the gallery. */
    @Serializable
    @SerialName("capture_photo")
    data class CapturePhoto(val front: Boolean = true, val back: Boolean = true) : Action

    /** Record [seconds] of audio and save it on the phone. */
    @Serializable
    @SerialName("record_audio")
    data class RecordAudio(val seconds: Int = 10) : Action

    /**
     * Send a text message. [to] is a phone number, or blank to reply to whoever sent the triggering SMS.
     * With [includeLocation] a Google Maps link to the current location is appended.
     */
    @Serializable
    @SerialName("send_sms")
    data class SendSms(val to: String = "", val body: String = "", val includeLocation: Boolean = true) : Action

    /** iOS "Lock Screen": lock the phone now (uses the accessibility service). */
    @Serializable
    @SerialName("lock_screen")
    data object LockScreen : Action

    /** iOS "Take Screenshot" (uses the accessibility service). */
    @Serializable
    @SerialName("take_screenshot")
    data object TakeScreenshot : Action

    /** iOS "Create Alarm": add a Clock alarm at [hour]:[minute]. */
    @Serializable
    @SerialName("set_alarm")
    data class SetAlarm(val hour: Int = 7, val minute: Int = 0, val label: String = "") : Action

    /** iOS "Start Timer". */
    @Serializable
    @SerialName("start_timer")
    data class StartTimer(val seconds: Int = 300, val label: String = "") : Action

    /** iOS "If": run [then] when [condition] holds, otherwise [otherwise]. */
    @Serializable
    @SerialName("if")
    data class If(
        val condition: Condition = Condition.Charging(),
        val then: List<Action> = emptyList(),
        val otherwise: List<Action> = emptyList(),
    ) : Action

    /**
     * iOS "Choose from Menu": a notification with up to 3 buttons (Android's limit); the tapped option's actions run,
     * then the rest of the automation. Only allowed at the top level of an automation.
     */
    @Serializable
    @SerialName("menu")
    data class ChooseFromMenu(val prompt: String = "", val options: List<MenuOption> = emptyList()) : Action
}

@Serializable
data class MenuOption(val label: String, val actions: List<Action> = emptyList())

/** How the phone reaches the internet. */
@Serializable
enum class Internet { WIFI, MOBILE, NONE }

/** What an If action can check. */
@Serializable
sealed interface Condition {
    @Serializable @SerialName("battery_above") data class BatteryAbove(val percent: Int = 50) : Condition
    @Serializable @SerialName("battery_below") data class BatteryBelow(val percent: Int = 20) : Condition
    @Serializable @SerialName("charging") data class Charging(val yes: Boolean = true) : Condition
    @Serializable @SerialName("wifi_is") data class WifiIs(val ssid: String = "") : Condition
    @Serializable @SerialName("time_between") data class TimeBetween(val window: TimeWindow = TimeWindow(8 * 60, 20 * 60)) : Condition
    @Serializable @SerialName("dnd_on") data class DndOn(val yes: Boolean = true) : Condition
    @Serializable @SerialName("weekday") data class Weekday(val days: Set<Int> = setOf(7, 1, 2, 3, 4)) : Condition

    // Power & device
    @Serializable @SerialName("battery_saver") data class BatterySaver(val on: Boolean = true) : Condition
    @Serializable @SerialName("airplane") data class Airplane(val on: Boolean = true) : Condition
    @Serializable @SerialName("dark_mode") data class DarkMode(val on: Boolean = true) : Condition

    // Connections
    @Serializable @SerialName("internet") data class InternetVia(val via: Internet = Internet.WIFI) : Condition
    /** [device] null means any device. Sees audio devices and anything connected over Bluetooth LE. */
    @Serializable @SerialName("bluetooth_connected") data class BluetoothConnected(val device: BtDevice? = null, val connected: Boolean = true) : Condition
    @Serializable @SerialName("headphones") data class Headphones(val connected: Boolean = true) : Condition

    // Sound & calls
    @Serializable @SerialName("ringer") data class Ringer(val mode: RingerMode = RingerMode.NORMAL) : Condition
    @Serializable @SerialName("volume") data class Volume(val above: Boolean = true, val percent: Int = 50) : Condition
    @Serializable @SerialName("music_playing") data class MusicPlaying(val yes: Boolean = true) : Condition
    @Serializable @SerialName("on_call") data class OnCall(val yes: Boolean = true) : Condition

    // Place & screen
    @Serializable @SerialName("at_place") data class AtPlace(val place: Place? = null, val there: Boolean = true) : Condition
    @Serializable @SerialName("locked") data class Locked(val yes: Boolean = true) : Condition
    /** Known only while the App-trigger accessibility service runs. */
    @Serializable @SerialName("app_in_front") data class AppInFront(val app: AppRef = AppRef("", ""), val yes: Boolean = true) : Condition

    // Chance
    @Serializable @SerialName("chance") data class Chance(val percent: Int = 50) : Condition
}

fun Condition.label(): String = when (this) {
    is Condition.BatteryAbove -> t("battery is above %d%%", percent)
    is Condition.BatteryBelow -> t("battery is below %d%%", percent)
    is Condition.Charging -> t(if (yes) "phone is charging" else "phone isn't charging")
    is Condition.WifiIs -> t("connected to Wi-Fi “%s”", ssid)
    is Condition.TimeBetween -> t("time is %s", window.label())
    is Condition.DndOn -> t(if (yes) "Do Not Disturb is on" else "Do Not Disturb is off")
    is Condition.Weekday -> t("today is %s", daysLabel(days))
    is Condition.BatterySaver -> t(if (on) "battery saver is on" else "battery saver is off")
    is Condition.Airplane -> t(if (on) "airplane mode is on" else "airplane mode is off")
    is Condition.DarkMode -> t(if (on) "dark mode is on" else "dark mode is off")
    is Condition.InternetVia -> t(when (via) { Internet.WIFI -> "internet is over Wi-Fi"; Internet.MOBILE -> "internet is over mobile data"; Internet.NONE -> "there's no internet" })
    is Condition.BluetoothConnected -> when (device) {
        null -> t(if (connected) "a Bluetooth device is connected" else "no Bluetooth device is connected")
        else -> t(if (connected) "%s is connected over Bluetooth" else "%s isn't connected over Bluetooth", device.name)
    }
    is Condition.Headphones -> t(if (connected) "headphones are connected" else "headphones aren't connected")
    is Condition.Ringer -> t(when (mode) { RingerMode.NORMAL -> "ringer is normal"; RingerMode.VIBRATE -> "ringer is on vibrate"; RingerMode.SILENT -> "ringer is silent" })
    is Condition.Volume -> t(if (above) "volume is above %d%%" else "volume is below %d%%", percent)
    is Condition.MusicPlaying -> t(if (yes) "music is playing" else "music isn't playing")
    is Condition.OnCall -> t(if (yes) "I'm on a call" else "I'm not on a call")
    is Condition.AtPlace -> t(if (there) "I'm at %s" else "I'm not at %s", place?.name ?: t("a place"))
    is Condition.Locked -> t(if (yes) "the phone is locked" else "the phone is unlocked")
    is Condition.AppInFront -> t(if (yes) "%s is in front" else "%s isn't in front", app.name)
    is Condition.Chance -> t("%d%% of the time", percent)
}

/** Every action in [actions], including those nested inside If and Choose from Menu. */
fun flatten(actions: List<Action>): List<Action> = actions.flatMap { a ->
    listOf(a) + when (a) {
        is Action.If -> flatten(a.then) + flatten(a.otherwise)
        is Action.ChooseFromMenu -> a.options.flatMap { flatten(it.actions) }
        else -> emptyList()
    }
}

@Serializable
enum class VolumeStream { MEDIA, RING, NOTIFICATION, ALARM }

@Serializable
enum class RingerMode { NORMAL, VIBRATE, SILENT }

@Serializable
enum class Toggle { ON, OFF, TOGGLE }

@Serializable
enum class MediaCommand { PLAY_PAUSE, PLAY, PAUSE, NEXT, PREVIOUS }

@Serializable
enum class Radio { WIFI, BLUETOOTH, MOBILE_DATA, AIRPLANE }

@Serializable
enum class RunMode { AFTER_CONFIRMATION, IMMEDIATELY }

/** Where "Send Location" sends. SMS is silent/automatic; WhatsApp and Telegram open the app. */
@Serializable
enum class SendVia { SMS, WHATSAPP, TELEGRAM }

/** For WhatsApp/Telegram: open with the message ready (you tap send) or let the accessibility service tap send. */
@Serializable
enum class SendMode { PREFILLED, AUTO }

@Serializable
data class Automation(
    val id: Long = 0,
    val name: String = "",
    val enabled: Boolean = true,
    val trigger: Trigger,
    val actions: List<Action> = emptyList(),
    val runMode: RunMode = RunMode.AFTER_CONFIRMATION,
    /** iOS "Notify When Run"; only used with [RunMode.IMMEDIATELY]. */
    val notifyWhenRun: Boolean = true,
) {
    val title: String get() = name.ifBlank { trigger.label() }
}

object AutomationJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        prettyPrint = false
    }
    private val listSerializer = ListSerializer(Automation.serializer())

    fun encode(a: Automation): String = json.encodeToString(Automation.serializer(), a)
    fun decode(s: String): Automation = json.decodeFromString(Automation.serializer(), s)
    fun encodeAll(list: List<Automation>): String = json.encodeToString(listSerializer, list)
    fun decodeAll(s: String): List<Automation> = json.decodeFromString(listSerializer, s)
}

private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun daysLabel(days: Set<Int>): String = when {
    days.isEmpty() || days.size == 7 -> t("daily")
    days == setOf(1, 2, 3, 4, 5) -> t("weekdays")
    days == setOf(6, 7) -> t("weekends")
    // Israeli work week, Sunday to Thursday.
    days == setOf(7, 1, 2, 3, 4) -> t("Sun–Thu")
    else -> weekOrder().filter { it in days }.joinToString(", ") { t(dayNames[it - 1]) }
}

/** iOS-style summary, e.g. "At 07:30, daily" or "When charger is connected". Whole sentences, so Hebrew reads naturally. */
fun Trigger.label(): String = when (this) {
    is Trigger.TimeOfDay -> when (solar) {
        null -> t("At %02d:%02d, %s", hour, minute, daysLabel(days))
        else -> {
            val sunrise = solar == SolarEvent.SUNRISE
            val at = if (offsetMinutes == 0) t(if (sunrise) "At sunrise" else "At sunset")
                else t(if (sunrise) "%s sunrise" else "%s sunset", offsetLabel(offsetMinutes))
            "$at, ${daysLabel(days)}"
        }
    }
    is Trigger.Arrive -> t("When I arrive at %s", place.name) + (window?.let { ", ${it.label()}" } ?: "")
    is Trigger.Leave -> t("When I leave %s", place.name) + (window?.let { ", ${it.label()}" } ?: "")
    is Trigger.BeforeCommute -> if (minutesBefore == 0) {
        t("At my %02d:%02d commute to %s", departHour, departMinute, destination.name)
    } else {
        t("%d min before my %02d:%02d commute to %s", minutesBefore, departHour, departMinute, destination.name)
    }
    is Trigger.Charger -> t(if (connected) "When charger is connected" else "When charger is disconnected")
    is Trigger.BatteryLevel -> when (condition) {
        LevelCondition.EQUALS -> t("When battery equals %d%%", percent)
        LevelCondition.RISES_ABOVE -> t("When battery rises above %d%%", percent)
        LevelCondition.FALLS_BELOW -> t("When battery falls below %d%%", percent)
    }
    is Trigger.LowPowerMode -> t(if (on) "When Battery Saver is turned on" else "When Battery Saver is turned off")
    is Trigger.AirplaneMode -> t(if (on) "When Airplane Mode is turned on" else "When Airplane Mode is turned off")
    is Trigger.Bluetooth -> {
        val who = if (devices.isEmpty()) t("any Bluetooth device") else devices.joinToString { it.name }
        t(if (connected) "When %s connects" else "When %s disconnects", who)
    }
    is Trigger.WiFi -> {
        val which = if (networks.isEmpty()) t("any Wi-Fi") else networks.joinToString()
        t(if (joins) "When phone joins %s" else "When phone leaves %s", which)
    }
    is Trigger.AndroidAuto -> t(if (connects) "When Android Auto connects" else "When Android Auto disconnects")
    is Trigger.Email -> t("When I get an email") + fromLabel(senders) + containsLabel(" subject has “%s”", subjectContains)
    is Trigger.Message -> t("When I get a message") + fromLabel(senders) + containsLabel(" containing “%s”", contains)
    is Trigger.Alarm -> when (event) {
        AlarmEvent.GOES_OFF -> t("When an alarm goes off")
        AlarmEvent.SNOOZED -> t("When an alarm is snoozed")
        AlarmEvent.STOPPED -> t("When an alarm is stopped")
    }
    is Trigger.Sleep -> t(if (phase == SleepPhase.BEDTIME) "When Bedtime begins" else "When waking up") + ", ${hours.label()}"
    is Trigger.Focus -> t(if (on) "When Do Not Disturb turns on" else "When Do Not Disturb turns off")
    is Trigger.Transaction -> t("When I pay with Wallet") + containsLabel(" at “%s”", merchantContains)
    is Trigger.SoundRecognition -> t("When a sound is recognised") + if (sounds.isEmpty()) "" else ": " + sounds.joinToString()
    is Trigger.App -> {
        val which = apps.joinToString { it.name }.ifEmpty { t("an app") }
        t(
            when {
                opened && closed -> "When %s is opened or closed"
                closed -> "When %s is closed"
                else -> "When %s is opened"
            },
            which,
        )
    }
    is Trigger.Nfc -> t("When I tap %s", name)
    is Trigger.Workout -> {
        val which = if (types.isEmpty()) t("a workout") else types.joinToString { t(workoutName(it)) }
        t(if (starts) "When %s starts on the watch" else "When %s ends on the watch", which)
    }
}

private fun fromLabel(senders: List<String>) = if (senders.isEmpty()) "" else t(" from %s", senders.joinToString())
private fun containsLabel(template: String, text: String) = if (text.isBlank()) "" else t(template, text)

fun Action.label(): String = when (this) {
    is Action.ShowNotification -> t("Show notification “%s”", title)
    is Action.OpenApp -> t("Open %s", app.name)
    is Action.OpenUrl -> t("Open %s", url)
    is Action.Webhook -> "$method ${url.substringAfter("://").take(40)}"
    is Action.HomeAssistant -> t("Home Assistant: %s", service) + if (entityId.isBlank()) "" else t(" on %s", entityId)
    is Action.SetVolume -> t("Set %s volume to %d%%", t(stream.name.lowercase()), percent)
    is Action.SetRinger -> t("Set ringer to %s", t(mode.name.lowercase()))
    is Action.SetDnd -> t(if (on) "Turn Do Not Disturb on" else "Turn Do Not Disturb off")
    is Action.Flashlight -> t("Flashlight %s", t(mode.name.lowercase()))
    is Action.Brightness -> t("Set brightness to %d%%", percent)
    is Action.Speak -> t("Speak “%s”", text.take(30))
    is Action.CopyToClipboard -> t("Copy “%s”", text.take(30))
    is Action.Wait -> t("Wait %d s", seconds)
    is Action.Media -> t("Media: %s", t(command.name.lowercase().replace('_', '/')))
    is Action.Vibrate -> t("Vibrate")
    is Action.RunAutomation -> t("Run “%s”", name)
    is Action.SetRadio -> radio.label + " " + t(mode.name.lowercase())
    is Action.ForceStop -> t("Force-stop %s", app.name)
    is Action.If -> t("If %s", condition.label())
    is Action.ChooseFromMenu -> t("Choose: %s", options.joinToString(" / ") { it.label })
    is Action.SendLocation -> t("Send location")
    is Action.CapturePhoto -> t(
        when {
            front && back -> "Capture front & back photo"
            back -> "Capture back photo"
            else -> "Capture front photo"
        },
    )
    is Action.RecordAudio -> t("Record %d s audio", seconds)
    is Action.SendSms -> if (to.isBlank()) t("Text back the sender") else t("Text %s", to)
    is Action.LockScreen -> t("Lock the screen")
    is Action.TakeScreenshot -> t("Take a screenshot")
    is Action.SetAlarm -> t("Set alarm for %02d:%02d", hour, minute)
    is Action.StartTimer -> t("Start a %d s timer", seconds)
}

val Radio.label: String get() = t(
    when (this) {
        Radio.WIFI -> "Wi-Fi"
        Radio.BLUETOOTH -> "Bluetooth"
        Radio.MOBILE_DATA -> "Mobile data"
        Radio.AIRPLANE -> "Airplane Mode"
    },
)
