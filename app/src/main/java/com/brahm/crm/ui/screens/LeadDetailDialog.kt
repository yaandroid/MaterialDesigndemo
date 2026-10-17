package com.brahm.crm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brahm.crm.data.Lead
import com.brahm.crm.data.Stage
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.ui.components.AiReplySheet

@Composable
fun LeadDetailDialog(vm: AppViewModel, lead: Lead, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(lead.name) }
    var phone by remember { mutableStateOf(lead.phone) }
    var email by remember { mutableStateOf(lead.email) }
    var note by remember { mutableStateOf(lead.note) }
    var stage by remember { mutableStateOf(Stage.from(lead.stage)) }
    var newTask by remember { mutableStateOf("") }
    var showAi by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lead details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(12.dp))
                Text("Stage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stage.entries.forEach { s ->
                        FilterChip(
                            selected = stage == s,
                            onClick = { stage = s },
                            label = { Text(s.label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(onClick = { showAi = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text("  AI reply draft")
                }

                Spacer(Modifier.height(12.dp))
                Text("Quick task", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        newTask, { newTask = it },
                        placeholder = { Text("e.g. Call tomorrow") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { if (newTask.isNotBlank()) { vm.addTask(newTask, lead.id); newTask = "" } }
                    ) { Text("Add") }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { vm.deleteLead(lead); onDismiss() }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.height(18.dp))
                    Text("  Delete lead", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.updateLead(
                    lead.copy(name = name, phone = phone, email = email, note = note, stage = stage.name)
                )
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showAi) {
        AiReplySheet(
            vm = vm,
            person = name,
            incomingText = note,
            phone = phone,
            onDismiss = { showAi = false }
        )
    }
}
