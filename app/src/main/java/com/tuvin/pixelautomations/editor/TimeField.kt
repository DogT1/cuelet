package com.tuvin.pixelautomations.editor

import com.tuvin.pixelautomations.setup.t

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

/** iOS-style time row: shows "07:30"; tapping opens a clock-dial picker. Edits [state] in place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, state: TimePickerState) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) {
            Text(t(label), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(12.dp))
        }
        OutlinedButton(onClick = { open = true }) {
            Text("%02d:%02d".format(state.hour, state.minute), style = MaterialTheme.typography.titleLarge)
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text(t("Done")) } },
            text = { TimePicker(state = state) },
        )
    }
}

/** Same, for a value stored as minutes since midnight; [onChange] gets the new value when the dialog closes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, minuteOfDay: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(t(label), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = { open = true }) {
            Text("%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60), style = MaterialTheme.typography.titleLarge)
        }
    }
    if (open) {
        val state = rememberTimePickerState(minuteOfDay / 60, minuteOfDay % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { onChange(state.hour * 60 + state.minute); open = false }) { Text(t("Done")) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(t("Cancel")) } },
            text = { TimePicker(state = state) },
        )
    }
}

private typealias Modifier = androidx.compose.ui.Modifier
