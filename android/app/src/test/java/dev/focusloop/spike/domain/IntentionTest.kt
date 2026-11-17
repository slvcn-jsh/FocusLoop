package dev.focusloop.spike.domain

import org.junit.Assert.*
import org.junit.Test

class IntentionTest {
    @Test fun optionalTargetCanBeAbsentAndTitleIsTrimmed() {
        val value = IntentionRules.validate(Intention(title = "  Study  ", plannedMinutes = 25, targetPackage = " "))
        assertEquals("Study", value.title)
        assertNull(value.targetPackage)
    }
    @Test fun validatesDurationAndTargetWithoutInferringPurpose() {
        assertThrows(IllegalArgumentException::class.java) { IntentionRules.validate(Intention(title = "Study", plannedMinutes = 0)) }
        assertThrows(IllegalArgumentException::class.java) { IntentionRules.validate(Intention(title = "Study", plannedMinutes = 25, targetPackage = "https://example.com")) }
        assertEquals("org.example.reader", IntentionRules.validate(Intention(title = "Rest", plannedMinutes = 10, targetPackage = "org.example.reader")).targetPackage)
    }
    @Test fun skippedAndUnreportedAreDistinctFromCompletion() {
        val unanswered: Reflection? = null
        val skipped = Reflection(ReportedOutcome.SKIPPED, 1)
        assertNull(unanswered)
        assertNotEquals(ReportedOutcome.COMPLETED, skipped.outcome)
        assertNotEquals(ReportedOutcome.NOT_COMPLETED, skipped.outcome)
    }
}
