package dev.focusloop.spike.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import dev.focusloop.spike.SpikeState
import dev.focusloop.spike.domain.*
import dev.focusloop.spike.platform.*
import java.io.File
import java.time.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every action and usage row is synthetic. These tests never construct the production ViewModel. */
@RunWith(AndroidJUnit4::class)
class CompleteUiFlowTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fake: SyntheticActions
    private var permissionRequests = 0

    private fun launch(width: Int = 360, font: Float = 1f, onboarded: Boolean = true) {
        fake = SyntheticActions(onboarded)
        ui.runOnUiThread { ui.activity.enableEdgeToEdge() }
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                Box(Modifier.fillMaxSize().background(Color(0xFFE9EEF0)), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.width(width.dp).fillMaxHeight().testTag("synthetic-frame")) {
                        Text("UI TEST · SYNTHETIC DATA", Modifier.fillMaxWidth().background(Color(0xFFFFE8A6)).padding(6.dp))
                        FocusLoopApp(fake.state.value, fake, PlatformActions({ permissionRequests++ }, { permissionRequests++ }, { permissionRequests++ }), Modifier.weight(1f))
                    }
                }
            }
        }
        ui.waitForIdle()
    }

    private fun settle() { ui.mainClock.advanceTimeBy(350); ui.waitForIdle() }
    private fun activate(node: SemanticsNodeInteraction) {
        node.assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        settle()
    }
    private fun tab(name: String) { activate(ui.onNodeWithTag("nav-$name")) }
    private fun press(text: String) {
        ui.waitUntil(5_000) { ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        activate(ui.onNodeWithText(text))
    }
    private fun back() { activate(ui.onNodeWithContentDescription("Back")) }
    private fun page(tag: String) {
        try {
            ui.waitUntil(5_000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: ComposeTimeoutException) {
            throw AssertionError("Destination $tag did not appear. Synthetic UI only:\n" + ui.onNodeWithTag("synthetic-frame").printToString(), failure)
        }
        ui.onNodeWithTag(tag).assertExists()
    }
    private fun click(pageTag: String, text: String) {
        page(pageTag)
        ui.onNodeWithTag(pageTag).performScrollToNode(hasText(text, substring = false))
        settle()
        press(text)
    }
    private fun capture(name: String, dialog: Boolean = false) {
        ui.runOnUiThread {
            ui.activity.currentFocus?.clearFocus()
            WindowCompat.getInsetsController(ui.activity.window, ui.activity.window.decorView).hide(WindowInsetsCompat.Type.ime())
        }
        ui.waitUntil(5_000) { ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) != true }
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        val bitmap = (if (dialog) ui.onNode(isDialog()) else ui.onNodeWithTag("synthetic-frame")).captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    @Test fun compactFourDestinationsAndUsageDetailRetainFilter() {
        launch(); capture("compact-home")
        tab("usage"); capture("compact-usage")
        press("7 days")
        ui.runOnIdle { assertEquals(UsagePeriod.WEEK, fake.state.value.period) }
        click("usage-page", "Sample Reader")
        page("app-detail-page"); capture("compact-app-detail")
        back()
        page("usage-page")
        ui.onNodeWithTag("usage-page").performScrollToNode(hasText("7 days"))
        ui.onNodeWithText("7 days").assertIsSelected()
        click("usage-page", "Sample Reader")
        click("app-detail-page", "Create intention")
        page("editor-page")
        ui.onNodeWithText("Sample Reader").assertExists()
        back()
        tab("focus"); capture("compact-focus")
        tab("settings"); capture("compact-settings")
    }

    @Test fun sessionLifecycleRevisionAndSkippedReflectionStaySeparate() {
        launch(); tab("focus")
        click("focus-page", "+  New intention")
        page("editor-page")
        ui.onNodeWithText("Intention title").performTextInput("Read sample chapter")
        click("editor-page", "Save as template")
        capture("compact-editor")
        click("editor-page", "Start session")
        page("active-page"); capture("compact-active")
        click("active-page", "Pause")
        ui.runOnIdle { assertEquals(SessionStatus.PAUSED, fake.state.value.session!!.status) }
        click("active-page", "Resume")
        click("active-page", "Extend")
        press("Add time")
        click("active-page", "Voluntary checkpoint")
        press("Revise intention")
        ui.onNodeWithText("Revised intention").performTextReplacement("Read sample appendix")
        press("Save revision")
        click("active-page", "Finish session")
        page("reflection-page"); capture("compact-reflection")
        ui.runOnIdle {
            assertNull(fake.state.value.core.current!!.reflection)
            assertEquals(25 * 60_000L, fake.state.value.session!!.originalPlanMs)
            assertEquals("Read sample chapter", fake.state.value.session!!.title)
            assertEquals("Read sample appendix", fake.state.value.session!!.currentTitle)
            assertEquals(30 * 60_000L, fake.state.value.session!!.targetMs)
        }
        click("reflection-page", "Skip reflection")
        page("summary-page"); capture("compact-summary")
        ui.runOnIdle { assertEquals(ReportedOutcome.SKIPPED, fake.state.value.core.current!!.reflection!!.outcome) }
        click("summary-page", "View Focus history")
        page("history-page")
        ui.runOnIdle { assertEquals(1, fake.state.value.core.historyTotal); assertEquals(1, fake.state.value.core.intentions.size) }
    }

    @Test fun freshOnboardingCanSkipGoalsAndAllPermissions() {
        launch(onboarded = false); capture("compact-welcome")
        click("welcome-page", "Get started")
        click("onboarding-goals-page", "Skip for now")
        click("onboarding-privacy-page", "Continue")
        click("onboarding-access-page", "Not now — continue manually")
        page("home-page")
        ui.runOnIdle { assertTrue(fake.state.value.preferences.onboarded); assertTrue(fake.state.value.preferences.goals.isEmpty()); assertEquals(0, permissionRequests) }
    }

    @Test fun settingsValidateDefaultsAndRequireDeleteConfirmation() {
        launch(); tab("settings"); click("settings-page", "Session defaults")
        ui.onNodeWithText("Default minutes").performTextReplacement("0")
        ui.onNodeWithTag("defaults-page").performScrollToNode(hasText("Save defaults"))
        ui.onNodeWithText("Save defaults").assertIsNotEnabled()
        ui.onNodeWithTag("defaults-page").performScrollToNode(hasText("Default minutes"))
        ui.onNodeWithText("Default minutes").performTextReplacement("45")
        click("defaults-page", "Save defaults")
        ui.runOnIdle { assertEquals(45, fake.state.value.preferences.defaultMinutes) }
        click("settings-page", "My goals")
        press(PERSONAL_GOALS.first())
        click("goals-page", "Save goals")
        ui.runOnIdle { assertTrue(PERSONAL_GOALS.first() in fake.state.value.preferences.goals) }
        click("settings-page", "Data & privacy")
        click("privacy-page", "Delete session history…")
        press("Keep")
        ui.runOnIdle { assertEquals(0, fake.historyDeletions) }
        click("privacy-page", "Delete session history…")
        press("Delete history")
        ui.runOnIdle { assertEquals(1, fake.historyDeletions) }
        capture("compact-privacy")
    }

    @Test fun unfoldedPanelsAndCustomDatePickerAreReachable() {
        assumeTrue("Wide-layout proof requires an unfolded window of at least 680 dp; constrained cover layouts are not wide evidence.", ui.activity.resources.configuration.screenWidthDp >= 680)
        launch(width = 680); capture("unfolded-home")
        ui.onNodeWithTag("synthetic-frame").assertWidthIsEqualTo(680.dp)
        tab("usage"); capture("unfolded-usage")
        press("Custom")
        ui.onNodeWithText("Choose up to 366 days").assertExists()
        capture("unfolded-custom-dates", dialog = true)
        press("Cancel")
        tab("focus"); capture("unfolded-focus")
        tab("settings"); capture("unfolded-settings")
    }

    @Test fun narrowWindowAtDoubleFontKeepsNavigationAndFormsUsable() {
        launch(width = 320, font = 2f); capture("large-font-home")
        tab("focus")
        click("focus-page", "+  New intention")
        page("editor-page")
        ui.onNodeWithText("Intention title").performTextInput("Sample large text plan")
        click("editor-page", "Save intention for later")
        page("focus-page")
        tab("settings"); capture("large-font-settings")
        click("settings-page", "Session defaults")
        click("defaults-page", "Save defaults")
        page("settings-page")
    }

    @Test fun unavailableAndEmptyUsageNeverBecomeZeroOrBlockManualFocus() {
        launch(); tab("usage")
        for ((access, message) in listOf(UsageAccess.NOT_GRANTED to "Your usage is unavailable", UsageAccess.REVOKED to "Usage Access was turned off", UsageAccess.GRANTED to "Usage data is unavailable")) {
            ui.runOnIdle { fake.state.value = fake.state.value.copy(usage = fake.state.value.usage!!.copy(access = access, result = UsageResult.Unavailable("Synthetic unavailable result"))) }
            ui.onNodeWithText(message).assertExists()
        }
        ui.runOnIdle { fake.state.value = fake.state.value.copy(usage = fake.state.value.usage!!.copy(result = UsageResult.Empty)) }
        ui.onNodeWithText("No records available").assertExists(); capture("compact-empty-usage")
        tab("focus"); click("focus-page", "+  New intention")
        page("editor-page")
        ui.onNodeWithText("Intention title").performTextInput("Manual sample")
        click("editor-page", "Start session")
        page("active-page")
    }
}

private class SyntheticActions(onboarded: Boolean) : AppActions {
    val state = mutableStateOf(SpikeState(sessionLoaded = true, preferencesLoaded = true,
        preferences = UserPreferences(onboarded = onboarded), usage = if (onboarded) sampleUsage(UsagePeriod.TODAY) else
            sampleUsage(UsagePeriod.TODAY).copy(access = UsageAccess.NOT_GRANTED, result = UsageResult.Unavailable("Synthetic manual mode"), buckets = emptyList(), identities = emptyMap()),
        todayUsage = sampleUsage(UsagePeriod.TODAY), recentUsage = sampleUsage(UsagePeriod.WEEK)))
    private var id = 1L
    private var elapsed = 1000L
    var historyDeletions = 0
    private fun now(): ClockMark { elapsed += 1000; return ClockMark(1_000_000 + elapsed, elapsed, 7) }
    private fun update(record: SessionRecord) {
        val old = state.value.core
        val history = if (record.session.isOpen) old.history else listOf(record) + old.history.filterNot { it.id == record.id }
        state.value = state.value.copy(core = old.copy(current = record, history = history, historyTotal = history.size))
    }
    override fun startIntention(value: Intention, saveTemplate: Boolean, onSuccess: (Long) -> Unit) {
        val valid = IntentionRules.validate(value)
        if (saveTemplate) savePlan(valid) {}
        val record = SessionRecord(id++, SessionRules.start(state.value.session, valid.title, valid.plannedMinutes, now()), valid.targetPackage, valid.reminderPreferred, null)
        update(record); onSuccess(record.id)
    }
    override fun savePlan(value: Intention, onSuccess: () -> Unit) {
        val core = state.value.core
        state.value = state.value.copy(core = core.copy(intentions = core.intentions.filterNot { it.id == value.id } + value.copy(id = value.id.takeIf { it > 0 } ?: id++)))
        onSuccess()
    }
    override fun deletePlan(id: Long, onSuccess: () -> Unit) { state.value = state.value.copy(core = state.value.core.copy(intentions = state.value.core.intentions.filterNot { it.id == id })); onSuccess() }
    override fun sessionAction(event: EventKind, addedMinutes: Int, onSuccess: (Long) -> Unit) {
        val record = state.value.core.current!!
        update(record.copy(session = SessionRules.transition(record.session, event, now(), if (event == EventKind.EXTEND) addedMinutes * 60_000L else 0)))
        onSuccess(record.id)
    }
    override fun reviseIntention(title: String, onSuccess: () -> Unit) { val record = state.value.core.current!!; update(record.copy(session = SessionRules.revise(record.session, title, now()))); onSuccess() }
    override fun saveReflection(id: Long, outcome: ReportedOutcome, onSuccess: () -> Unit) { update(state.value.core.current!!.copy(reflection = Reflection(outcome, now().wallMs))); onSuccess() }
    override fun savePreferences(value: UserPreferences, onSuccess: () -> Unit) { state.value = state.value.copy(preferences = validPreferences(value)); onSuccess() }
    override fun clearHistory(onSuccess: () -> Unit) { historyDeletions++; state.value = state.value.copy(core = state.value.core.copy(current = null, history = emptyList(), historyTotal = 0)); onSuccess() }
    override fun clearPlans(onSuccess: () -> Unit) { state.value = state.value.copy(core = state.value.core.copy(intentions = emptyList())); onSuccess() }
    override fun loadMoreHistory() {}
    override fun loadRecord(id: Long) {}
    override fun refreshUsage(period: UsagePeriod, custom: CustomDates?) { state.value = state.value.copy(period = period, customDates = custom, usage = sampleUsage(period, custom)) }
    override fun retry() {}
    override fun dismissError() { state.value = state.value.copy(sessionError = null) }
    override fun testReminder() { state.value = state.value.copy(reminder = "Synthetic test only; no notification scheduled.") }

    companion object {
        private fun sampleUsage(period: UsagePeriod, custom: CustomDates? = null): UsageSnapshot {
            val zone = ZoneId.systemDefault(); val today = LocalDate.now(); val now = today.atTime(18, 0).atZone(zone).toInstant()
            val range = usageRange(period, now, zone, custom)
            val first = Instant.ofEpochMilli(range.startMs).atZone(zone).toLocalDate()
            val buckets = (0..java.time.temporal.ChronoUnit.DAYS.between(first, today)).flatMap { offset ->
                val date = first.plusDays(offset); val start = date.atStartOfDay(zone).toInstant().toEpochMilli(); val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                listOf(UsageBucket("org.example.reader", 2_100_000 + offset * 120_000, start, end), UsageBucket("org.example.notes", 600_000 + offset * 60_000, start, end))
            }
            return UsageSnapshot(UsageAccess.GRANTED, range, aggregateUsage(buckets, range), buckets,
                mapOf("org.example.reader" to AppIdentity("Sample Reader"), "org.example.notes" to AppIdentity("Sample Notes")))
        }
    }
}
