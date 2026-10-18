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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.brahm.crm.ai.AiResult
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.util.asRelativeTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(vm: AppViewModel) {
    val scroll = rememberScrollState()
    val broadcasts by vm.scheduledBroadcasts.collectAsState()
    val replies by vm.scheduledReplies.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val inboxCount by vm.inboxPeopleCount.collectAsState()

    var loading by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text("Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Today's picture — messages, what's important, what's scheduled, tasks.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // ---- AI daily analysis ----
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("AI daily analysis", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        loading = true; error = null
                        vm.analyzeToday { res ->
                            loading = false
                            when (res) {
                                is AiResult.Ok -> summary = res.text
                                is AiResult.Err -> error = res.message
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.height(0.dp)); Text("  Analysing…")
                    } else Text("Analyse today's messages")
                }
                error?.let {
                    Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                if (summary.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(summary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("AI-generated — review before acting.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Scheduled ----
        SectionCard("Scheduled messages") {
            val pendingB = broadcasts.filter { !it.sent }
            val pendingR = replies.filter { !it.sent }
            if (pendingB.isEmpty() && pendingR.isEmpty()) {
                Text("Nothing scheduled.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pendingB.forEach { b ->
                    val count = b.phonesCsv.split(",").filter { it.isNotBlank() }.size
                    Text(
                        "• ${b.channel.uppercase()} → $count contact(s) · ${fmt.format(Date(b.dueAt))}",
                        fontSize = 13.sp
                    )
                }
                pendingR.forEach { r ->
                    Text(
                        "• Reply → ${r.person} · ${fmt.format(Date(r.dueAt))}",
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Today's tasks ----
        SectionCard("Today's tasks") {
            val open = tasks.filter { !it.done }
            if (open.isEmpty()) {
                Text("No open tasks.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                open.take(12).forEach { t ->
                    Text("• ${t.title}", fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "People in inbox: $inboxCount",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}
