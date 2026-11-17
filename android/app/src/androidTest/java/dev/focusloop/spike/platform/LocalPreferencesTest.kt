package dev.focusloop.spike.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.focusloop.spike.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalPreferencesTest {
    @Test fun freshOnboardingAndGoalsSurviveRecreationWithoutChangingCoreRecords() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "synthetic-preferences-${System.nanoTime()}"
        try {
            val first = LocalPreferences(context, name, existingCoreInstall = false)
            assertFalse(first.read().onboarded)
            assertFalse(LocalPreferences(context, name, existingCoreInstall = true).read().onboarded)
            val saved = UserPreferences(true, setOf("Study or work"), 40, true)
            first.write(saved)
            assertEquals(saved, LocalPreferences(context, name, existingCoreInstall = true).read())
        } finally { context.deleteSharedPreferences(name) }
    }
}
