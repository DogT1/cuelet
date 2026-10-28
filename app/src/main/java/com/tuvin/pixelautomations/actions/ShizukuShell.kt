package com.tuvin.pixelautomations.actions

import android.content.pm.PackageManager
import com.tuvin.pixelautomations.model.Radio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/** Shell commands run with Shizuku's adb-level rights, for settings Android doesn't let normal apps change. */
object ShizukuShell {
    fun available(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun permitted(): Boolean = available() &&
        runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)

    fun requestPermission(requestCode: Int = 7) {
        if (available()) Shizuku.requestPermission(requestCode)
    }

    /** The shell command that switches [radio] on or off. */
    fun radioCommand(radio: Radio, on: Boolean): String = when (radio) {
        Radio.WIFI -> "cmd wifi set-wifi-enabled " + if (on) "enabled" else "disabled"
        Radio.BLUETOOTH -> "cmd bluetooth_manager " + if (on) "enable" else "disable"
        Radio.MOBILE_DATA -> "svc data " + if (on) "enable" else "disable"
        Radio.AIRPLANE -> "cmd connectivity airplane-mode " + if (on) "enable" else "disable"
    }

    fun forceStopCommand(pkg: String): String {
        require(Regex("[A-Za-z0-9_.]+").matches(pkg)) { "not a package name: $pkg" }
        return "am force-stop $pkg"
    }

    /** Runs [command] through Shizuku and returns its output; throws with a readable reason on failure. */
    suspend fun run(command: String): String = withContext(Dispatchers.IO) {
        check(available()) { "Shizuku isn't running. Open Shizuku and start it (needed again after each reboot)." }
        check(permitted()) { "Automations isn't allowed in Shizuku yet. Use Allow in the editor." }
        // Shizuku 13 made newProcess private; calling it by reflection is the common workaround for one-off
        // shell commands (the alternative, a bound UserService, is overkill for these).
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java,
        ).apply { isAccessible = true }
        val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        val out = process.inputStream.bufferedReader().readText() + process.errorStream.bufferedReader().readText()
        val code = process.waitFor()
        check(code == 0) { "“$command” failed ($code): ${out.trim().take(200)}" }
        out.trim()
    }
}
