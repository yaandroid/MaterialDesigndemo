package com.brahm.crm.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brahm.crm.data.PersonSetting
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonSettingsSheet(
    current: PersonSetting,
    onSave: (PersonSetting) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var autoReply by remember { mutableStateOf(current.autoReplyEnabled) }
    var aiAuto by remember { mutableStateOf(current.aiAutoEnabled) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Chat settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                current.person,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            ToggleRow(
                "Notification auto-reply",
                "Offer a one-tap reply whenever this person messages",
                autoReply
            ) { autoReply = it }

            ToggleRow(
                "AI auto-draft",
                "Pre-draft an AI reply for this person's messages",
                aiAuto
            ) { aiAuto = it }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    onSave(current.copy(autoReplyEnabled = autoReply, aiAutoEnabled = aiAuto))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(0.dp))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.padding(start = 4.dp))
    }
}

/**
 * Lets the user type a reply, pick a date + time, choose WA / WA Business,
 * and confirm. Uses the platform date & time pickers.
 */
@Composable
fun ScheduleReplyFlow(
    context: Context,
    defaultText: String,
    onDismiss: () -> Unit,
    onConfirm: (text: String, whenMs: Long, target: String) -> Unit
) {
    var text by remember { mutableStateOf(defaultText) }
    var target by remember { mutableStateOf("wa") }
    var whenMs by remember { mutableStateOf(0L) }
    var whenLabel by remember { mutableStateOf("Not set") }

    fun pickDateTime() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                TimePickerDialog(
                    context,
                    { _, hh, mm ->
                        val c = Calendar.getInstance()
                        c.set(y, m, d, hh, mm, 0)
                        whenMs = c.timeInMillis
                        whenLabel = "%02d/%02d/%d  %02d:%02d".format(d, m + 1, y, hh, mm)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    false
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule a reply") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Send via", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(target == "wa", { target = "wa" }, { Text("WhatsApp") })
                    FilterChip(target == "wab", { target = "wab" }, { Text("Business") })
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { pickDateTime() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Pick date & time: $whenLabel")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "At that time you'll get a one-tap notification to open WhatsApp with this message ready — you press Send.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() && whenMs > System.currentTimeMillis(),
                onClick = { onConfirm(text.trim(), whenMs, target) }
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
