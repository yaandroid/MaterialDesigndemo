package com.brahm.crm.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.brahm.crm.data.Lead
import com.brahm.crm.data.Stage
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.ui.components.Avatar
import com.brahm.crm.ui.components.EmptyState
import com.brahm.crm.ui.components.StageChip

@Composable
fun LeadsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val leads by vm.leads.collectAsState()
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Lead?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.importContacts { res ->
                Toast.makeText(
                    context,
                    "Imported ${res.imported}, skipped ${res.skipped}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(context, "Contacts permission needed to import", Toast.LENGTH_SHORT).show()
        }
    }

    fun startImport() {
        if (!vm.contactsEnabled) {
            Toast.makeText(context, "Contacts feature is off (see Settings)", Toast.LENGTH_SHORT).show()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.importContacts { res ->
                Toast.makeText(context, "Imported ${res.imported}, skipped ${res.skipped}", Toast.LENGTH_LONG).show()
            }
        } else {
            importLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val filtered = remember(leads, query) {
        if (query.isBlank()) leads
        else leads.filter {
            it.name.contains(query, true) || it.phone.contains(query, true)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                text = { Text("Add lead") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search leads") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { startImport() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.ContactPage, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.height(0.dp))
                Text("  Import phone contacts")
            }
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyState(
                    "No leads yet",
                    "Tap “Add lead”, or import your phone contacts to get started.",
                    Icons.Filled.PersonOutline
                )
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { lead ->
                        LeadRow(lead) { detail = lead }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AddLeadDialog(
            onDismiss = { showAdd = false },
            onSave = { n, p, e, note -> vm.addLead(n, p, e, note); showAdd = false }
        )
    }

    detail?.let { lead ->
        LeadDetailDialog(
            vm = vm,
            lead = lead,
            onDismiss = { detail = null }
        )
    }
}

@Composable
private fun LeadRow(lead: Lead, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(lead.name)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(lead.name, fontWeight = FontWeight.SemiBold)
                val sub = listOf(lead.phone, lead.source).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            StageChip(Stage.from(lead.stage))
        }
    }
}

@Composable
private fun AddLeadDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add lead") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, phone, email, note) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
