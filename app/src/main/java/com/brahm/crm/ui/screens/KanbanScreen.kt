package com.brahm.crm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brahm.crm.data.Lead
import com.brahm.crm.data.Stage
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.ui.components.Avatar
import com.brahm.crm.ui.components.stageColor

@Composable
fun KanbanScreen(vm: AppViewModel) {
    val leads by vm.leads.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Board", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Drag your leads through the stages",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Stage.entries.forEach { stage ->
                KanbanColumn(
                    stage = stage,
                    leads = leads.filter { Stage.from(it.stage) == stage },
                    onMove = { lead, target -> vm.moveStage(lead, target) }
                )
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    stage: Stage,
    leads: List<Lead>,
    onMove: (Lead, Stage) -> Unit
) {
    val color = stageColor(stage)
    Column(
        Modifier
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.height(10.dp).width(10.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(stage.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(6.dp))
            Text("(${leads.size})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))

        if (leads.isEmpty()) {
            Text(
                "No leads here",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(leads, key = { it.id }) { lead ->
                    KanbanCard(lead, stage, onMove)
                }
            }
        }
    }
}

@Composable
private fun KanbanCard(lead: Lead, stage: Stage, onMove: (Lead, Stage) -> Unit) {
    val stages = Stage.entries
    val idx = stages.indexOf(stage)
    val prev = stages.getOrNull(idx - 1)
    val next = stages.getOrNull(idx + 1)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(lead.name, size = 32)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(lead.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (lead.phone.isNotBlank()) {
                        Text(lead.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (lead.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    lead.note, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { prev?.let { onMove(lead, it) } },
                    enabled = prev != null
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Move back",
                        tint = if (prev != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick = { next?.let { onMove(lead, it) } },
                    enabled = next != null
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Move forward",
                        tint = if (next != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
