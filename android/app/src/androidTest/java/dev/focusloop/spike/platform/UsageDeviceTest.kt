package dev.focusloop.spike.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.focusloop.spike.domain.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageDeviceTest {
    @Test fun userGrantedRecentRangesReturnExplicitDataStates() {
        val adapter = AndroidUsage(InstrumentationRegistry.getInstrumentation().targetContext)
        assumeTrue("User has not opted into Usage Access", adapter.access() == UsageAccess.GRANTED)
        UsagePeriod.entries.forEach { period ->
            val snapshot = adapter.query(period, if (period == UsagePeriod.CUSTOM) {
                val today = java.time.LocalDate.now()
                CustomDates(today.minusDays(3), today)
            } else null)
            assertEquals(UsageAccess.GRANTED, snapshot.access)
            assertTrue(snapshot.requested.startMs <= snapshot.requested.endMs)
            assertFalse("Android service did not return an accessible result for ${period.name}", snapshot.result is UsageResult.Unavailable)
            // No packages, durations or raw usage records are logged or exported.
        }
    }
}
