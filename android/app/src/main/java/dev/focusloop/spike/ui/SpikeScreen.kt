package dev.focusloop.spike.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                Text("Phase 1 feasibility experiment", style = MaterialTheme.typography.titleMedium)
                Text("Local diagnostics only. No device usage is stored or uploaded.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (maxWidth >= 720.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                SessionPanel(state, model)
                                ReminderPanel(state, model, requestNotifications, openNotificationSettings)
                            }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                UsagePanel(state, model, openUsageSettings)
                            }
                        }
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SessionPanel(state, model)
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
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    DiagnosticCard("App-owned session timing") {
        Text("Works without Usage Access. These durations do not measure activity inside another app.")
        state.sessionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!state.sessionLoaded) Text("Loading saved session...")
        val session = state.session
        if (session == null || !session.isOpen) {
            OutlinedTextField(title, { title = it.take(120) }, label = { Text("Session title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(minutes, { minutes = it.take(4) }, label = { Text("Planned minutes (1-1440)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button({ model.start(title, minutes) }, enabled = state.sessionLoaded && !state.busy && title.isNotBlank() && (minutes.toIntOrNull() ?: 0) in 1..1440,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Start manual session") }
            Text("This spike retains only the latest session. Starting again replaces an ended experiment record.")
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
                if (s.status == SessionStatus.RUNNING) Action("Pause", !state.busy) { model.act(EventKind.PAUSE) }
                else Action(if (s.status == SessionStatus.RECOVERY_REQUIRED) "Resume from now (keep unknown gap)" else "Resume", !state.busy) { model.act(EventKind.RESUME) }
                if (s.status != SessionStatus.RECOVERY_REQUIRED) Action("Extend by 5 minutes", !state.busy) { model.act(EventKind.EXTEND) }
                Action("Finish session", !state.busy) { model.act(EventKind.FINISH) }
                Action("Cancel session", !state.busy) { model.act(EventKind.CANCEL) }
            }
            Text("Reflection: not collected in this experiment. Finishing does not report task success.")
            Text("Persisted event history", style = MaterialTheme.typography.titleSmall)
            s.events.forEach { Text("${it.kind.name.lowercase()}: ${date(it.wallMs)}${if (it.addedMs > 0) " (+${duration(it.addedMs)})" else ""}", style = MaterialTheme.typography.bodySmall) }
        }
        Action("Reload saved record", !state.busy) { model.reloadSession() }
        Action("Clear experimental session...", !state.busy) { confirmClear = true }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Clear this experiment record?") },
        text = { Text("This permanently removes the locally saved session and its events, including any active session. Android usage data is unaffected.") },
        confirmButton = { TextButton({ confirmClear = false; model.clear() }) { Text("Clear record") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Keep record") } })
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
