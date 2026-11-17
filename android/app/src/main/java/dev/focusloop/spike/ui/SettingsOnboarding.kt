package dev.focusloop.spike.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.focusloop.spike.SpikeState
import dev.focusloop.spike.domain.*

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    Page("welcome-page") {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            LoopCard(tint = Mint) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoopMark(Modifier.size(72.dp)) }
                Text("Make screen time intentional.", style = MaterialTheme.typography.titleLarge)
            }
        }
        item { Heading("Welcome to FocusLoop", "Understand your usage. Choose your intentions. Reflect on what matters to you—without judgment.") }
        item {
            LoopCard {
                Text("Your choices stay yours", style = MaterialTheme.typography.titleMedium)
                Text("No account. No forced blocking. Optional goals, permissions, reminders and reflection.", color = Muted)
            }
        }
        item { Primary("Get started", onClick = onNext) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(state: SpikeState, actions: AppActions, onboarding: Boolean, onDone: () -> Unit, onSkip: () -> Unit = {}) {
    var selected by rememberSaveable { mutableStateOf(state.preferences.goals.toTypedArray()) }
    Page(if (onboarding) "onboarding-goals-page" else "goals-page") {
        item { Heading("What matters to you?", "Choose any goals, or leave this for later. These are different from individual session intentions.", "Personal goals") }
        item {
            LoopCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PERSONAL_GOALS.forEach { goal ->
                        FilterChip(selected = goal in selected, onClick = {
                            selected = if (goal in selected) selected.filterNot { it == goal }.toTypedArray() else selected + goal
                        }, label = { Text(goal) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
        }
        item { Primary(if (onboarding) "Continue" else "Save goals", !state.busy) { actions.savePreferences(state.preferences.copy(goals = selected.toSet()), onDone) } }
        if (onboarding) item { Secondary("Skip for now", onClick = onSkip) }
        item { Note("You can change these choices in Settings. No goal selection restricts access or determines your intentions.") }
    }
}

@Composable
fun PrivacyIntroduction(onNext: () -> Unit) {
    Page("onboarding-privacy-page") {
        item { Heading("Private. Local. Yours.", "You're in control of what you record.", "Privacy") }
        item { LoopCard { Text("No account required", style = MaterialTheme.typography.titleMedium); Text("Intentions, sessions and reflections stay on this device. FocusLoop has no cloud account or remote analytics connection.", color = Muted) } }
        item { LoopCard { Text("Keep your data sources clear", style = MaterialTheme.typography.titleMedium); Text("Android app usage is an observation. Session timing is an app-owned record. Reflections are what you choose to report.", color = Muted) } }
        item { LoopCard { Text("Choose what to share with yourself", style = MaterialTheme.typography.titleMedium); Text("Usage Access, notifications and reflection are optional. You can manage permissions and delete your local records later.", color = Muted) } }
        item { Primary("Continue", onClick = onNext) }
    }
}

@Composable
fun UsagePermissionScreen(state: SpikeState, openSettings: () -> Unit, onRefresh: () -> Unit, onContinue: (() -> Unit)? = null) {
    val access = state.usage?.access
    Page(if (onContinue == null) "usage-access-page" else "onboarding-access-page") {
        item { Heading("Usage Access is optional", "See available Android app-usage records, or continue with manual intentions and sessions.", "Your permission") }
        item {
            LoopCard(tint = Mint) {
                Text("Understand usage on this device", style = MaterialTheme.typography.titleLarge)
                Text("Android Usage Access enables per-app foreground-time estimates. FocusLoop cannot determine why you opened another app.", color = Muted)
                Pill(when (access) { UsageAccess.GRANTED -> "Allowed"; UsageAccess.REVOKED -> "Revoked"; UsageAccess.NOT_GRANTED -> "Not granted"; else -> "Checking or unavailable" })
            }
        }
        item { Note("Device usage is queried into memory and is not uploaded or saved as FocusLoop history. Declining or revoking access leaves manual sessions available.") }
        item { Primary(if (access == UsageAccess.GRANTED) "Manage Usage Access" else "Open Android Usage Access settings", onClick = openSettings) }
        item { Secondary("Check access again", onClick = onRefresh) }
        if (onContinue != null) item { Secondary(if (access == UsageAccess.GRANTED) "Continue to Home" else "Not now — continue manually", onClick = onContinue) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: SpikeState, onNavigate: (String) -> Unit) {
    Page("settings-page") {
        item { Heading("Your choices stay in your control", "Manage goals, permissions and local records.") }
        item {
            LoopCard {
                LinkRow("My goals", "Optional · change what matters to you") { onNavigate("my-goals") }
                if (state.preferences.goals.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.preferences.goals.forEach { Pill(it) }
                }
            }
        }
        item {
            LoopCard {
                Text("Permissions & reminders", style = MaterialTheme.typography.titleMedium)
                LinkRow("Usage Access", when (state.usage?.access) { UsageAccess.GRANTED -> "Allowed · Android app usage"; UsageAccess.REVOKED -> "Revoked · manual mode remains available"; else -> "Optional · manual mode is always available" }) { onNavigate("usage-access") }
                HorizontalDivider()
                LinkRow("Notifications", "Voluntary checkpoints · best effort") { onNavigate("notifications") }
                HorizontalDivider()
                LinkRow("Session defaults", "${state.preferences.defaultMinutes} min · editable before starting") { onNavigate("session-defaults") }
            }
        }
        item {
            LoopCard {
                Text("Privacy & data", style = MaterialTheme.typography.titleMedium)
                Text("Local-first with no account. Your saved plans and recorded sessions stay separate from Android device observations.", color = Muted)
                LinkRow("Data & privacy", "Review and delete local records") { onNavigate("data-privacy") }
            }
        }
        item { LoopCard { LinkRow("About FocusLoop", "Research basis and limitations") { onNavigate("about") } } }
    }
}

@Composable
fun NotificationSettings(state: SpikeState, actions: AppActions, request: () -> Unit, systemSettings: () -> Unit) {
    var sentTest by rememberSaveable { mutableStateOf(false) }
    Page("notifications-page") {
        item { Heading("A gentle checkpoint", "Reminders are optional and can always be dismissed.") }
        item {
            LoopCard {
                Text("Android notifications", style = MaterialTheme.typography.titleMedium)
                Pill(if (state.notificationsAllowed) "Allowed for checkpoints" else "Off or not granted")
                Text("Reminders may arrive late, particularly while the phone is idle or battery restricted. They never block another app or finish a session.", color = Muted)
                if (!state.notificationsAllowed) Primary("Request notification permission", onClick = request)
                Secondary("Manage notifications in Android", onClick = systemSettings)
            }
        }
        item {
            LoopCard {
                ToggleRow("Reminders by default", "Applies to new intentions. Each session can choose its own reminder.", state.preferences.remindersEnabled) {
                    actions.savePreferences(state.preferences.copy(remindersEnabled = it))
                }
                Text("You can turn reminders off in Android at any time. Pause or cancel a session to cancel its pending reminder.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            LoopCard {
                Text("Try a reminder", style = MaterialTheme.typography.titleMedium)
                Text("Request an independent test reminder in about one minute. It may be delayed and will not change a session.", color = Muted)
                Secondary("Send a test reminder") { actions.testReminder(); sentTest = true }
                if (sentTest) Note(state.reminder)
            }
        }
        item { Note("After reboot or force-stop, reminders may be lost. Open FocusLoop to recover your session; an unknown timing gap remains explicit.") }
    }
}

@Composable
fun SessionDefaults(state: SpikeState, actions: AppActions, onDone: () -> Unit) {
    var minutes by rememberSaveable { mutableStateOf(state.preferences.defaultMinutes.toString()) }
    var reminder by rememberSaveable { mutableStateOf(state.preferences.remindersEnabled) }
    Page("defaults-page") {
        item { Heading("A starting point that fits you", "Defaults apply to new plans. Existing sessions keep their original settings.", "Session defaults") }
        item {
            LoopCard {
                OutlinedTextField(minutes, { minutes = it.take(4) }, modifier = Modifier.fillMaxWidth(), label = { Text("Default minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    isError = (minutes.toIntOrNull() ?: 0) !in 1..1440, supportingText = { Text("Choose 1–1440 minutes.") })
                ToggleRow("Reminder by default", "You can change this for each intention.", reminder) { reminder = it }
            }
        }
        item { Primary("Save defaults", !state.busy && (minutes.toIntOrNull() ?: 0) in 1..1440) { actions.savePreferences(state.preferences.copy(defaultMinutes = minutes.toInt(), remindersEnabled = reminder), onDone) } }
    }
}

@Composable
fun DataPrivacyScreen(state: SpikeState, actions: AppActions) {
    var deleteHistory by remember { mutableStateOf(false) }
    var deletePlans by remember { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    Page("privacy-page") {
        item { Heading("Your records, your choice", "Review what FocusLoop keeps and choose what to remove.", "Data & privacy") }
        item { LoopCard { Text("Stored on this device", style = MaterialTheme.typography.titleMedium); Text("Saved intentions, session timing and events, your optional goals, preferences and self-reported reflections. No account or cloud sync.", color = Muted) } }
        item { LoopCard { Text("Device observations stay separate", style = MaterialTheme.typography.titleMedium); Text("Usage Access records come from Android and are kept in memory while needed. FocusLoop does not store that per-app usage as your session history.", color = Muted) } }
        result?.let { message -> item { Note(message) } }
        item {
            LoopCard {
                Text("Delete local session history", style = MaterialTheme.typography.titleMedium)
                Text("Remove all sessions, events and reflections, including any active session and the preserved early prototype record. Saved intentions and Android usage remain.", color = Muted)
                Secondary("Delete session history…", !state.busy) { deleteHistory = true }
            }
        }
        item {
            LoopCard {
                Text("Delete saved intentions", style = MaterialTheme.typography.titleMedium)
                Text("Remove reusable templates. Past sessions keep their original plans, and any current session remains open.", color = Muted)
                Secondary("Delete saved intentions…", !state.busy && !state.storageFailed) { deletePlans = true }
            }
        }
    }
    if (deleteHistory) ConfirmDialog("Delete all session history?", "This permanently removes sessions, events and reflections, including an active session. Saved intentions and Android device usage are unaffected.", "Delete history", { deleteHistory = false }) {
        actions.clearHistory { deleteHistory = false; result = "Local session history was deleted. Saved intentions and Android usage were kept." }
    }
    if (deletePlans) ConfirmDialog("Delete all saved intentions?", "Your session history and active session remain unchanged.", "Delete intentions", { deletePlans = false }) {
        actions.clearPlans { deletePlans = false; result = "Saved intentions were deleted. Session history and any active session were kept." }
    }
}

@Composable
fun AboutScreen(onReviewWelcome: () -> Unit) {
    val uri = LocalUriHandler.current
    Page("about-page") {
        item { Heading("FocusLoop", "Make screen time intentional.", "About") }
        item { LoopCard { Text("Research-informed, choice-led", style = MaterialTheme.typography.titleMedium); Text("Research and firsthand observations informed voluntary reflection and user control. FocusLoop has not established effectiveness in reducing procrastination or improving wellbeing.", color = Muted) } }
        item { LoopCard { Text("Honest data", style = MaterialTheme.typography.titleMedium); Text("Device observations, app-owned session records and self-reported outcomes are different sources. Missing data stays unknown. App use does not prove intention or success.", color = Muted) } }
        item {
            LoopCard {
                Text("Project research references", style = MaterialTheme.typography.titleMedium)
                LinkRow("Wellspent · JMIR", "Source supplied in the project evidence register") { uri.openUri("https://mhealth.jmir.org/2026/1/e56824/") }
                LinkRow("one sec field experiment", "Source supplied in the project evidence register") { uri.openUri("https://pmc.ncbi.nlm.nih.gov/articles/PMC9974409/") }
                LinkRow("MindPhone", "Source supplied in the project evidence register") { uri.openUri("https://doi.org/10.1145/3532106.3533575") }
            }
        }
        item { Secondary("Review welcome and permissions", onClick = onReviewWelcome) }
        item { Text("Version 0.3 · Native Android · local-first", style = MaterialTheme.typography.bodySmall, color = Muted) }
    }
}
