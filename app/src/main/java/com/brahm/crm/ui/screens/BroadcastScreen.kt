package com.brahm.crm.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.brahm.crm.data.Lead
import com.brahm.crm.data.Stage
import com.brahm.crm.service.BroadcastAlarmReceiver
import com.brahm.crm.service.BulkSender
import com.brahm.crm.util.Share
import java.util.Calendar

@Composable
fun BroadcastScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val leads by vm.leads.collectAsState()

    var channel by remember { mutableStateOf("sms") }           // sms | wa | wab
    var stageFilter by remember { mutableStateOf<Stage?>(null) }
    var message by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<Long, Boolean>() }

    val smsPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Only leads with a phone number can be messaged.
    val candidates = remember(leads, stageFilter) {
        leads.filter { it.phone.isNotBlank() }
            .filter { stageFilter == null || Stage.from(it.stage) == stageFilter }
    }
    val chosen = candidates.filter { selected[it.id] == true }

    fun phones() = chosen.map { it.phone }.filter { it.isNotBlank() }

    fun sendNow() {
        if (message.isBlank() || chosen.isEmpty()) {
            Toast.makeText(context, "Pick contacts and type a message", Toast.LENGTH_SHORT).show()
            return
        }
        if (channel == "sms") {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                smsPerm.launch(Manifest.permission.SEND_SMS); return
            }
            val res = BulkSender.sendSms(context, phones(), message, vm.smsSimSubId)
            Toast.makeText(context, "SMS: ${res.sent} sent, ${res.failed} failed", Toast.LENGTH_LONG).show()
        } else {
            // WhatsApp assisted: open the first chat prefilled; user sends, then returns.
            val first = phones().firstOrNull()
            if (first != null) {
                Share.openWhatsApp(context, first, message)
                Toast.makeText(
                    context,
                    "Opening WhatsApp one by one — press Send, then come back for the next.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun schedule() {
        if (message.isBlank() || chosen.isEmpty()) {
            Toast.makeText(context, "Pick contacts and type a message", Toast.LENGTH_SHORT).show()
            return
        }
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, hh, mm ->
                val c = Calendar.getInstance(); c.set(y, m, d, hh, mm, 0)
                if (c.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(context, "Pick a future time", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                vm.saveScheduledBroadcast(channel, message, phones(), c.timeInMillis) { id ->
                    BroadcastAlarmReceiver.schedule(context, id, c.timeInMillis)
                }
                Toast.makeText(context, "Scheduled for ${"%02d/%02d %02d:%02d".format(d, m + 1, hh, mm)}", Toast.LENGTH_LONG).show()
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Broadcast", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Message your selected contacts — SMS auto-sends; WhatsApp opens each chat for you to press Send.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Text("Channel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(channel == "sms", { channel = "sms" }, { Text("SMS") })
            FilterChip(channel == "wa", { channel = "wa" }, { Text("WhatsApp") })
            FilterChip(channel == "wab", { channel = "wab" }, { Text("WA Business") })
        }

        Spacer(Modifier.height(8.dp))
        Text("Filter by board stage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(stageFilter == null, { stageFilter = null }, { Text("All") })
            Stage.entries.forEach { st ->
                FilterChip(stageFilter == st, { stageFilter = st }, { Text(st.label) })
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recipients (${chosen.size}/${candidates.size})", fontWeight = FontWeight.SemiBold)
            Text(
                "Select all",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    val allOn = candidates.all { selected[it.id] == true }
                    candidates.forEach { selected[it.id] = !allOn }
                }
            )
        }

        if (candidates.isEmpty()) {
            Text(
                "No leads with a phone number in this filter. Import contacts or add phone numbers to leads.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(candidates, key = { it.id }) { lead ->
                    RecipientRow(lead, selected[lead.id] == true) {
                        selected[lead.id] = !(selected[lead.id] ?: false)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sendNow() }, modifier = Modifier.weight(1f)) { Text("Send now") }
            OutlinedButton(onClick = { schedule() }, modifier = Modifier.weight(1f)) { Text("Schedule") }
        }
    }
}

@Composable
private fun RecipientRow(lead: Lead, checked: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(lead.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(lead.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
