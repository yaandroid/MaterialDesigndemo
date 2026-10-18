package com.brahm.crm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.util.Permissions

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    var key by remember { mutableStateOf(vm.groqKey) }
    var model by remember { mutableStateOf(vm.groqModel) }
    var tone by remember { mutableStateOf(vm.replyTone) }
    var showKey by remember { mutableStateOf(false) }

    val leadCount by vm.leadCount.collectAsState()
    val inboxCount by vm.inboxPeopleCount.collectAsState()
    val accessGranted = Permissions.isNotificationAccessGranted(context)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // ---- Appearance ----
        SettingsCard("Appearance") {
            Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (v, label) ->
                    FilterChip(
                        selected = vm.themeMode == v,
                        onClick = { vm.updateThemeMode(v) },
                        label = { Text(label) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- AI / Groq ----
        SettingsCard("AI (Groq)") {
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Groq API key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tone,
                onValueChange = { tone = it },
                label = { Text("Reply tone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    vm.saveGroqKey(key.trim())
                    vm.saveGroqModel(model.trim())
                    vm.setTone(tone.trim())
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save AI settings") }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Feature toggles ----
        SettingsCard("Features") {
            ToggleRow(
                "AI reply drafts",
                "Generate reply suggestions with Groq",
                vm.aiEnabled
            ) { vm.setAi(it) }
            ToggleRow(
                "Inbox (notification reader)",
                "Collect your incoming notifications per person",
                vm.inboxEnabled
            ) { vm.setInbox(it) }
            ToggleRow(
                "Contacts import",
                "Allow importing phone contacts as leads",
                vm.contactsEnabled
            ) { vm.setContacts(it) }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Permissions ----
        SettingsCard("Permissions") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Notification access", fontWeight = FontWeight.Medium)
                    Text(
                        if (accessGranted) "Granted" else "Not granted",
                        fontSize = 12.sp,
                        color = if (accessGranted) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = { Permissions.openNotificationAccessSettings(context) }) {
                    Text(if (accessGranted) "Manage" else "Grant")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Customer data ----
        SettingsCard("Customer data") {
            Text("Leads / contacts: $leadCount", fontSize = 13.sp)
            Text("People in inbox: $inboxCount", fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    vm.clearInbox()
                    Toast.makeText(context, "Inbox data cleared", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear inbox data") }
            Spacer(Modifier.height(4.dp))
            Text(
                "Lead records stay saved on this device only. Delete a lead from its card in the Leads tab.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))
        // ---- Branding footer ----
        Text(
            "Brahm InnerBrain Technologies Pvt. Ltd.",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Developed by Sandeep Kushwah", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Support: 7990918709", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Version 1.1", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
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
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
