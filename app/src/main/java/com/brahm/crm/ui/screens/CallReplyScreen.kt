package com.brahm.crm.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.util.SimUtil

@Composable
fun CallReplyScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    var exclude by remember { mutableStateOf(vm.callExcludeNumbers) }
    // Refresh key so the SIM list re-reads after permission is granted.
    var simRefresh by remember { mutableIntStateOf(0) }
    val sims = remember(simRefresh) { SimUtil.listSims(context) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { simRefresh++ }

    fun requestCallPerms() {
        val needed = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) { simRefresh++; Toast.makeText(context, "All permissions granted", Toast.LENGTH_SHORT).show() }
        else permLauncher.launch(missing.toTypedArray())
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text("Call Auto-Reply", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Reply automatically after missed, incoming or outgoing calls.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Master + permission
        CardBox {
            ToggleRow("Enable call auto-reply", "Master switch", vm.callReplyEnabled) { on ->
                vm.setCallReply(on); if (on) requestCallPerms()
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { requestCallPerms() }, modifier = Modifier.fillMaxWidth()) {
                Text("Grant call & SMS permissions")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Per-event cards
        EventCard(
            title = "Missed calls", subtitle = "Rang but not answered",
            enabled = vm.callOnMissed, onEnabled = { vm.updateCallOnMissed(it) },
            message = vm.msgMissed, onMessage = { vm.updateMsgMissed(it) },
            channel = vm.channelMissed, onChannel = { vm.updateChannelMissed(it) }
        )
        Spacer(Modifier.height(12.dp))
        EventCard(
            title = "Incoming (answered)", subtitle = "After an answered incoming call",
            enabled = vm.callOnIncoming, onEnabled = { vm.updateCallOnIncoming(it) },
            message = vm.msgIncoming, onMessage = { vm.updateMsgIncoming(it) },
            channel = vm.channelIncoming, onChannel = { vm.updateChannelIncoming(it) }
        )
        Spacer(Modifier.height(12.dp))
        EventCard(
            title = "Outgoing calls", subtitle = "After a call you made",
            enabled = vm.callOnOutgoing, onEnabled = { vm.updateCallOnOutgoing(it) },
            message = vm.msgOutgoing, onMessage = { vm.updateMsgOutgoing(it) },
            channel = vm.channelOutgoing, onChannel = { vm.updateChannelOutgoing(it) }
        )

        Spacer(Modifier.height(12.dp))

        // SIM
        CardBox("SIM") {
            ToggleRow(
                "Reply from the same SIM",
                "Use whichever SIM the call came on (when the phone reports it)",
                vm.callReplyFromSameSim
            ) { vm.setCallSameSim(it) }
            Spacer(Modifier.height(6.dp))
            Text("Default SIM (fallback)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            sims.forEach { sim ->
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(selected = vm.callSimSubId == sim.subId, onClick = { vm.setCallSim(sim.subId) })
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = vm.callSimSubId == sim.subId, onClick = { vm.setCallSim(sim.subId) })
                    Text(sim.label, fontSize = 14.sp)
                }
            }
            if (sims.size <= 1) {
                Text(
                    "Only Default shows until permission is granted. Tap “Grant” above, allow Phone, then SIM 1 / SIM 2 will appear.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { simRefresh++ }, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh SIM list")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Exclude
        CardBox("Exclude numbers") {
            Text("These numbers never get an auto-reply. One per line or comma separated.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = exclude, onValueChange = { exclude = it },
                label = { Text("Excluded numbers") },
                modifier = Modifier.fillMaxWidth().height(90.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.setCallExclude(exclude.trim()); Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save excluded numbers") }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EventCard(
    title: String, subtitle: String,
    enabled: Boolean, onEnabled: (Boolean) -> Unit,
    message: String, onMessage: (String) -> Unit,
    channel: String, onChannel: (String) -> Unit
) {
    var text by remember(message) { mutableStateOf(message) }
    CardBox(title) {
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        ToggleRow("Auto-reply for this", "Turn on to reply on these calls", enabled, onEnabled)

        Spacer(Modifier.height(8.dp))
        Text("Send via", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(channel == "sms", { onChannel("sms") }, { Text("SMS") })
            FilterChip(channel == "wa", { onChannel("wa") }, { Text("WhatsApp") })
            FilterChip(channel == "both", { onChannel("both") }, { Text("Both") })
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth().height(96.dp)
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { onMessage(text.trim()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save message")
        }
    }
}

@Composable
private fun CardBox(title: String? = null, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
