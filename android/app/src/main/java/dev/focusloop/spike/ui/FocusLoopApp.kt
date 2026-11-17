package dev.focusloop.spike.ui

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dev.focusloop.spike.SpikeState

data class PlatformActions(val usageSettings: () -> Unit, val notificationPermission: () -> Unit, val notificationSettings: () -> Unit)
private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(Destination("home", "Home", Icons.Outlined.Home), Destination("usage", "Usage", Icons.Outlined.BarChart),
    Destination("focus", "Focus", Icons.Outlined.RadioButtonUnchecked), Destination("settings", "Settings", Icons.Outlined.Tune))
private const val EDITOR_ROUTE = "editor?template={template}&app={app}&preset={preset}&minutes={minutes}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusLoopApp(state: SpikeState, actions: AppActions, platform: PlatformActions, modifier: Modifier = Modifier) {
    FocusLoopTheme {
        if (!state.preferencesLoaded) {
            Surface(modifier.fillMaxSize(), color = CanvasColor) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { LoopMark(); Text("FocusLoop", style = MaterialTheme.typography.titleLarge) }
                    if (state.storageFailed) { Text(state.sessionError ?: "Local records need attention."); Primary("Try loading again") { actions.retry() } }
                    else LoadingData("Opening your local records…")
                }
            }
            return@FocusLoopTheme
        }
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route ?: if (state.preferences.onboarded) "home" else "welcome"
        val onboarding = route in setOf("welcome", "choose-goals", "privacy-intro", "access-intro")
        val root = destinations.any { it.route == route }
        val selected = when {
            route == "home" -> "home"
            route == "usage" || route.startsWith("app/") -> "usage"
            route in setOf("settings", "my-goals", "usage-access", "notifications", "session-defaults", "data-privacy", "about") -> "settings"
            else -> "focus"
        }
        val title = when {
            route == "home" || onboarding -> "FocusLoop"
            root -> destinations.firstOrNull { it.route == route }?.label ?: "FocusLoop"
            route.startsWith("app/") -> "App details"
            route.startsWith("editor?") -> "New intention"
            route == "active" -> "Your session"
            route.startsWith("reflection/") -> "Reflection"
            route.startsWith("summary/") -> "Session summary"
            route == "history" -> "Focus history"
            route == "my-goals" -> "My goals"
            route == "usage-access" -> "Usage Access"
            route == "notifications" -> "Notifications"
            route == "session-defaults" -> "Session defaults"
            route == "data-privacy" -> "Data & privacy"
            else -> "About FocusLoop"
        }
        fun mainTab(target: String) {
            nav.navigate(target) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
        }
        fun editor(seed: EditorSeed) {
            nav.navigate("editor?template=${seed.templateId}&app=${Uri.encode(seed.target.orEmpty())}&preset=${Uri.encode(seed.preset)}&minutes=${seed.minutes}")
        }
        fun doneOnboarding() {
            actions.savePreferences(state.preferences.copy(onboarded = true)) {
                nav.navigate("home") { popUpTo(nav.graph.id); launchSingleTop = true }
            }
        }
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        val density = LocalDensity.current
        val keyboard = WindowInsets.ime.getBottom(density) > 0
        val start = remember { if (state.preferences.onboarded) "home" else "welcome" }
        BoxWithConstraints(modifier.fillMaxSize().testTag("focusloop-app")) {
            val rail = maxWidth >= 840.dp && !onboarding
            Scaffold(modifier = Modifier.fillMaxSize().imePadding(), containerColor = CanvasColor,
                topBar = {
                    Column {
                        TopAppBar(title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (route == "home" || onboarding) LoopMark()
                                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                        }, navigationIcon = {
                            if (!root && route != "welcome") IconButton({ if (dispatcher != null) dispatcher.onBackPressed() else nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                            }
                        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasColor))
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }, bottomBar = {
                    if (!onboarding && !rail && !keyboard) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        destinations.forEach { destination ->
                            NavigationBarItem(selected = selected == destination.route, onClick = { mainTab(destination.route) },
                                icon = { Icon(destination.icon, if (density.fontScale >= 1.5f) destination.label else null) },
                                label = if (density.fontScale < 1.5f) ({ Text(destination.label) }) else null,
                                modifier = Modifier.testTag("nav-${destination.route}"))
                        }
                    }
                }) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (rail) NavigationRail(containerColor = CanvasColor) {
                        destinations.forEach { destination -> NavigationRailItem(selected = selected == destination.route,
                            onClick = { mainTab(destination.route) }, icon = { Icon(destination.icon, destination.label) },
                            label = { Text(destination.label) }, modifier = Modifier.testTag("nav-${destination.route}")) }
                    }
                    Column(Modifier.weight(1f)) {
                        state.sessionError?.let { message ->
                            Surface(color = if (state.storageFailed) MaterialTheme.colorScheme.errorContainer else BlueWash) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(message, style = MaterialTheme.typography.bodyMedium)
                                    if (state.storageFailed) Text("Last shown records may be out of date. Mutations are disabled until loading succeeds.", style = MaterialTheme.typography.bodySmall)
                                    Row { TextButton({ actions.retry() }) { Text("Retry") }; if (!state.storageFailed) TextButton({ actions.dismissError() }) { Text("Dismiss") } }
                                }
                            }
                        }
                        NavHost(navController = nav, startDestination = start, modifier = Modifier.weight(1f)) {
                            composable("welcome") { WelcomeScreen { nav.navigate("choose-goals") } }
                            composable("choose-goals") { GoalsScreen(state, actions, true, { nav.navigate("privacy-intro") }, { nav.navigate("privacy-intro") }) }
                            composable("privacy-intro") { PrivacyIntroduction { nav.navigate("access-intro") } }
                            composable("access-intro") { UsagePermissionScreen(state, platform.usageSettings, { actions.refreshUsage(state.period, state.customDates) }, ::doneOnboarding) }
                            composable("home") { HomeScreen(state, { mainTab("usage") }, { editor(EditorSeed(preset = it)) }, { nav.navigate("active") },
                                { nav.navigate("app/${Uri.encode(it)}") }, { nav.navigate("usage-access") }) }
                            composable("usage") { UsageScreen(state, actions, { nav.navigate("app/${Uri.encode(it)}") }, { nav.navigate("usage-access") }) }
                            composable("app/{package}", arguments = listOf(navArgument("package") { type = NavType.StringType })) { back ->
                                val pkg = back.arguments?.getString("package").orEmpty()
                                AppDetailScreen(state, pkg, { editor(EditorSeed(target = pkg)) }, { nav.navigate("usage-access") }, { actions.refreshUsage(state.period, state.customDates) })
                            }
                            composable("focus") { FocusScreen(state, actions, ::editor, { nav.navigate("active") }, { nav.navigate("history") }) }
                            composable(EDITOR_ROUTE, arguments = listOf(
                                navArgument("template") { type = NavType.LongType; defaultValue = 0L }, navArgument("app") { defaultValue = "" },
                                navArgument("preset") { defaultValue = "" }, navArgument("minutes") { type = NavType.IntType; defaultValue = 0 })) { back ->
                                val seed = EditorSeed(back.arguments?.getLong("template") ?: 0, back.arguments?.getString("app"),
                                    back.arguments?.getString("preset").orEmpty(), back.arguments?.getInt("minutes") ?: 0)
                                IntentionEditor(state, seed, actions, { nav.popBackStack() }, {
                                    nav.navigate("active") { popUpTo(back.destination.id) { inclusive = true } }
                                }, platform.notificationPermission, { nav.navigate("active") })
                            }
                            composable("active") { ActiveSessionScreen(state, actions, { id, canceled ->
                                nav.navigate(if (canceled) "summary/$id" else "reflection/$id") { popUpTo("active") { inclusive = true } }
                            }, { mainTab("focus") }) }
                            composable("reflection/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { back ->
                                val id = back.arguments?.getLong("id") ?: 0
                                LaunchedEffect(id) { actions.loadRecord(id) }
                                val record = state.core.current?.takeIf { it.id == id } ?: state.core.history.find { it.id == id } ?: state.selectedRecords[id]
                                ReflectionScreen(record, state, actions) { nav.navigate("summary/$id") { popUpTo(back.destination.id) { inclusive = true } } }
                            }
                            composable("summary/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { back ->
                                val id = back.arguments?.getLong("id") ?: 0
                                LaunchedEffect(id) { actions.loadRecord(id) }
                                val record = state.core.current?.takeIf { it.id == id } ?: state.core.history.find { it.id == id } ?: state.selectedRecords[id]
                                SummaryScreen(record, { mainTab("home") }, { nav.navigate("history") }, { nav.navigate("reflection/$id") })
                            }
                            composable("history") { HistoryScreen(state, actions) { nav.navigate("summary/$it") } }
                            composable("settings") { SettingsScreen(state) { nav.navigate(it) } }
                            composable("my-goals") { GoalsScreen(state, actions, false, { nav.popBackStack() }) }
                            composable("usage-access") { UsagePermissionScreen(state, platform.usageSettings, { actions.refreshUsage(state.period, state.customDates) }) }
                            composable("notifications") { NotificationSettings(state, actions, platform.notificationPermission, platform.notificationSettings) }
                            composable("session-defaults") { SessionDefaults(state, actions) { nav.popBackStack() } }
                            composable("data-privacy") { DataPrivacyScreen(state, actions) }
                            composable("about") { AboutScreen { nav.navigate("welcome") } }
                        }
                    }
                }
            }
        }
    }
}
