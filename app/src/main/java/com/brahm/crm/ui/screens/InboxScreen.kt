package com.brahm.crm.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.brahm.crm.data.InboxPerson
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.ui.components.Avatar
import com.brahm.crm.ui.components.EmptyState
import com.brahm.crm.util.Permissions
import com.brahm.crm.util.asRelativeTime

@Composable
fun InboxScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val people by vm.inboxPeople.collectAsState()
    var openPerson by remember { mutableStateOf<InboxPerson?>(null) }
    var query by remember { mutableStateOf("") }
    // recomputed each recomposition (cheap); reflects whether access is granted
    val accessGranted = Permissions.isNotificationAccessGranted(context)

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Inbox", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Your incoming messages, grouped by person",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (people.isNotEmpty()) {
                TextButton(onClick = { vm.clearInbox() }) { Text("Clear") }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (!vm.inboxEnabled) {
            InfoCard("Inbox feature is turned off. Turn it on in Settings to collect messages.")
        } else if (!accessGranted) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Notification access needed",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "To read your own incoming notifications into this inbox, grant access to “BRAHM CRM Inbox Reader”.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { Permissions.openNotificationAccessSettings(context) }) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.height(18.dp))
                        Text("  Grant notification access")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (people.isEmpty()) {
            EmptyState(
                "No messages yet",
                "Once access is on, your incoming notifications will appear here grouped per person.",
                Icons.Filled.Inbox
            )
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search people or messages") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            val filtered = if (query.isBlank()) people else people.filter {
                it.person.contains(query, true) ||
                    it.appLabel.contains(query, true) ||
                    it.lastText.contains(query, true)
            }
            if (filtered.isEmpty()) {
                Text(
                    "No matches.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.person + it.appLabel }) { p ->
                        PersonRow(p) { openPerson = p }
                    }
                }
            }
        }
    }

    openPerson?.let { p ->
        PersonThreadDialog(vm = vm, person = p, onDismiss = { openPerson = null })
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, Modifier.padding(16.dp), fontSize = 13.sp)
    }
}

@Composable
private fun PersonRow(p: InboxPerson, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.person)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.person, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(p.lastAt.asRelativeTime(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    p.lastText, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                )
                Text(
                    "${p.appLabel} · ${p.count} message(s)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
