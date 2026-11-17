package dev.focusloop.spike.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.focusloop.spike.SpikeState
import dev.focusloop.spike.domain.*

data class EditorSeed(val templateId: Long = 0, val target: String? = null, val preset: String = "", val minutes: Int = 0)

@Composable
fun ActiveReturn(record: SessionRecord, onActive: () -> Unit) {
    LoopCard(tint = Mint) {
        Pill(when (record.session.status) {
            SessionStatus.PAUSED -> "Paused session"
            SessionStatus.RECOVERY_REQUIRED -> "Recovery needed"
            else -> "Active session"
        })
        Text(record.session.currentTitle, style = MaterialTheme.typography.titleLarge)
        Text("${shortDuration(record.session.activeMs)} active · ${shortDuration(record.session.targetMs)} planned with extensions", color = Muted)
        Primary("Return to session", onClick = onActive)
    }
}

@Composable
fun FocusScreen(state: SpikeState, actions: AppActions, onEditor: (EditorSeed) -> Unit, onActive: () -> Unit, onHistory: () -> Unit) {
    var delete by rememberSaveable { mutableStateOf<Long?>(null) }
    val active = state.core.current?.takeIf { it.session.isOpen }
    Page("focus-page") {
        item { Heading("What do you intend to do?", "A session intention is different from a long-term goal.", "Plan with flexibility") }
        if (active != null) item { ActiveReturn(active, onActive) }
        item { Primary("+  New intention", state.sessionLoaded && !state.storageFailed) { onEditor(EditorSeed()) } }
        item {
            LoopCard {
                Text("Saved intentions", style = MaterialTheme.typography.titleLarge)
                Pill("User-entered")
                if (!state.sessionLoaded) LoadingData("Loading your plans…")
                else if (state.core.intentions.isEmpty()) Text("No saved intentions yet. Choose a starting point or create your own.", color = Muted)
                state.core.intentions.forEachIndexed { index, plan ->
                    if (index > 0) HorizontalDivider()
                    LinkRow(plan.title, "${plan.plannedMinutes} min · ${plan.targetPackage ?: "no target app"}", icon = {
                        Icon(Icons.Outlined.BookmarkBorder, null, tint = Teal)
                    }) { onEditor(EditorSeed(templateId = plan.id)) }
                    TextButton(enabled = !state.busy && !state.storageFailed, onClick = { delete = plan.id }) { Text("Delete ${plan.title}") }
                }
            }
        }
        item {
            LoopCard {
                Text("Reusable starting points", style = MaterialTheme.typography.titleLarge)
                listOf("Watch a lecture" to 60, "Check messages" to 10, "Read an article" to 20).forEachIndexed { index, (title, minutes) ->
                    if (index > 0) HorizontalDivider()
                    LinkRow(title, "$minutes min · editable", icon = { Icon(Icons.Outlined.BookmarkBorder, null, tint = Teal) }) {
                        onEditor(EditorSeed(preset = title, minutes = minutes))
                    }
                }
            }
        }
        item { Secondary("View session history", onClick = onHistory) }
        item { Note("Usage Access is optional. Saving a plan does not start a session or monitor another app.") }
    }
    if (delete != null) ConfirmDialog("Delete saved intention?", "Past sessions keep their original plan. Only this reusable template will be removed.", "Delete", { delete = null }) {
        actions.deletePlan(delete!!) { delete = null }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntentionEditor(state: SpikeState, seed: EditorSeed, actions: AppActions, onClose: () -> Unit, onStarted: (Long) -> Unit, onPermission: () -> Unit, onActive: () -> Unit) {
    val existing = state.core.intentions.find { it.id == seed.templateId }
    if (seed.templateId > 0 && !state.sessionLoaded) { Page("editor-page") { item { LoadingData("Loading your intention…") } }; return }
    if (seed.templateId > 0 && existing == null) {
        Page("editor-page") { item { Heading("This saved intention is unavailable"); Secondary("Return to Focus", onClick = onClose) } }; return
    }
    val initialTitle = existing?.title ?: seed.preset
    val initialMinutes = existing?.plannedMinutes ?: seed.minutes.takeIf { it > 0 } ?: state.preferences.defaultMinutes
    val initialTarget = existing?.targetPackage ?: seed.target
    val initialReminder = existing?.reminderPreferred ?: state.preferences.remindersEnabled
    var title by rememberSaveable(seed) { mutableStateOf(initialTitle) }
    var minutes by rememberSaveable(seed) { mutableStateOf(initialMinutes.toString()) }
    var target by rememberSaveable(seed) { mutableStateOf(initialTarget.orEmpty()) }
    var reminder by rememberSaveable(seed) { mutableStateOf(initialReminder) }
    var saveTemplate by rememberSaveable(seed) { mutableStateOf(existing != null) }
    var appPicker by remember { mutableStateOf(false) }
    var manualPackage by rememberSaveable { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val dirty = title != initialTitle || minutes != initialMinutes.toString() || target != initialTarget.orEmpty() || reminder != initialReminder
    BackHandler(dirty) { discard = true }
    val value = Intention(existing?.id ?: 0, title, minutes.toIntOrNull() ?: 0, target, reminder)
    val valid = runCatching { IntentionRules.validate(value) }.isSuccess
    val enabled = !state.busy && !state.storageFailed && state.sessionLoaded
    Page("editor-page") {
        item { Heading(if (existing == null) "Set a clear intention" else "Make this plan yours", "Choose a plan before you begin. You remain in control.", "New intention") }
        item {
            LoopCard {
                Text("Required", style = MaterialTheme.typography.labelLarge, color = Teal)
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("Intention title") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    supportingText = { Text("What would you like to do?") })
                OutlinedTextField(minutes, { minutes = it.take(4) }, label = { Text("Planned minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = (minutes.toIntOrNull() ?: 0) !in 1..1440,
                    supportingText = { Text("Choose 1–1440 minutes. Extensions keep this original plan.") })
            }
        }
        item {
            LoopCard {
                Text("Optional", style = MaterialTheme.typography.labelLarge, color = Teal)
                LinkRow("Target application", if (target.isBlank()) "None selected" else state.usage?.identities?.get(target)?.name ?: target) { appPicker = true }
                if (target.isNotBlank()) TextButton({ target = "" }) { Text("Remove target app") }
                ToggleRow("Reminder at planned time", "Optional and dismissible; delivery may be delayed.", reminder) { reminder = it }
                if (reminder && !state.notificationsAllowed) {
                    Note("Notifications are off or not yet allowed. This session can still run; allow notifications if you want a reminder.", warning = true)
                    Secondary("Review notification permission", onClick = onPermission)
                }
                HorizontalDivider()
                ToggleRow(if (existing == null) "Save as template" else "Update saved template", "Reuse this intention later.", saveTemplate) { saveTemplate = it }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Study", "Messages", "Social").forEach { starter -> AssistChip({ title = starter }, { Text(starter) }) }
            }
        }
        item { Note("An intention is user-recorded. It does not prove why an app was opened or whether a task was completed.") }
        if (state.session?.isOpen == true) item {
            LoopCard(tint = Mint) {
                Text("A session is already open. You can save this plan for later.")
                Primary("Return to active session", onClick = onActive)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Primary("Start session", enabled && valid && state.session?.isOpen != true) { actions.startIntention(value, saveTemplate, onStarted) }
                Secondary(if (existing == null) "Save intention for later" else "Save changes", enabled && valid) { actions.savePlan(value, onClose) }
                TextButton({ if (dirty) discard = true else onClose() }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
            }
        }
    }
    if (discard) ConfirmDialog("Leave this draft?", "Your saved intention and any active session will stay unchanged.", "Discard draft", { discard = false }, onClose)
    if (appPicker) AlertDialog(onDismissRequest = { appPicker = false }, title = { Text("Choose a target app") }, text = {
        Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Apps with available usage records. Choosing an app is optional.", color = Muted)
            val snapshot = state.usage
            val rows = (snapshot?.result as? UsageResult.Records)?.rows.orEmpty()
            rows.forEach { row ->
                LinkRow(snapshot?.identities?.get(row.packageName)?.name ?: row.packageName) { target = row.packageName; appPicker = false }
            }
            if (rows.isEmpty()) Text("No app records are available. Continue without a target, or enter a package identifier if you know it.")
            TextButton({ manualPackage = !manualPackage }) { Text("Enter package identifier") }
            if (manualPackage) OutlinedTextField(target, { target = it.take(255) }, label = { Text("Package, e.g. org.example.reader") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }, confirmButton = { TextButton({ appPicker = false }) { Text("Done") } }, dismissButton = { TextButton({ target = ""; appPicker = false }) { Text("No target") } })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActiveSessionScreen(state: SpikeState, actions: AppActions, onEnded: (Long, Boolean) -> Unit, onFocus: () -> Unit) {
    val record = state.core.current
    if (record == null || !record.session.isOpen) {
        Page("active-page") { item { Heading("No open session", "Your saved history is available in Focus."); Primary("Go to Focus", onClick = onFocus) } }; return
    }
    val session = record.session
    var checkpoint by rememberSaveable(record.id) { mutableStateOf(false) }
    var lastPrompt by rememberSaveable(record.id) { mutableStateOf(-1L) }
    var extend by rememberSaveable(record.id) { mutableStateOf(false) }
    var revise by rememberSaveable(record.id) { mutableStateOf(false) }
    var cancel by remember { mutableStateOf(false) }
    val enabled = !state.busy && !state.storageFailed
    val passed = session.activeMs >= session.targetMs
    LaunchedEffect(passed, session.targetMs, session.status) {
        if (passed && session.status == SessionStatus.RUNNING && lastPrompt != session.targetMs) { lastPrompt = session.targetMs; checkpoint = true }
    }
    fun finish() { actions.sessionAction(EventKind.FINISH) { onEnded(it, false) } }
    Page("active-page") {
        item { Heading(session.currentTitle, "App-owned session · original plan ${shortDuration(session.originalPlanMs)}", "Your intention") }
        item {
            LoopCard {
                Pill(when (session.status) { SessionStatus.PAUSED -> "Paused"; SessionStatus.RECOVERY_REQUIRED -> "Recovery needed"; else -> "Active session" })
                Box(Modifier.fillMaxWidth().heightIn(min = 216.dp).padding(12.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize()) {
                        val side = minOf(size.width, size.height) - 16.dp.toPx()
                        val offset = Offset((size.width - side) / 2, (size.height - side) / 2)
                        drawArc(Border, -90f, 360f, false, offset, Size(side, side), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                        val fraction = (session.activeMs.toDouble() / session.targetMs.coerceAtLeast(1)).coerceIn(0.0, 1.0).toFloat()
                        drawArc(Teal, -90f, 360f * fraction, false, offset, Size(side, side), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(timerDuration(session.activeMs), style = MaterialTheme.typography.headlineLarge)
                        Text(if (session.hasUnknownGap) "known active time" else "active time", color = Muted)
                        Text("${timerDuration((session.targetMs - session.activeMs).coerceAtLeast(0))} remaining", style = MaterialTheme.typography.bodySmall, color = Teal)
                    }
                }
                Text("Elapsed including pauses: ${timerDuration(session.elapsedMs)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (session.hasUnknownGap) Note("Timing is incomplete after recovery. Only known intervals are shown; an unobserved gap was not reconstructed.", warning = true)
                if (passed) Text("Your planned time has passed. You can finish, extend, or keep going.", color = Teal)
                if (session.status == SessionStatus.RUNNING) Secondary("Pause", enabled) { actions.sessionAction(EventKind.PAUSE) }
                else Secondary(if (session.status == SessionStatus.RECOVERY_REQUIRED) "Resume from now" else "Resume", enabled) { actions.sessionAction(EventKind.RESUME) }
                Secondary("Extend", enabled && session.status != SessionStatus.RECOVERY_REQUIRED) { extend = true }
                Primary("Finish session", enabled, onClick = ::finish)
            }
        }
        item { Secondary("Voluntary checkpoint", enabled) { checkpoint = true } }
        item { Note("You can leave FocusLoop and return without losing this session. Session time is separate from device usage and your reported outcome.") }
        item { TextButton({ cancel = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) { Text("Cancel session") } }
    }
    if (checkpoint) ModalBottomSheet(onDismissRequest = { checkpoint = false }) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Heading("Voluntary checkpoint", "How would you like to proceed?")
            Primary("Finish", enabled, onClick = ::finish)
            Secondary("Extend", enabled && session.status != SessionStatus.RECOVERY_REQUIRED) { checkpoint = false; extend = true }
            Secondary("Revise intention", enabled) { checkpoint = false; revise = true }
            Secondary("Dismiss and keep going") { checkpoint = false }
            Text("You can always dismiss this check-in. No app is blocked.", style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (extend) {
        var extra by rememberSaveable { mutableStateOf("5") }
        AlertDialog(onDismissRequest = { extend = false }, title = { Text("Add time with intention") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your original ${shortDuration(session.originalPlanMs)} plan stays in history. This extension is a new event.")
                OutlinedTextField(extra, { extra = it.take(4) }, label = { Text("Extra minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(5, 10, 15, 30).forEach { AssistChip({ extra = it.toString() }, { Text("+$it") }) } }
            }
        }, confirmButton = { TextButton(enabled = enabled && (extra.toIntOrNull() ?: 0) in 1..1440, onClick = {
            actions.sessionAction(EventKind.EXTEND, extra.toInt()) { extend = false }
        }) { Text("Add time") } }, dismissButton = { TextButton({ extend = false }) { Text("Cancel") } })
    }
    if (revise) {
        var text by rememberSaveable { mutableStateOf(session.currentTitle) }
        AlertDialog(onDismissRequest = { revise = false }, title = { Text("Revise your intention") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The original intention and planned duration stay in history. Your revised wording is recorded as a new event.")
                OutlinedTextField(text, { text = it.take(120) }, label = { Text("Revised intention") }, singleLine = true)
            }
        }, confirmButton = { TextButton(enabled = enabled && text.isNotBlank(), onClick = { actions.reviseIntention(text) { revise = false } }) { Text("Save revision") } },
            dismissButton = { TextButton({ revise = false }) { Text("Cancel") } })
    }
    if (cancel) ConfirmDialog("Cancel this session?", "The session will stay in history as canceled. No task outcome will be inferred.", "Cancel session", { cancel = false }) {
        actions.sessionAction(EventKind.CANCEL) { onEnded(it, true) }
    }
}

@Composable
fun ReflectionScreen(record: SessionRecord?, state: SpikeState, actions: AppActions, onDone: () -> Unit) {
    var choice by rememberSaveable(record?.id) { mutableStateOf(record?.reflection?.outcome?.name) }
    Page("reflection-page") {
        item { Heading("How did it go?", "Optional reflection. There is no right or wrong answer.", "Your perspective") }
        if (record == null) item { LoopCard { Text("Loading this session, or it is no longer available."); Secondary("Return", onClick = onDone) } }
        else if (record.session.status != SessionStatus.FINISHED) item { Note("This session has not been finished. No reflection is required."); Secondary("View summary", onClick = onDone) }
        else {
            item { LoopCard { Text(record.session.currentTitle, style = MaterialTheme.typography.titleMedium); Text("Finishing a session and reporting an outcome are separate choices.", color = Muted) } }
            items(ReportedOutcome.entries, key = { it.name }) { outcome ->
                Surface(shape = MaterialTheme.shapes.medium, color = if (choice == outcome.name) Mint else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (choice == outcome.name) Teal else Border)) {
                    Row(Modifier.fillMaxWidth().selectable(choice == outcome.name, role = Role.RadioButton, onClick = { choice = outcome.name }).padding(12.dp).heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RadioButton(choice == outcome.name, null)
                        Text(outcome.label, modifier = Modifier.weight(1f))
                    }
                }
            }
            item { Primary("Save and continue", choice != null && !state.busy && !state.storageFailed) { actions.saveReflection(record.id, ReportedOutcome.valueOf(choice!!), onDone) } }
            item { Secondary("Skip reflection", !state.busy && !state.storageFailed) { actions.saveReflection(record.id, ReportedOutcome.SKIPPED, onDone) } }
            item { Note("Skipped or unanswered reflection is not counted as success or failure.") }
        }
    }
}

@Composable
fun SummaryScreen(record: SessionRecord?, onHome: () -> Unit, onHistory: () -> Unit, onReflect: () -> Unit) {
    Page("summary-page") {
        item { Heading("Your session, in perspective", "Recorded timing and your own reflection stay separate.", "Session summary") }
        if (record == null) item { LoopCard { Text("This record is loading or no longer available."); Secondary("View history", onClick = onHistory) } }
        else {
            item {
                LoopCard {
                    Text(record.session.currentTitle, style = MaterialTheme.typography.titleLarge)
                    Pill(if (record.session.status == SessionStatus.CANCELLED) "Canceled" else if (record.session.isOpen) "Still open" else "Finished")
                    SummaryLine("Original intention", record.session.title)
                    SummaryLine("Original plan", shortDuration(record.session.originalPlanMs))
                    SummaryLine("Extensions", shortDuration(record.session.targetMs - record.session.originalPlanMs))
                    SummaryLine("Active session time", timerDuration(record.session.activeMs))
                    SummaryLine("Elapsed including pauses", timerDuration(record.session.elapsedMs))
                    SummaryLine("Target application", record.targetPackage ?: "None")
                    if (record.session.hasUnknownGap) Note("Timing is incomplete. An unknown recovery gap is excluded.", warning = true)
                }
            }
            item {
                LoopCard(tint = Mint) {
                    Text("Your reflection", style = MaterialTheme.typography.titleMedium)
                    Text(when (record.reflection?.outcome) { null -> "Not answered"; ReportedOutcome.SKIPPED -> "Skipped · no outcome reported"; else -> record.reflection.outcome.label })
                    Text("Self-reported, never inferred from app usage or session duration.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (record.session.status == SessionStatus.FINISHED) Secondary(if (record.reflection == null) "Add an optional reflection" else "Edit reflection", onClick = onReflect)
                }
            }
            item {
                LoopCard {
                    Text("Plan and event history", style = MaterialTheme.typography.titleMedium)
                    record.session.events.forEach { event ->
                        Text(when (event.kind) {
                            EventKind.START -> "Started"
                            EventKind.PAUSE -> "Paused"
                            EventKind.RESUME -> "Resumed"
                            EventKind.EXTEND -> "Extended +${shortDuration(event.addedMs)}"
                            EventKind.REVISE -> "Revised intention: ${event.note.orEmpty()}"
                            EventKind.RECOVER -> "Recovery gap recorded"
                            EventKind.FINISH -> "Finished"
                            EventKind.CANCEL -> "Canceled"
                        }, style = MaterialTheme.typography.titleSmall)
                        Text(localDateTime(event.wallMs), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
        }
        item { Primary("Back to Home", onClick = onHome) }
        item { Secondary("View Focus history", onClick = onHistory) }
    }
}

@Composable
fun HistoryScreen(state: SpikeState, actions: AppActions, onDetail: (Long) -> Unit) {
    Page("history-page") {
        item { Heading("Your recorded sessions", "${state.core.history.size} of ${state.core.historyTotal} ended sessions. Your history belongs to you.", "Focus history") }
        if (!state.sessionLoaded) item { LoadingData("Loading history…") }
        else if (state.core.history.isEmpty()) item { LoopCard { Text("No ended sessions yet", style = MaterialTheme.typography.titleMedium); Text("Your sessions will appear here when you finish or cancel them.", color = Muted) } }
        items(state.core.history, key = { it.id }) { record ->
            LoopCard {
                LinkRow(record.session.currentTitle, "${if (record.session.status == SessionStatus.CANCELLED) "Canceled" else "Finished"} · ${shortDuration(record.session.activeMs)} active") { onDetail(record.id) }
                Text("Reflection: ${when (record.reflection?.outcome) { null -> "not answered"; ReportedOutcome.SKIPPED -> "skipped"; else -> record.reflection.outcome.label }}", style = MaterialTheme.typography.bodySmall, color = Muted)
                if (record.session.hasUnknownGap) Pill("Timing incomplete")
            }
        }
        if (state.core.history.size < state.core.historyTotal) item { Secondary("Load more sessions", !state.busy) { actions.loadMoreHistory() } }
    }
}

@Composable
fun SummaryLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(description, style = MaterialTheme.typography.bodySmall, color = Muted) }
        Switch(checked, null)
    }
}

@Composable
fun ConfirmDialog(title: String, description: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(description) },
        confirmButton = { TextButton(onConfirm) { Text(confirm) } }, dismissButton = { TextButton(onDismiss) { Text("Keep") } })
}
