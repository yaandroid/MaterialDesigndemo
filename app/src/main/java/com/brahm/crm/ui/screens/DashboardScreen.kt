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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brahm.crm.data.Stage
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.ui.components.Avatar
import com.brahm.crm.ui.components.SectionTitle
import com.brahm.crm.ui.components.StageChip
import com.brahm.crm.ui.components.StatCard

@Composable
fun DashboardScreen(vm: AppViewModel, onNavigate: (String) -> Unit = {}) {
    val leadCount by vm.leadCount.collectAsState()
    val newCount by vm.newCount.collectAsState()
    val followCount by vm.followCount.collectAsState()
    val closedCount by vm.closedCount.collectAsState()
    val openTasks by vm.openTaskCount.collectAsState()
    val leads by vm.leads.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Your Dashboard",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Lead & customer overview",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Contacts", leadCount.toString(), Icons.Filled.Groups, Color(0xFF5B4BD6), Modifier.weight(1f))
            StatCard("New leads", newCount.toString(), Icons.Filled.PersonAdd, Color(0xFF3A7BD5), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Follow-up", followCount.toString(), Icons.Filled.Schedule, Color(0xFFE08A00), Modifier.weight(1f))
            StatCard("Open tasks", openTasks.toString(), Icons.AutoMirrored.Filled.List, Color(0xFF00897B), Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Quick access")
        QuickGrid(onNavigate)

        Spacer(Modifier.height(20.dp))
        SectionTitle("Pipeline")
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                PipelineRow("New", newCount, leadCount, Color(0xFF5B4BD6))
                Spacer(Modifier.height(10.dp))
                PipelineRow("Follow-up", followCount, leadCount, Color(0xFFE08A00))
                Spacer(Modifier.height(10.dp))
                PipelineRow("Closed", closedCount, leadCount, Color(0xFF2E9E6B))
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Recent leads")
        if (leads.isEmpty()) {
            Text(
                "No leads yet. Add one from the Leads tab, or import your contacts.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            leads.take(5).forEach { lead ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Avatar(lead.name)
                        Spacer(Modifier.height(0.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(lead.name, fontWeight = FontWeight.SemiBold)
                            if (lead.phone.isNotBlank()) {
                                Text(
                                    lead.phone, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StageChip(Stage.from(lead.stage))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private data class Shortcut(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)

@Composable
private fun QuickGrid(onNavigate: (String) -> Unit) {
    val items = listOf(
        Shortcut("Leads", Icons.Filled.People, "leads"),
        Shortcut("Board", Icons.Filled.ViewKanban, "kanban"),
        Shortcut("Inbox", Icons.Filled.Inbox, "inbox"),
        Shortcut("Broadcast", Icons.Filled.Campaign, "broadcast"),
        Shortcut("Call Reply", Icons.Filled.PhoneCallback, "call_reply"),
        Shortcut("Analytics", Icons.Filled.Insights, "analytics"),
        Shortcut("WA Web", Icons.Filled.Language, "wa_web"),
        Shortcut("Settings", Icons.Filled.Settings, "settings")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { s ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clickable { onNavigate(s.route) }
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(8.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.Icon(
                                s.icon, contentDescription = s.label,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(26.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(s.label, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PipelineRow(label: String, value: Int, total: Int, color: Color) {
    val fraction = if (total <= 0) 0f else value.toFloat() / total.toFloat()
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("$value", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}
