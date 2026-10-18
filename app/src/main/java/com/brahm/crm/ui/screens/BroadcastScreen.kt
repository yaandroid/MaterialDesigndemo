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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.service.BroadcastAlarmReceiver
import com.brahm.crm.util.SheetImport
import com.brahm.crm.util.SheetRow
import com.brahm.crm.util.Share
import java.util.Calendar

@Composable
fun BroadcastScreen(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("SMS") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("WhatsApp") })
        }
        if (tab == 0) BroadcastTab(vm, isSms = true)
        else BroadcastTab(vm, isSms = false)
    }
}

@Composable
private fun BroadcastTab(vm: AppViewModel, isSms: Boolean) {
    val context = LocalContext.current
    val leads by vm.leads.collectAsState()

    var stageFilter by remember { mutableStateOf<Stage?>(null) }
    var message by remember { mutableStateOf("") }
    var waTarget by remember { mutableStateOf("wa") }
    var delaySec by remember { mutableStateOf(vm.bulkDelaySeconds.toString()) }
    var progress by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<Long, Boolean>() }
    var sheet by remember { mutableStateOf<List<SheetRow>>(emptyList()) }

    val smsPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val csvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            sheet = runCatching { SheetImport.parseCsv(context, uri) }.getOrDefault(emptyList())
            Toast.makeText(context, "Loaded ${sheet.size} rows from sheet", Toast.LENGTH_LONG).show()
        }
    }

    val candidates = remember(leads, stageFilter) {
        leads.filter { it.phone.isNotBlank() }
            .filter { stageFilter == null || Stage.from(it.stage) == stageFilter }
    }
    val chosenLeads = candidates.filter { selected[it.id] == true }

    // Build (phone -> personalized message) pairs from sheet (if loaded) else chosen leads.
    fun buildPairs(): List<Pair<String, String>> {
        return if (sheet.isNotEmpty()) {
            sheet.map { it.phone to SheetImport.personalize(message, it) }
        } else {
            chosenLeads.map { lead ->
                val row = SheetRow(lead.phone, lead.name, mapOf("name" to lead.name, "phone" to lead.phone))
                lead.phone to SheetImport.personalize(message, row)
            }
        }
    }

    fun recipientCount() = if (sheet.isNotEmpty()) sheet.size else chosenLeads.size

    fun sendNow() {
        val pairs = buildPairs()
        if (message.isBlank() || pairs.isEmpty()) {
            Toast.makeText(context, "Pick recipients and type a message", Toast.LENGTH_SHORT).show()
            return
        }
        if (isSms) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) { smsPerm.launch(Manifest.permission.SEND_SMS); return }
            val delay = delaySec.toIntOrNull() ?: 0
            vm.setBulkDelay(delay)
            progress = "Sending…"
            vm.sendSmsSequential(
                pairs, delay,
                onProgress = { done, total -> progress = "Sending $done / $total" },
                onDone = { sent, failed -> progress = "Done: $sent sent, $failed failed" }
            )
        } else {
            // WhatsApp assisted: open the first chat prefilled; user sends, returns for next.
            val first = pairs.firstOrNull()
            if (first != null) {
                Share.openWhatsApp(context, first.first, first.second)
                Toast.makeText(
                    context,
                    "Opening WhatsApp one by one — press Send, then come back for the next.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun schedule() {
        val pairs = buildPairs()
        if (message.isBlank() || pairs.isEmpty()) {
            Toast.makeText(context, "Pick recipients and type a message", Toast.LENGTH_SHORT).show()
            return
        }
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, hh, mm ->
                val c = Calendar.getInstance(); c.set(y, m, d, hh, mm, 0)
                if (c.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(context, "Pick a future time", Toast.LENGTH_SHORT).show(); return@TimePickerDialog
                }
                // Scheduled broadcasts store a single message (per-row personalization
                // applies at send-now; scheduled uses the common text to all phones).
                val phones = pairs.map { it.first }
                val channel = if (isSms) "sms" else waTarget
                vm.saveScheduledBroadcast(channel, message, phones, c.timeInMillis) { id ->
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
        Text(
            if (isSms) "SMS to your contacts (auto-sends)"
            else "WhatsApp — opens each chat for you to press Send",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        if (!isSms) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(waTarget == "wa", { waTarget = "wa" }, { Text("WhatsApp") })
                FilterChip(waTarget == "wab", { waTarget = "wab" }, { Text("Business") })
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message — use {name}, {number}, {id} or any sheet column") },
            modifier = Modifier.fillMaxWidth().height(96.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { csvPicker.launch("*/*") }, modifier = Modifier.weight(1f)) {
                Text(if (sheet.isEmpty()) "Upload sheet (CSV)" else "Sheet: ${sheet.size} rows")
            }
            if (sheet.isNotEmpty()) {
                OutlinedButton(onClick = { sheet = emptyList() }) { Text("Clear") }
            }
        }
        if (sheet.isNotEmpty()) {
            Text(
                "Using uploaded sheet — {column} names from its header row are replaced per contact.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSms) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = delaySec,
                onValueChange = { delaySec = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Delay between SMS (seconds) — e.g. 6 to 10") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (sheet.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Filter by board stage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(stageFilter == null, { stageFilter = null }, { Text("All") })
                Stage.entries.forEach { st -> FilterChip(stageFilter == st, { stageFilter = st }, { Text(st.label) }) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recipients (${chosenLeads.size}/${candidates.size})", fontWeight = FontWeight.SemiBold)
                Text(
                    "Select all", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        val allOn = candidates.all { selected[it.id] == true }
                        candidates.forEach { selected[it.id] = !allOn }
                    }
                )
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(candidates, key = { it.id }) { lead ->
                    RecipientRow(lead, selected[lead.id] == true) {
                        selected[lead.id] = !(selected[lead.id] ?: false)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text("Recipients: ${sheet.size} (from sheet)", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
        }

        if (progress.isNotBlank()) {
            Text(progress, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sendNow() }, modifier = Modifier.weight(1f)) {
                Text("Send now (${recipientCount()})")
            }
            OutlinedButton(onClick = { schedule() }, modifier = Modifier.weight(1f)) { Text("Schedule") }
        }
    }
}

@Composable
private fun RecipientRow(lead: Lead, checked: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
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
