package com.brahm.crm.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brahm.crm.BrahmApp
import com.brahm.crm.ai.AiResult
import com.brahm.crm.ai.GroqClient
import com.brahm.crm.data.InboxPerson
import com.brahm.crm.data.Lead
import com.brahm.crm.data.NotificationLog
import com.brahm.crm.data.PersonSetting
import com.brahm.crm.data.ScheduledBroadcast
import com.brahm.crm.data.ScheduledReply
import com.brahm.crm.data.Stage
import com.brahm.crm.data.TaskItem
import com.brahm.crm.util.ContactsImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = BrahmApp.db
    private val settings = BrahmApp.settings

    // ---------- Streams ----------
    val leads: StateFlow<List<Lead>> =
        db.leadDao().observeAll().stateInVm(emptyList())

    val tasks: StateFlow<List<TaskItem>> =
        db.taskDao().observeAll().stateInVm(emptyList())

    val inboxPeople: StateFlow<List<InboxPerson>> =
        db.notificationDao().observePeople().stateInVm(emptyList())

    val leadCount: StateFlow<Int> = db.leadDao().countAll().stateInVm(0)
    val newCount: StateFlow<Int> = db.leadDao().countByStage(Stage.NEW.name).stateInVm(0)
    val followCount: StateFlow<Int> = db.leadDao().countByStage(Stage.FOLLOW_UP.name).stateInVm(0)
    val closedCount: StateFlow<Int> = db.leadDao().countByStage(Stage.CLOSED.name).stateInVm(0)
    val openTaskCount: StateFlow<Int> = db.taskDao().countOpen().stateInVm(0)
    val inboxPeopleCount: StateFlow<Int> = db.notificationDao().countPeople().stateInVm(0)

    fun notificationsForPerson(person: String): StateFlow<List<NotificationLog>> =
        db.notificationDao().observeForPerson(person).stateInVm(emptyList())

    // ---- Per-person settings ----
    fun personSetting(person: String): StateFlow<PersonSetting?> =
        db.personSettingDao().observe(person).stateInVm(null)

    fun savePersonSetting(s: PersonSetting) = viewModelScope.launch {
        db.personSettingDao().upsert(s)
    }

    /** Move a person onto the Kanban board at the given stage (creates a lead if needed). */
    fun movePersonToBoard(person: String, phone: String, stage: Stage) = viewModelScope.launch {
        val existing = db.personSettingDao().get(person)
        db.personSettingDao().upsert(
            (existing ?: PersonSetting(person)).copy(boardStage = stage.name)
        )
        // find or create a lead for this person
        val leadList = db.leadDao()
        val current = leads.value.firstOrNull {
            it.name.equals(person, true) || (phone.isNotBlank() && it.phone == phone)
        }
        if (current == null) {
            leadList.upsert(Lead(name = person, phone = phone, source = "Inbox", stage = stage.name))
        } else {
            leadList.update(current.copy(stage = stage.name, updatedAt = System.currentTimeMillis()))
        }
    }

    // ---- Scheduled replies ----
    val scheduledReplies: StateFlow<List<ScheduledReply>> =
        db.scheduledReplyDao().observeAll().stateInVm(emptyList())

    fun scheduledFor(person: String): StateFlow<List<ScheduledReply>> =
        db.scheduledReplyDao().observeForPerson(person).stateInVm(emptyList())

    fun addScheduledReply(
        person: String, phone: String, text: String, dueAt: Long, target: String,
        onScheduled: (Long) -> Unit
    ) = viewModelScope.launch {
        val id = db.scheduledReplyDao().upsert(
            ScheduledReply(person = person, phone = phone, text = text, dueAt = dueAt, target = target)
        )
        onScheduled(id)
    }

    fun deleteScheduledReply(r: ScheduledReply) = viewModelScope.launch {
        db.scheduledReplyDao().delete(r)
    }

    // ---- Bulk broadcast ----
    val scheduledBroadcasts: StateFlow<List<ScheduledBroadcast>> =
        db.scheduledBroadcastDao().observeAll().stateInVm(emptyList())

    fun saveScheduledBroadcast(
        channel: String, message: String, phones: List<String>, dueAt: Long,
        onSaved: (Long) -> Unit
    ) = viewModelScope.launch {
        val id = db.scheduledBroadcastDao().upsert(
            ScheduledBroadcast(
                channel = channel, message = message,
                phonesCsv = phones.joinToString(","), dueAt = dueAt
            )
        )
        onSaved(id)
    }

    fun deleteScheduledBroadcast(b: ScheduledBroadcast) = viewModelScope.launch {
        db.scheduledBroadcastDao().delete(b)
    }

    /** SIM selected for SMS (shared with call auto-reply). */
    val smsSimSubId: Int get() = settings.callSimSubId

    /** Sends SMS to each (phone → message) with a delay (seconds) between each. */
    fun sendSmsSequential(
        perPhone: List<Pair<String, String>>,
        delaySeconds: Int,
        onProgress: (done: Int, total: Int) -> Unit,
        onDone: (sent: Int, failed: Int) -> Unit
    ) = viewModelScope.launch {
        var sent = 0
        var failed = 0
        val total = perPhone.size
        perPhone.forEachIndexed { index, (phone, body) ->
            val ok = com.brahm.crm.service.BulkSender.sendOne(getApplication(), phone, body, settings.callSimSubId)
            if (ok) sent++ else failed++
            onProgress(index + 1, total)
            if (delaySeconds > 0 && index < total - 1) {
                kotlinx.coroutines.delay(delaySeconds * 1000L)
            }
        }
        onDone(sent, failed)
    }

    // ---------- Settings as UI state ----------
    var groqKey by mutableStateOf(settings.groqApiKey); private set
    var groqModel by mutableStateOf(settings.groqModel); private set
    var inboxEnabled by mutableStateOf(settings.inboxEnabled); private set
    var aiEnabled by mutableStateOf(settings.aiEnabled); private set
    var contactsEnabled by mutableStateOf(settings.contactsEnabled); private set
    var replyTone by mutableStateOf(settings.defaultReplyTone); private set
    var themeMode by mutableStateOf(settings.themeMode); private set
    fun updateThemeMode(v: String) { settings.themeMode = v; themeMode = v }

    // ---- Call auto-reply as UI state ----
    var callReplyEnabled by mutableStateOf(settings.callReplyEnabled); private set
    var callSmsEnabled by mutableStateOf(settings.callSmsEnabled); private set
    var callSmsText by mutableStateOf(settings.callSmsText); private set
    var callSimSubId by mutableStateOf(settings.callSimSubId); private set
    var callWhatsappEnabled by mutableStateOf(settings.callWhatsappEnabled); private set
    var callWhatsappTarget by mutableStateOf(settings.callWhatsappTarget); private set
    var callWhatsappText by mutableStateOf(settings.callWhatsappText); private set
    var callExcludeNumbers by mutableStateOf(settings.callExcludeNumbers); private set
    var callReplyFromSameSim by mutableStateOf(settings.callReplyFromSameSim); private set
    var callOnMissed by mutableStateOf(settings.callOnMissed); private set
    var callOnIncoming by mutableStateOf(settings.callOnIncoming); private set
    var callOnOutgoing by mutableStateOf(settings.callOnOutgoing); private set
    var bulkDelaySeconds by mutableStateOf(settings.bulkDelaySeconds); private set
    var msgMissed by mutableStateOf(settings.msgMissed); private set
    var msgIncoming by mutableStateOf(settings.msgIncoming); private set
    var msgOutgoing by mutableStateOf(settings.msgOutgoing); private set
    var channelMissed by mutableStateOf(settings.channelMissed); private set
    var channelIncoming by mutableStateOf(settings.channelIncoming); private set
    var channelOutgoing by mutableStateOf(settings.channelOutgoing); private set

    fun saveGroqKey(v: String) { settings.groqApiKey = v; groqKey = v }
    fun saveGroqModel(v: String) { settings.groqModel = v; groqModel = v }
    fun setInbox(v: Boolean) { settings.inboxEnabled = v; inboxEnabled = v }
    fun setAi(v: Boolean) { settings.aiEnabled = v; aiEnabled = v }
    fun setContacts(v: Boolean) { settings.contactsEnabled = v; contactsEnabled = v }
    fun setTone(v: String) { settings.defaultReplyTone = v; replyTone = v }

    fun setCallReply(v: Boolean) { settings.callReplyEnabled = v; callReplyEnabled = v }
    fun setCallSms(v: Boolean) { settings.callSmsEnabled = v; callSmsEnabled = v }
    fun updateCallSmsText(v: String) { settings.callSmsText = v; callSmsText = v }
    fun setCallSim(v: Int) { settings.callSimSubId = v; callSimSubId = v }
    fun setCallWhatsapp(v: Boolean) { settings.callWhatsappEnabled = v; callWhatsappEnabled = v }
    fun updateCallWhatsappTarget(v: String) { settings.callWhatsappTarget = v; callWhatsappTarget = v }
    fun updateCallWhatsappText(v: String) { settings.callWhatsappText = v; callWhatsappText = v }
    fun setCallExclude(v: String) { settings.callExcludeNumbers = v; callExcludeNumbers = v }
    fun setCallSameSim(v: Boolean) { settings.callReplyFromSameSim = v; callReplyFromSameSim = v }
    fun updateCallOnMissed(v: Boolean) { settings.callOnMissed = v; callOnMissed = v }
    fun updateCallOnIncoming(v: Boolean) { settings.callOnIncoming = v; callOnIncoming = v }
    fun updateCallOnOutgoing(v: Boolean) { settings.callOnOutgoing = v; callOnOutgoing = v }
    fun setBulkDelay(v: Int) { settings.bulkDelaySeconds = v; bulkDelaySeconds = v }
    fun updateMsgMissed(v: String) { settings.msgMissed = v; msgMissed = v }
    fun updateMsgIncoming(v: String) { settings.msgIncoming = v; msgIncoming = v }
    fun updateMsgOutgoing(v: String) { settings.msgOutgoing = v; msgOutgoing = v }
    fun updateChannelMissed(v: String) { settings.channelMissed = v; channelMissed = v }
    fun updateChannelIncoming(v: String) { settings.channelIncoming = v; channelIncoming = v }
    fun updateChannelOutgoing(v: String) { settings.channelOutgoing = v; channelOutgoing = v }

    // ---------- Lead actions ----------
    fun addLead(name: String, phone: String, email: String, note: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            db.leadDao().upsert(
                Lead(name = name.trim(), phone = phone.trim(), email = email.trim(), note = note.trim())
            )
        }
    }

    fun updateLead(lead: Lead) = viewModelScope.launch {
        db.leadDao().update(lead.copy(updatedAt = System.currentTimeMillis()))
    }

    fun deleteLead(lead: Lead) = viewModelScope.launch { db.leadDao().delete(lead) }

    fun moveStage(lead: Lead, stage: Stage) = viewModelScope.launch {
        db.leadDao().update(lead.copy(stage = stage.name, updatedAt = System.currentTimeMillis()))
    }

    // ---------- Task actions ----------
    fun addTask(title: String, leadId: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch { db.taskDao().upsert(TaskItem(title = title.trim(), leadId = leadId)) }
    }

    fun toggleTask(task: TaskItem) = viewModelScope.launch {
        db.taskDao().update(task.copy(done = !task.done))
    }

    fun deleteTask(task: TaskItem) = viewModelScope.launch { db.taskDao().delete(task) }

    // ---------- Contacts ----------
    fun importContacts(onDone: (ContactsImporter.Result) -> Unit) = viewModelScope.launch {
        val res = ContactsImporter.importInto(getApplication(), db)
        onDone(res)
    }

    // ---------- Inbox ----------
    fun clearInbox() = viewModelScope.launch { db.notificationDao().clearAll() }

    // ---------- Daily AI analysis ----------
    fun analyzeToday(onResult: (AiResult) -> Unit) {
        viewModelScope.launch {
            val startOfDay = run {
                val c = java.util.Calendar.getInstance()
                c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
                c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            val msgs = db.notificationDao().since(startOfDay)
            val block = msgs.joinToString("\n") { "- ${it.person} (${it.appLabel}): ${it.text}" }
            val res = GroqClient.analyzeDay(settings.groqApiKey, settings.groqModel, block)
            onResult(res)
        }
    }

    // ---------- AI ----------
    fun draftReply(incomingText: String, person: String, onResult: (AiResult) -> Unit) {
        viewModelScope.launch {
            val res = GroqClient.draftReply(
                apiKey = settings.groqApiKey,
                model = settings.groqModel,
                incomingText = incomingText,
                personName = person,
                tone = settings.defaultReplyTone
            )
            onResult(res)
        }
    }

    // ---------- helpers ----------
    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInVm(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)
}
