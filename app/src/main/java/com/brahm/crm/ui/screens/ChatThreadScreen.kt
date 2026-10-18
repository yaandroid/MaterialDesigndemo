package com.brahm.crm.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.brahm.crm.ai.AiResult
import com.brahm.crm.data.PersonSetting
import com.brahm.crm.data.Stage
import com.brahm.crm.service.ScheduledReplyReceiver
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.util.Share
import com.brahm.crm.util.asRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(vm: AppViewModel, person: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val messages by vm.notificationsForPerson(person).collectAsState()
    val setting by vm.personSetting(person).collectAsState()
    val scheduled by vm.scheduledFor(person).collectAsState()
    val listState = rememberLazyListState()

    var menuOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSchedule by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }

    val latestText = messages.lastOrNull()?.text ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(person, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(
                            "Inbox chat",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSchedule = true }) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Schedule reply")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Settings")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Chat settings") },
                            onClick = { menuOpen = false; showSettings = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Schedule a reply") },
                            onClick = { menuOpen = false; showSchedule = true }
                        )
                        Stage.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text("Move to: ${st.label}") },
                                onClick = {
                                    menuOpen = false
                                    vm.movePersonToBoard(person, "", st)
                                    Toast.makeText(context, "Moved to ${st.label}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (!vm.aiEnabled) {
                            Toast.makeText(context, "AI is off (Settings)", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        aiLoading = true
                        vm.draftReply(latestText, person) { res ->
                            aiLoading = false
                            when (res) {
                                is AiResult.Ok -> draft = res.text
                                is AiResult.Err -> Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "AI draft",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(if (aiLoading) "AI is drafting…" else "Type a reply") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (draft.isBlank()) return@IconButton
                            Share.copyToClipboard(context, "reply", draft)
                            // open WhatsApp if we can infer, else share sheet
                            Share.shareText(context, draft)
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (scheduled.any { !it.sent }) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "⏰ ${scheduled.count { !it.sent }} reply scheduled",
                        Modifier.fillMaxWidth().padding(8.dp),
                        fontSize = 12.sp
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(messages, key = { it.id }) { m ->
                    IncomingBubble(m.text, m.postedAt.asRelativeTime(), m.deleted)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size)
    }

    if (showSettings) {
        PersonSettingsSheet(
            current = setting ?: PersonSetting(person),
            onSave = { vm.savePersonSetting(it); showSettings = false },
            onDismiss = { showSettings = false }
        )
    }

    if (showSchedule) {
        ScheduleReplyFlow(
            context = context,
            defaultText = draft.ifBlank { "" },
            onDismiss = { showSchedule = false },
            onConfirm = { text, whenMs, target ->
                vm.addScheduledReply(person, "", text, whenMs, target) { id ->
                    ScheduledReplyReceiver.schedule(context, id, whenMs)
                }
                Toast.makeText(context, "Reply scheduled", Toast.LENGTH_SHORT).show()
                showSchedule = false
            }
        )
    }
}

@Composable
private fun IncomingBubble(text: String, time: String, deleted: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Row {
                    Text(time, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    if (deleted) {
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
