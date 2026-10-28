package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tuvin.pixelautomations.engine.AppWatchService
import com.tuvin.pixelautomations.model.AppRef
import com.tuvin.pixelautomations.triggers.NfcTrigger

/** Searchable list of launchable apps with checkboxes, chosen apps on top. */
@Composable
fun AppPicker(selected: List<AppRef>, onChange: (List<AppRef>) -> Unit) {
    val ctx = LocalContext.current
    val all = remember {
        AppWatchService.launchablePackages(ctx)
            .filter { it != ctx.packageName }
            .map { AppRef(it, AppWatchService.appName(ctx, it)) }
            .sortedBy { it.name.lowercase() }
    }
    var query by remember { mutableStateOf("") }
    val chosen = selected.map { it.pkg }.toSet()
    val shown = (selected + all.filter { it.pkg !in chosen && (query.isBlank() || it.name.contains(query, ignoreCase = true)) })
        .take(selected.size + 40)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(query, { query = it }, label = { Text(t("Search apps")) }, modifier = Modifier.fillMaxWidth())
        shown.forEach { app ->
            val on = app.pkg in chosen
            Row(
                Modifier.fillMaxWidth().toggleable(value = on, role = Role.Checkbox, onValueChange = {
                    onChange(if (on) selected.filterNot { it.pkg == app.pkg } else selected + app)
                }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = on, onCheckedChange = null)
                Text(app.name)
            }
        }
    }
}

/**
 * iOS-style "Scan": hold the phone to a tag while this is showing. Reads the tag's ID; with [write] also writes
 * a record so Android always opens Automations for it (erasing what was on the tag).
 */
@Composable
fun NfcScanner(tagId: String?, write: Boolean, onWriteChange: (Boolean) -> Unit, onScanned: (String) -> Unit) {
    val ctx = LocalContext.current
    val adapter = remember { NfcAdapter.getDefaultAdapter(ctx) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            adapter == null -> Text(t("This phone has no NFC."), style = MaterialTheme.typography.bodySmall)
            !adapter.isEnabled -> Text(t("Turn on NFC in quick settings first."), style = MaterialTheme.typography.bodySmall)
        }
        Text(if (tagId == null) t("No tag scanned yet.") else t("Tag ID: %s", tagId), style = MaterialTheme.typography.bodyMedium)
        Row(
            Modifier.fillMaxWidth().toggleable(value = write, role = Role.Checkbox, onValueChange = onWriteChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = write, onCheckedChange = null)
            Text(t("Write tag (needed if the tag already holds a link; erases it)"))
        }
        OutlinedButton(enabled = adapter?.isEnabled == true, onClick = { scanning = !scanning; status = null }) {
            Text(t(if (scanning) "Cancel scan" else if (tagId == null) "Scan tag" else "Scan again"))
        }
        if (scanning) Text(t("Hold the top back of the phone against the tag…"), style = MaterialTheme.typography.bodySmall)
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }

    val activity = ctx.findActivity()
    DisposableEffect(scanning) {
        if (scanning && adapter != null && activity != null) {
            val main = Handler(Looper.getMainLooper())
            adapter.enableReaderMode(activity, { tag ->
                val id = NfcTrigger.idOf(tag.id)
                val result = if (write) runCatching { writeTag(tag, id, ctx.packageName) } else Result.success(Unit)
                main.post {
                    scanning = false
                    result.onSuccess {
                        onScanned(id)
                        status = t(if (write) "Scanned and written." else "Scanned.")
                    }.onFailure { status = t("Couldn't write the tag: %s. Try again or untick Write tag.", it.message) }
                }
            }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V, null) // keeps the system scan sound as feedback
        }
        onDispose { if (adapter != null && activity != null) adapter.disableReaderMode(activity) }
    }
}

private fun writeTag(tag: Tag, id: String, pkg: String) {
    val message = NdefMessage(
        arrayOf(NdefRecord.createUri(NfcTrigger.uriFor(id)), NdefRecord.createApplicationRecord(pkg)),
    )
    Ndef.get(tag)?.use { ndef ->
        ndef.connect()
        check(ndef.isWritable) { "the tag is read-only" }
        check(ndef.maxSize >= message.byteArrayLength) { "the tag is too small" }
        ndef.writeNdefMessage(message)
        return
    }
    NdefFormatable.get(tag)?.use { f ->
        f.connect()
        f.format(message)
        return
    }
    error("this tag type can't hold data")
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
