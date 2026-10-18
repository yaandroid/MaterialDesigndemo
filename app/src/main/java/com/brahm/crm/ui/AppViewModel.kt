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

    // ---------- Settings as UI state ----------
    var groqKey by mutableStateOf(settings.groqApiKey); private set
    var groqModel by mutableStateOf(settings.groqModel); private set
    var inboxEnabled by mutableStateOf(settings.inboxEnabled); private set
    var aiEnabled by mutableStateOf(settings.aiEnabled); private set
    var contactsEnabled by mutableStateOf(settings.contactsEnabled); private set
    var replyTone by mutableStateOf(settings.defaultReplyTone); private set

    // ---- Call auto-reply as UI state ----
    var callReplyEnabled by mutableStateOf(settings.callReplyEnabled); private set
    var callSmsEnabled by mutableStateOf(settings.callSmsEnabled); private set
    var callSmsText by mutableStateOf(settings.callSmsText); private set
    var callSimSubId by mutableStateOf(settings.callSimSubId); private set
    var callWhatsappEnabled by mutableStateOf(settings.callWhatsappEnabled); private set
    var callWhatsappTarget by mutableStateOf(settings.callWhatsappTarget); private set
    var callWhatsappText by mutableStateOf(settings.callWhatsappText); private set
    var callExcludeNumbers by mutableStateOf(settings.callExcludeNumbers); private set

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
