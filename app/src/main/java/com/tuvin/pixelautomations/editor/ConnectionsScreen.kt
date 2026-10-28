package com.tuvin.pixelautomations.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.actions.ShizukuShell
import com.tuvin.pixelautomations.setup.Connections
import com.tuvin.pixelautomations.setup.t

/** Addresses and secrets for service actions. Saved on Done, privately on this phone only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Connections(ctx) }
    var haUrl by remember { mutableStateOf(store.homeAssistantUrl) }
    var haToken by remember { mutableStateOf(store.homeAssistantToken) }

    fun save() {
        store.homeAssistantUrl = haUrl
        store.homeAssistantToken = haToken
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(t("Connections")) },
            navigationIcon = { TextButton(onClick = onBack) { Text(t("Cancel")) } },
            actions = { TextButton(onClick = { save(); onBack() }) { Text(t("Done")) } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                t("Stored only on this phone, never in exports."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionCard(t("Home Assistant"), Icons.Default.Home, Color(0xFF0891B2)) {
                OutlinedTextField(
                    haUrl, { haUrl = it }, label = { Text(t("Address, e.g. http://homeassistant.local:8123")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    haToken, { haToken = it }, label = { Text(t("Long-lived access token")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                )
            }
            SectionCard(t("Shizuku"), Icons.Default.Terminal, Color(0xFFEA580C)) {
                val (status, dot) = when {
                    !ShizukuShell.available() -> t("Not running. Open Shizuku and start it (again after each reboot).") to Color(0xFFDC2626)
                    !ShizukuShell.permitted() -> t("Running, but Automations isn't allowed yet.") to Color(0xFFD97706)
                    else -> t("Running and allowed.") to Color(0xFF16A34A)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
                    Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                }
                if (ShizukuShell.available() && !ShizukuShell.permitted()) {
                    OutlinedButton(onClick = { ShizukuShell.requestPermission() }) { Text(t("Allow")) }
                }
            }
        }
    }
}

/** A Connections card: a colour tile + title header, then its fields. Matches the editor's section style. */
@Composable
private fun SectionCard(title: String, icon: ImageVector, color: Color, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(color),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                Text(
                    title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            content()
        }
    }
}
