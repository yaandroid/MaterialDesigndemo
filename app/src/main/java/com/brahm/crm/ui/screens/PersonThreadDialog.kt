package com.brahm.crm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brahm.crm.data.InboxPerson
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.ui.components.AiReplySheet
import com.brahm.crm.util.asRelativeTime

@Composable
fun PersonThreadDialog(vm: AppViewModel, person: InboxPerson, onDismiss: () -> Unit) {
    val messages by vm.notificationsForPerson(person.person).collectAsState()
    var showAi by remember { mutableStateOf(false) }
    val latestText = messages.firstOrNull()?.text ?: person.lastText

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(person.person) },
        text = {
            Column {
                Text(
                    person.appLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { m ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(m.text, fontSize = 14.sp)
                                Row {
                                    Text(
                                        m.postedAt.asRelativeTime(),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    if (m.deleted) {
                                        Text(
                                            "  · deleted by sender (backup kept)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showAi = true }) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.height(18.dp))
                Text("  AI reply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showAi) {
        AiReplySheet(
            vm = vm,
            person = person.person,
            incomingText = latestText,
            phone = null,
            onDismiss = { showAi = false }
        )
    }
}
