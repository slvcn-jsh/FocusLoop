package dev.focusloop.spike.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.focusloop.spike.SpikeState
import dev.focusloop.spike.SpikeViewModel
import dev.focusloop.spike.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val palette = lightColorScheme(primary = Color(0xFF0F7C88), onPrimary = Color.White,
    background = Color(0xFFF7FAFB), surface = Color.White, onSurface = Color(0xFF16343A),
    onBackground = Color(0xFF16343A), onSurfaceVariant = Color(0xFF5E7074))

@Composable
fun SpikeScreen(model: SpikeViewModel, openUsageSettings: () -> Unit, requestNotifications: () -> Unit,
    openNotificationSettings: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = palette) {
        Scaffold { insets ->
            Column(Modifier.fillMaxSize().padding(insets).imePadding().padding(16.dp)) {
                Text("FocusLoop", style = MaterialTheme.typography.headlineLarge)
                Text("Phase 2 core prototype", style = MaterialTheme.typography.titleMedium)
                Text("Local diagnostics only. No device usage is stored or uploaded.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (maxWidth >= 600.dp && LocalDensity.current.fontScale <= 1.3f) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                SessionPanel(state, model)
                                HistoryPanel(state, model)
                                ReminderPanel(state, model, requestNotifications, openNotificationSettings)
                            }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                UsagePanel(state, model, openUsageSettings)
                            }
                        }
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SessionPanel(state, model)
                            HistoryPanel(state, model)
                            UsagePanel(state, model, openUsageSettings)
                            ReminderPanel(state, model, requestNotifications, openNotificationSettings)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), border = BorderStroke(1.dp, Color(0xFFD6E3E5)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun SessionPanel(state: SpikeState, model: SpikeViewModel) {
    var title by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("25") }
    var target by rememberSaveable { mutableStateOf("") }
    var reminder by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf(0L) }
    var deleteIntentionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val canMutate = !state.busy && !state.storageFailed
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    DiagnosticCard("App-owned session timing") {
        Text("Works without Usage Access. These durations do not measure activity inside another app.")
        state.sessionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.storageFailed) Text("Showing the last displayed record. Reload or retry clearing to establish current storage state.")
        if (!state.sessionLoaded) Text("Loading saved session...")
        val session = state.session
        if (session == null || !session.isOpen) {
            OutlinedTextField(title, { title = it.take(120) }, label = { Text("Session title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(minutes, { minutes = it.take(4) }, label = { Text("Planned minutes (1-1440)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(target, { target = it.take(255) }, label = { Text("Target package (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(Modifier.fillMaxWidth().toggleable(value = reminder, role = Role.Checkbox, onValueChange = { reminder = it })) {
                Checkbox(reminder, null)
                Text("Save reminder preference. Session reminders are not scheduled in this prototype.", modifier = Modifier.weight(1f))
            }
            Button({ model.start(title, minutes, target, reminder) }, enabled = state.sessionLoaded && canMutate && title.isNotBlank() && (minutes.toIntOrNull() ?: 0) in 1..1440,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Start manual session") }
            Action(if (editingId == 0L) "Save intention without starting" else "Update saved intention", state.sessionLoaded && canMutate) {
                model.saveIntention(Intention(editingId, title, minutes.toIntOrNull() ?: 0, target, reminder))
            }
            if (editingId != 0L) Action("Cancel editing saved intention") { editingId = 0; title = ""; minutes = "25"; target = ""; reminder = false }
            Text("Past sessions stay in local history when you start another. Device usage is separate.")
        }
        session?.let { s ->
            HorizontalDivider()
            Text(s.title, style = MaterialTheme.typography.titleMedium)
            Text("State: ${s.status.name.lowercase().replace('_', ' ')}${if (state.restored) " (restored record)" else ""}")
            Text("Original plan: ${duration(s.originalPlanMs)} | Extended target: ${duration(s.targetMs)}")
            Text("Active: ${duration(s.activeMs)}", style = MaterialTheme.typography.headlineSmall)
            Text("Elapsed including pauses: ${duration(s.elapsedMs)}")
            Text("Remaining active time to target: ${duration((s.targetMs - s.activeMs).coerceAtLeast(0))}")
            if (s.activeMs >= s.targetMs) Text("Your planned time has passed. You can finish or continue.")
            if (s.hasUnknownGap) Text("Timing is incomplete after a reboot or clock identity change. Only known intervals are shown; the gap is not reconstructed.")
            if (s.isOpen) {
                if (s.status == SessionStatus.RUNNING) Action("Pause", canMutate) { model.act(EventKind.PAUSE) }
                else Action(if (s.status == SessionStatus.RECOVERY_REQUIRED) "Resume from now (keep unknown gap)" else "Resume", canMutate) { model.act(EventKind.RESUME) }
                if (s.status != SessionStatus.RECOVERY_REQUIRED) Action("Extend by 5 minutes", canMutate) { model.act(EventKind.EXTEND) }
                Action("Finish session", canMutate) { model.act(EventKind.FINISH) }
                Action("Cancel session", canMutate) { model.act(EventKind.CANCEL) }
            }
            state.core.current?.let { record ->
                Text("Target: ${record.targetPackage ?: "none"}. Saving a target does not monitor or block it.")
                if (s.status == SessionStatus.FINISHED) ReflectionPanel(record, !canMutate, model)
                else Text("No outcome is inferred from session timing or device usage.")
            }
            Text("Persisted event history", style = MaterialTheme.typography.titleSmall)
            s.events.forEach { Text("${it.kind.name.lowercase()}: ${date(it.wallMs)}${if (it.addedMs > 0) " (+${duration(it.addedMs)})" else ""}", style = MaterialTheme.typography.bodySmall) }
        }
        Action("Reload saved record", !state.busy) { model.reloadSession() }
        Action("Clear local session history...", !state.busy) { confirmClear = true }
        HorizontalDivider()
        Text("Saved intentions", style = MaterialTheme.typography.titleMedium)
        if (state.core.intentions.isEmpty()) Text("No saved intentions. Saving a plan is optional.")
        state.core.intentions.forEach { intention ->
            Text("${intention.title} · ${intention.plannedMinutes} min")
            Action("Use or edit: ${intention.title}", state.session?.isOpen != true && canMutate) {
                editingId = intention.id; title = intention.title; minutes = intention.plannedMinutes.toString()
                target = intention.targetPackage.orEmpty(); reminder = intention.reminderPreferred
            }
            Action("Delete saved intention: ${intention.title}", canMutate) { deleteIntentionId = intention.id }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Clear local session history?") },
        text = { Text("This permanently removes all sessions, events and reflections, including any active session and the preserved phase 1 record. Saved intentions and Android device usage are unaffected.") },
        confirmButton = { TextButton({ confirmClear = false; model.clear() }) { Text("Clear record") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Keep record") } })
    if (deleteIntentionId != null) AlertDialog(onDismissRequest = { deleteIntentionId = null },
        title = { Text("Delete saved intention?") }, text = { Text("Past sessions keep their original plan. This deletes only the reusable template.") },
        confirmButton = { TextButton({ model.deleteIntention(requireNotNull(deleteIntentionId)); deleteIntentionId = null }) { Text("Delete") } },
        dismissButton = { TextButton({ deleteIntentionId = null }) { Text("Keep") } })
}

@Composable
private fun ReflectionPanel(record: SessionRecord, busy: Boolean, model: SpikeViewModel) {
    var choice by remember(record.id, record.reflection) { mutableStateOf(record.reflection?.outcome) }
    Text("Optional self-reported reflection", style = MaterialTheme.typography.titleMedium)
    Text(when (record.reflection?.outcome) {
        null -> "Not answered. Finishing alone does not report success."
        ReportedOutcome.SKIPPED -> "Reflection skipped; this is not failure or success."
        else -> "You reported: ${record.reflection.outcome.label}"
    })
    ReportedOutcome.entries.filter { it != ReportedOutcome.SKIPPED }.forEach { outcome ->
        FilterChip(selected = choice == outcome, onClick = { choice = outcome }, label = { Text(outcome.label) }, enabled = !busy,
            modifier = Modifier.heightIn(min = 48.dp))
    }
    Action("Save reflection", !busy && choice != null && choice != ReportedOutcome.SKIPPED) { model.reflect(record.id, requireNotNull(choice)) }
    Action("Skip reflection", !busy) { model.reflect(record.id, ReportedOutcome.SKIPPED) }
}

@Composable
private fun HistoryPanel(state: SpikeState, model: SpikeViewModel) {
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    DiagnosticCard("Local session history") {
        Text("Latest 100 ended sessions shown; older records remain stored. Self-reported outcomes are separate from device observations.")
        if (state.core.history.isEmpty()) Text("No ended sessions yet.")
        state.core.history.forEach { record ->
            HorizontalDivider()
            Text(record.session.title, style = MaterialTheme.typography.titleMedium)
            Text("${record.session.status.name.lowercase()} · active ${duration(record.session.activeMs)}")
            Text("Outcome: ${record.reflection?.outcome?.label ?: "not answered"}")
            Action(if (detailId == record.id) "Close session details" else "View session details") { detailId = if (detailId == record.id) null else record.id }
            if (detailId == record.id) {
                Text("Original plan ${duration(record.session.originalPlanMs)} · target ${duration(record.session.targetMs)} · elapsed ${duration(record.session.elapsedMs)}")
                Text("Target: ${record.targetPackage ?: "none"}")
                if (record.session.hasUnknownGap) Text("Timing incomplete: an unknown recovery gap is excluded.")
                record.session.events.forEach { Text("${it.kind.name.lowercase()}: ${date(it.wallMs)}${if (it.addedMs > 0) " (+${duration(it.addedMs)})" else ""}") }
                if (record.session.status == SessionStatus.FINISHED) ReflectionPanel(record, state.busy || state.storageFailed, model)
            }
        }
    }
}

@Composable
private fun UsagePanel(state: SpikeState, model: SpikeViewModel, openUsageSettings: () -> Unit) {
    DiagnosticCard("Observed Android app usage") {
        Text("Optional device data. Foreground app durations are not screen-on time or evidence of your intention.")
        UsagePeriod.entries.forEach { period ->
            FilterChip(selected = state.period == period, onClick = { model.refreshUsage(period) }, label = { Text(period.label) }, modifier = Modifier.heightIn(min = 48.dp))
        }
        val snapshot = state.usage
        if (snapshot == null) Text("Checking access and loading current records...")
        else {
            Text("Usage Access: ${snapshot.access.name.lowercase().replace('_', ' ')}")
            Text("Requested local range: ${date(snapshot.requested.startMs)} to ${date(snapshot.requested.endMs)} (end exclusive)", style = MaterialTheme.typography.bodySmall)
            when (val result = snapshot.result) {
                is UsageResult.Unavailable -> Text(result.reason)
                UsageResult.Empty -> Text("No records returned. Coverage is unknown; this is not a measured zero.")
                is UsageResult.Records -> {
                    Text("Coverage unknown. Android may expand daily bucket boundaries; exact totals for the selected range are not established.")
                    Text("Returned bounds: ${date(result.returnedRange.startMs)} to ${date(result.returnedRange.endMs)}", style = MaterialTheme.typography.bodySmall)
                    Text("Buckets exceed requested bounds: ${if (result.exceedsRequest) "yes" else "no"}. Bounds alone do not prove continuous history.")
                    result.rows.forEach { row ->
                        HorizontalDivider()
                        Text(row.packageName, style = MaterialTheme.typography.titleSmall)
                        Text("Raw Android foreground buckets: ${duration(row.foregroundMs)}")
                    }
                }
            }
        }
        Action("Refresh device usage") { model.refreshUsage() }
        Action("Open Usage Access settings (optional)", onClick = openUsageSettings)
        Text("14/28-day and custom analytics are deferred until device coverage is verified.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReminderPanel(state: SpikeState, model: SpikeViewModel, request: () -> Unit, settings: () -> Unit) {
    DiagnosticCard("Optional reminder probe") {
        Text("Independent 60-second inexact notification test. It does not finish sessions or schedule production checkpoints. Reboot and force-stop can cancel delivery.")
        Text(state.reminder)
        Action("Request notification permission", onClick = request)
        Action("Open notification settings", onClick = settings)
        Action("Schedule 60-second probe") { model.reminder(true) }
        Action("Cancel probe and dismiss notification") { model.reminder(false) }
        Action("Refresh delivery status") { model.reminderStatus() }
    }
}

@Composable
private fun Action(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
}

private fun duration(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
}
private fun date(ms: Long): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))
