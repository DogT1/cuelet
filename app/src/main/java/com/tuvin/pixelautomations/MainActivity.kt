package com.tuvin.pixelautomations

import com.tuvin.pixelautomations.setup.t

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.tuvin.pixelautomations.ui.ThemeSettings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tuvin.pixelautomations.editor.AppViewModel
import com.tuvin.pixelautomations.editor.ConnectionsScreen
import com.tuvin.pixelautomations.editor.SettingsScreen
import com.tuvin.pixelautomations.setup.I18n
import com.tuvin.pixelautomations.editor.EditScreen
import com.tuvin.pixelautomations.editor.ListScreen
import com.tuvin.pixelautomations.editor.LogScreen
import com.tuvin.pixelautomations.model.Automation

private sealed interface Screen {
    data object List : Screen
    data class Edit(val automation: Automation?) : Screen
    data object Log : Screen
    data object Connections : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var screen by mutableStateOf<Screen>(Screen.List)
    private var batteryRestricted by mutableStateOf(false)
    /** Bumped on every resume so permission-dependent UI re-checks after a trip to Settings. */
    private var resumeCount by mutableIntStateOf(0)
    private var connectionsOverEditor by mutableStateOf(false)

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    /** Export: the user picks where to save; passwords and tokens (Connections) are never included. */
    private val exportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val ok = runCatching {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) } }
            }
            toast(if (ok.isSuccess) t("Exported %d automations", vm.automations.value.size) else t("Export failed: %s", ok.exceptionOrNull()?.message))
        }
    }

    private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val result = runCatching {
                val json = withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                vm.importJson(json)
            }
            toast(result.fold({ t("Imported %d automations", it) }, { t("Import failed: not an automations file (%s)", it.message) }))
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            // Teal brand, in the theme mode chosen in Settings (System / Light / Dark / AMOLED).
            val colors = ThemeSettings.scheme(isSystemInDarkTheme())
            val automations by vm.automations.collectAsState()
            val log by vm.log.collectAsState()
            // Right-to-left in Hebrew, left-to-right in English, whatever the phone's own language is.
            val direction = if (I18n.hebrew) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
            MaterialTheme(colorScheme = colors) {
                BackHandler(enabled = screen != Screen.List) {
                    when {
                        connectionsOverEditor -> connectionsOverEditor = false
                        screen == Screen.Connections -> screen = Screen.Settings
                        else -> screen = Screen.List
                    }
                }
                when (val s = screen) {
                    Screen.List -> ListScreen(
                        automations = automations,
                        batteryRestricted = batteryRestricted,
                        onNew = { screen = Screen.Edit(null) },
                        onOpen = { screen = Screen.Edit(it) },
                        onToggle = { a, on -> vm.setEnabled(a, on) },
                        onLog = { screen = Screen.Log },
                        onFixBattery = ::askBatteryExemption,
                        refreshKey = resumeCount,
                        onSettings = { screen = Screen.Settings },
                        onExport = { exportFile.launch("automations.json") },
                        onImport = { importFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    )
                    // Connections opens on top of the editor so the draft isn't lost.
                    is Screen.Edit -> Box(Modifier.fillMaxSize()) {
                        EditScreen(
                            initial = s.automation,
                            onCancel = { screen = Screen.List },
                            onDone = { vm.save(it); screen = Screen.List },
                            onDelete = { vm.delete(it); screen = Screen.List },
                            others = automations,
                            onOpenConnections = { connectionsOverEditor = true },
                            resumeTick = resumeCount,
                            onRunNow = {
                                vm.runNow(it)
                                Toast.makeText(this@MainActivity, t("Running “%s”…", it.title), Toast.LENGTH_SHORT).show()
                            },
                        )
                        if (connectionsOverEditor) {
                            Surface(Modifier.fillMaxSize()) {
                                ConnectionsScreen(onBack = { connectionsOverEditor = false; resumeCount++ })
                            }
                        }
                    }
                    Screen.Log -> LogScreen(log, onBack = { screen = Screen.List })
                    Screen.Settings -> SettingsScreen(
                        onBack = { screen = Screen.List },
                        onConnections = { screen = Screen.Connections },
                        resumeTick = resumeCount,
                    )
                    Screen.Connections -> ConnectionsScreen(onBack = { screen = Screen.Settings })
                }
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        batteryRestricted = !getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        resumeCount++
        vm.refresh()
    }

    @SuppressLint("BatteryLife") // personal sideloaded app; the user asked for reliable background triggers
    private fun askBatteryExemption() {
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }
}
