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

    var smsText by remember { mutableStateOf(vm.callSmsText) }
    var waText by remember { mutableStateOf(vm.callWhatsappText) }
    var exclude by remember { mutableStateOf(vm.callExcludeNumbers) }
    val sims = remember { SimUtil.listSims(context) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled by system; user can re-toggle */ }

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
        if (missing.isEmpty()) {
            Toast.makeText(context, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            permLauncher.launch(missing.toTypedArray())
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text("Call Auto-Reply", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "When you miss an incoming call, reply automatically.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Master switch
        CardBox {
            ToggleRow(
                "Enable missed-call auto-reply",
                "Master switch for this feature",
                vm.callReplyEnabled
            ) { on ->
                vm.setCallReply(on)
                if (on) requestCallPerms()
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { requestCallPerms() }, modifier = Modifier.fillMaxWidth()) {
                Text("Grant call & SMS permissions")
            }
        }

        Spacer(Modifier.height(12.dp))

        // SMS
        CardBox("Auto SMS reply") {
            ToggleRow(
                "Send an SMS on missed call",
                "A text reply to the caller",
                vm.callSmsEnabled
            ) { vm.setCallSms(it) }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = smsText,
                onValueChange = { smsText = it },
                label = { Text("SMS message") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Text("Send from", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            sims.forEach { sim ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = vm.callSimSubId == sim.subId,
                            onClick = { vm.setCallSim(sim.subId) }
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = vm.callSimSubId == sim.subId,
                        onClick = { vm.setCallSim(sim.subId) }
                    )
                    Text(sim.label, fontSize = 14.sp)
                }
            }
            if (sims.size <= 1) {
                Text(
                    "Grant permission above to list SIM 1 / SIM 2.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // WhatsApp assisted
        CardBox("WhatsApp reply (one-tap)") {
            ToggleRow(
                "Offer a WhatsApp reply",
                "A notification opens WhatsApp with your message ready — you press Send",
                vm.callWhatsappEnabled
            ) { vm.setCallWhatsapp(it) }

            Spacer(Modifier.height(8.dp))
            Text("Which WhatsApp?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.callWhatsappTarget == "wa",
                    onClick = { vm.setCallWhatsappTarget("wa") },
                    label = { Text("Normal") }
                )
                FilterChip(
                    selected = vm.callWhatsappTarget == "wab",
                    onClick = { vm.setCallWhatsappTarget("wab") },
                    label = { Text("Business") }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = waText,
                onValueChange = { waText = it },
                label = { Text("WhatsApp message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Safe mode: the app never presses Send for you — it opens the chat with the message filled in, and you tap Send.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        // Exclude
        CardBox("Exclude numbers") {
            Text(
                "These numbers never get an auto-reply. One per line, or comma separated.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = exclude,
                onValueChange = { exclude = it },
                label = { Text("Excluded numbers") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                vm.setCallSmsText(smsText.trim())
                vm.setCallWhatsappText(waText.trim())
                vm.setCallExclude(exclude.trim())
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Call Auto-Reply settings") }

        Spacer(Modifier.height(24.dp))
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
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
