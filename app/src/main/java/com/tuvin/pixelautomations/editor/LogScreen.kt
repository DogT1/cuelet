package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.model.RunLogRow
import com.tuvin.pixelautomations.setup.HebrewStrings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val fmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm:ss")

// A result is stored in whatever language was active when it ran, so match the template's leading text in BOTH
// languages (English key + Hebrew value), not just the current one.
private fun prefixesOf(key: String): List<String> = listOfNotNull(
    key.substringBefore("%s").trim().takeIf { it.isNotEmpty() },
    HebrewStrings.map[key]?.substringBefore("%s")?.trim()?.takeIf { it.isNotEmpty() },
)
private val failedPrefixes = prefixesOf("Failed: %s")
private val waitingPrefixes = prefixesOf("Waiting for a choice in “%s”")

private fun statusFor(result: String): Pair<ImageVector, Color> = when {
    waitingPrefixes.any { result.startsWith(it) } -> Icons.Default.HourglassEmpty to Color(0xFFD97706)
    failedPrefixes.any { result.startsWith(it) } -> Icons.Default.ErrorOutline to Color(0xFFDC2626)
    else -> Icons.Default.CheckCircle to Color(0xFF16A34A)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(rows: List<RunLogRow>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("Run Log")) },
                navigationIcon = { TextButton(onClick = onBack) { Text(t("Back")) } },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyLog(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows, key = { it.id }) { r ->
                val (icon, color) = statusFor(r.result)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(color),
                            contentAlignment = Alignment.Center,
                        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(r.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${fmt.format(Instant.ofEpochMilli(r.at).atZone(ZoneId.systemDefault()))} · ${r.trigger}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(r.result, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLog(modifier: Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp)) }
        Text(
            t("Nothing has run yet."), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            t("When an automation runs, you'll see it here."), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
        )
    }
}
