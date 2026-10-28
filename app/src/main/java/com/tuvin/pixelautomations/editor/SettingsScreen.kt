package com.tuvin.pixelautomations.editor

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.BuildConfig
import com.tuvin.pixelautomations.engine.Notifications
import com.tuvin.pixelautomations.setup.AppLanguage
import com.tuvin.pixelautomations.setup.I18n
import com.tuvin.pixelautomations.setup.Permissions
import com.tuvin.pixelautomations.setup.t
import com.tuvin.pixelautomations.ui.ThemeMode
import com.tuvin.pixelautomations.ui.ThemeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onConnections: () -> Unit, resumeTick: Int = 0) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val requestRuntime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }

    // Open the right place to grant a permission: a runtime prompt, a specific Settings page, or our own Connections.
    fun fix(perm: String) {
        when {
            perm == Permissions.HOME_ASSISTANT || perm == Permissions.SHIZUKU -> onConnections()
            // Background location and the battery exemption can't be requested inline — send the user to app settings.
            perm == Permissions.BATTERY || perm == Permissions.BACKGROUND_LOCATION ->
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            Permissions.isSpecial(perm) -> Permissions.settingsIntent(ctx, perm)?.let { ctx.startActivity(it) }
            else -> requestRuntime.launch(arrayOf(perm))
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(t("Settings")) },
            navigationIcon = { TextButton(onClick = onBack) { Text(t("Back")) } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t("Language"), style = MaterialTheme.typography.titleMedium)
                    // Language names are shown in their own language, so they're findable whatever is selected.
                    listOf(
                        AppLanguage.SYSTEM to t("Phone language"),
                        AppLanguage.ENGLISH to "English",
                        AppLanguage.HEBREW to "עברית",
                        AppLanguage.RUSSIAN to "Русский",
                        AppLanguage.GERMAN to "Deutsch",
                        AppLanguage.FRENCH to "Français",
                        AppLanguage.SPANISH to "Español",
                        AppLanguage.ITALIAN to "Italiano",
                    ).forEach { (lang, name) ->
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = I18n.setting == lang, role = Role.RadioButton, onClick = {
                                I18n.set(ctx, lang)
                                // Notification channel names follow the language too.
                                Notifications.createChannels(ctx)
                            }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = I18n.setting == lang, onClick = null)
                            Text(name, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t("Theme"), style = MaterialTheme.typography.titleMedium)
                    listOf(
                        ThemeMode.SYSTEM to t("System"),
                        ThemeMode.LIGHT to t("Light"),
                        ThemeMode.DARK to t("Dark"),
                        ThemeMode.AMOLED to t("AMOLED (black)"),
                    ).forEach { (m, name) ->
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = ThemeSettings.mode == m, role = Role.RadioButton,
                                onClick = { ThemeSettings.set(ctx, m) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = ThemeSettings.mode == m, onClick = null)
                            Text(name, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth().clickable(onClick = onConnections)) {
                Column(Modifier.padding(16.dp)) {
                    Text(t("Connections"), style = MaterialTheme.typography.titleMedium)
                    Text(t("Home Assistant and Shizuku"), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Recomputed when returning from a Settings page (resumeTick) or after a runtime grant (refresh).
            val statuses = remember(resumeTick, refresh) { Permissions.all().associateWith { Permissions.granted(ctx, it) } }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t("Permissions"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        t("Grant what the features you use need. Tap an item to fix it."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Permissions.all().forEach { perm ->
                        val ok = statuses[perm] == true
                        Row(
                            Modifier.fillMaxWidth().clickable { fix(perm) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (ok) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                                tint = if (ok) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(Permissions.describe(perm), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }

            Text(t("Version %s", BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
        }
    }
}
