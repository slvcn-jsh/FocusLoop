package dev.focusloop.spike.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.focusloop.spike.SpikeState
import dev.focusloop.spike.domain.*
import dev.focusloop.spike.platform.UsageSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: SpikeState, onUsage: () -> Unit, onEditor: (String) -> Unit, onActive: () -> Unit,
    onApp: (String) -> Unit, onPermission: () -> Unit) {
    val active = state.core.current?.takeIf { it.session.isOpen }
    val snapshot = state.todayUsage
    Page("home-page") {
        item { Heading("Today, at a glance", "A little awareness. A plan that stays yours.", "Your overview") }
        if (active != null) item { ActiveReturn(active, onActive) }
        item {
            AdaptivePanels(left = {
                LoopCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Today's app usage", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onUsage) { Text("Details →") }
                    }
                    val result = snapshot?.result
                    when {
                        snapshot == null -> LoadingData("Loading today's available usage…")
                        result is UsageResult.Records -> {
                            Text("App foreground buckets", color = Muted, style = MaterialTheme.typography.bodySmall)
                            Text(shortDuration(result.rows.sumOf { it.foregroundMs }), style = MaterialTheme.typography.headlineLarge)
                            Pill("Android estimate")
                            Text("Coverage unknown · buckets can span date boundaries", color = Muted, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("Most-used app", style = MaterialTheme.typography.bodySmall, color = Muted)
                                    val row = result.rows.firstOrNull()
                                    Text(row?.let { shortDuration(it.foregroundMs) } ?: "Unavailable", style = MaterialTheme.typography.titleLarge)
                                    row?.let { Text(snapshot.identities[it.packageName]?.name ?: it.packageName, style = MaterialTheme.typography.bodySmall, color = Muted) }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Your saved plans", style = MaterialTheme.typography.bodySmall, color = Muted)
                                    Text(if (state.sessionLoaded) state.core.intentions.size.toString() else "—", style = MaterialTheme.typography.titleLarge)
                                    Text("User-entered", style = MaterialTheme.typography.bodySmall, color = Muted)
                                }
                            }
                        }
                        else -> UsageState(snapshot, onPermission, onUsage)
                    }
                }
            }, right = {
                if (active == null) LoopCard(tint = Mint) {
                    Heading("Start with an intention", "Choose what you want to do. Your plan stays editable.", "Your next step")
                    Primary("+  New intention", state.sessionLoaded && !state.storageFailed) { onEditor("") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Study", "Messages", "Social").forEach { title -> AssistChip(onClick = { onEditor(title) }, label = { Text(title) }) }
                    }
                }
                LoopCard {
                    Text("Most-used applications", style = MaterialTheme.typography.titleMedium)
                    val records = snapshot?.result as? UsageResult.Records
                    if (records == null) Text("Available Android records will appear here when you choose to allow Usage Access.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    else records.rows.take(3).forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider()
                        ApplicationRow(row.packageName, snapshot.identities[row.packageName], row.foregroundMs) { onApp(row.packageName) }
                    }
                    TextButton(onUsage) { Text("Explore usage →") }
                }
                Note("Usage Access is optional. Your intentions and manual sessions work without it.")
            }, leftBelow = {
                state.recentUsage?.let { recent ->
                    if (recent.result is UsageResult.Records) LoopCard {
                        Text("Usage context", style = MaterialTheme.typography.titleMedium)
                        UsageChart(dailyUsage(recent.buckets, recent.requested, ZoneId.systemDefault()))
                        Text("Summed app foreground time is different from screen-on time, especially in multi-window.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            })
        }
    }
}

@Composable
fun LoadingData(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun UsageState(snapshot: UsageSnapshot, onPermission: () -> Unit, onRefresh: () -> Unit) {
    when (val result = snapshot.result) {
        is UsageResult.Unavailable -> {
            Text(when (snapshot.access) {
                UsageAccess.REVOKED -> "Usage Access was turned off"
                UsageAccess.NOT_GRANTED -> "Your usage is unavailable"
                else -> "Usage data is unavailable"
            }, style = MaterialTheme.typography.titleMedium)
            Text(if (snapshot.access == UsageAccess.NOT_GRANTED || snapshot.access == UsageAccess.REVOKED)
                "Choose whether to allow Android app-usage awareness. Manual sessions are always available."
                else result.reason, color = Muted)
            if (snapshot.access != UsageAccess.GRANTED) Secondary("Review optional Usage Access", onClick = onPermission)
            else Secondary("Try again", onClick = onRefresh)
        }
        UsageResult.Empty -> {
            Text("No records available", style = MaterialTheme.typography.titleMedium)
            Text("Android returned no records for this selection. Coverage is unknown; this does not mean zero usage.", color = Muted)
            Secondary("Refresh", onClick = onRefresh)
        }
        else -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(state: SpikeState, actions: AppActions, onApp: (String) -> Unit, onPermission: () -> Unit) {
    var customDialog by remember { mutableStateOf(false) }
    var coverage by remember { mutableStateOf(false) }
    val snapshot = state.usage
    val records = snapshot?.result as? UsageResult.Records
    Page("usage-page") {
        item { Heading("Explore your patterns", "Device observations, with data quality in view.", "Usage awareness") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                UsagePeriod.entries.forEach { period ->
                    FilterChip(selected = state.period == period, onClick = {
                        if (period == UsagePeriod.CUSTOM) customDialog = true else actions.refreshUsage(period)
                    }, label = { Text(period.label) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        if (state.period == UsagePeriod.CUSTOM && state.customDates != null) item {
            Text("${state.customDates.start} – ${state.customDates.endInclusive}", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            LoopCard {
                if (snapshot == null) LoadingData("Loading available Android records…")
                else if (records == null) UsageState(snapshot, onPermission) { actions.refreshUsage(state.period, state.customDates) }
                else {
                    Text("App foreground time", style = MaterialTheme.typography.titleMedium)
                    Pill("Android bucket estimate")
                    Text(shortDuration(records.rows.sumOf { it.foregroundMs }), style = MaterialTheme.typography.headlineLarge)
                    Text("${state.period.label} · returned Android buckets", color = Muted)
                    Text("Summed app durations, not device screen-on time. Coverage is unknown.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    TextButton({ coverage = true }) { Text("Understand data coverage →") }
                }
            }
        }
        if (records != null) {
            item {
                LoopCard {
                    Text("Daily trend", style = MaterialTheme.typography.titleMedium)
                    UsageChart(dailyUsage(snapshot.buckets, snapshot.requested, ZoneId.systemDefault()))
                }
            }
            item { Text("Application breakdown", style = MaterialTheme.typography.titleLarge) }
            items(records.rows, key = { it.packageName }) { row ->
                LoopCard { ApplicationRow(row.packageName, snapshot.identities[row.packageName], row.foregroundMs) { onApp(row.packageName) } }
            }
            item { Secondary("Refresh usage") { actions.refreshUsage(state.period, state.customDates) } }
        }
        if (state.period in setOf(UsagePeriod.TWO_WEEKS, UsagePeriod.FOUR_WEEKS, UsagePeriod.CUSTOM)) item {
            Note("Longer history is provisional. Android retention and bucket boundaries can differ from the dates requested; exact totals are not established.", warning = true)
        }
    }
    if (customDialog) CustomRangeDialog(state.customDates, onDismiss = { customDialog = false }) { dates ->
        actions.refreshUsage(UsagePeriod.CUSTOM, dates); customDialog = false
    }
    if (coverage && snapshot != null) CoverageSheet(snapshot) { coverage = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(initial: CustomDates?, onDismiss: () -> Unit, onChoose: (CustomDates) -> Unit) {
    val picker = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.start?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        initialSelectedEndDateMillis = initial?.endInclusive?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = (LocalDate.now().year - 5)..LocalDate.now().year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now())
        })
    var error by remember { mutableStateOf<String?>(null) }
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null, onClick = {
            val dates = CustomDates(Instant.ofEpochMilli(picker.selectedStartDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate(),
                Instant.ofEpochMilli(picker.selectedEndDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate())
            try { usageRange(UsagePeriod.CUSTOM, Instant.now(), ZoneId.systemDefault(), dates); onChoose(dates) }
            catch (e: IllegalArgumentException) { error = e.message }
        }) { Text("Apply range") }
    }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } }) {
        Column {
            error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            DateRangePicker(state = picker, modifier = Modifier.weight(1f, fill = false), title = { Text("Choose up to 366 days", Modifier.padding(16.dp)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverageSheet(snapshot: UsageSnapshot, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Heading("About this usage data")
            Text("Source: Android UsageStatsManager. App foreground durations are observations, not evidence of what you intended or achieved.")
            Text("Requested: ${localDateTime(snapshot.requested.startMs)} to ${localDateTime(snapshot.requested.endMs)} (end exclusive).")
            (snapshot.result as? UsageResult.Records)?.let {
                Text("Returned bounds: ${localDateTime(it.returnedRange.startMs)} to ${localDateTime(it.returnedRange.endMs)}.")
                Text("Bounds exceed request: ${if (it.exceedsRequest) "yes" else "no"}. Bounds alone do not establish continuous coverage.")
            }
            Note("Daily buckets may extend beyond the chosen period. Missing days are not zeroes. Multi-window can make app durations overlap. Charts and totals may cover different bucket boundaries.")
            Primary("Got it", onClick = onDismiss)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AppDetailScreen(state: SpikeState, packageName: String, onCreate: () -> Unit, onPermission: () -> Unit, onRefresh: () -> Unit) {
    val snapshot = state.usage
    val identity = snapshot?.identities?.get(packageName)
    val row = (snapshot?.result as? UsageResult.Records)?.rows?.find { it.packageName == packageName }
    Page("app-detail-page") {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppAvatar(packageName, identity)
                Column(Modifier.weight(1f)) {
                    Text(identity?.name ?: packageName, style = MaterialTheme.typography.headlineMedium)
                    Text(packageName, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
        item { Text("Selected period: ${state.period.label}", color = Muted) }
        item {
            LoopCard {
                Text("Observed device usage", style = MaterialTheme.typography.titleMedium)
                when {
                    snapshot == null -> LoadingData("Loading app records…")
                    snapshot.result !is UsageResult.Records -> UsageState(snapshot, onPermission, onRefresh)
                    row == null -> Text("No record for this application in the selected period. This is unavailable data, not a measured zero.")
                    else -> {
                        Text(shortDuration(row.foregroundMs), style = MaterialTheme.typography.headlineLarge)
                        Pill("Android bucket estimate")
                        Text("Foreground time in returned buckets · coverage unknown", color = Muted)
                    }
                }
            }
        }
        if (snapshot?.result is UsageResult.Records) item {
            LoopCard {
                Text("Daily history", style = MaterialTheme.typography.titleMedium)
                UsageChart(dailyUsage(snapshot.buckets, snapshot.requested, ZoneId.systemDefault(), packageName))
                Text("Available buckets are shown; missing history remains unknown.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { Note("Observed usage shows time in an app. It cannot establish your intention or task completion.") }
        item {
            LoopCard(tint = Mint) {
                Text("Choose an intention for this app", style = MaterialTheme.typography.titleMedium)
                Text("You decide what this application is for. A target app is optional.", color = Muted)
                Primary("Create intention", onClick = onCreate)
            }
        }
    }
}
