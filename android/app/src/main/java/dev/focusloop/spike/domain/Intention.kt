package dev.focusloop.spike.domain

data class Intention(val id: Long = 0, val title: String, val plannedMinutes: Int,
    val targetPackage: String? = null, val reminderPreferred: Boolean = false)

object IntentionRules {
    fun validate(value: Intention): Intention {
        require(value.title.isNotBlank() && value.title.length <= 120) { "Enter a title (1-120 characters)." }
        require(value.plannedMinutes in 1..1440) { "Use a duration from 1 to 1440 minutes." }
        val target = value.targetPackage?.trim()?.takeIf { it.isNotEmpty() }
        require(target == null || (target.length <= 255 && target.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")))) {
            "Use an Android package identifier or leave the target empty."
        }
        return value.copy(title = value.title.trim(), targetPackage = target)
    }
}

enum class ReportedOutcome(val label: String) {
    COMPLETED("Completed"), PARTIAL("Partially completed"), NOT_COMPLETED("Not completed"), SKIPPED("Prefer not to answer")
}
data class Reflection(val outcome: ReportedOutcome, val recordedWallMs: Long)
data class SessionRecord(val id: Long, val session: Session, val targetPackage: String?,
    val reminderPreferred: Boolean, val reflection: Reflection?)
data class CoreSnapshot(val current: SessionRecord? = null, val intentions: List<Intention> = emptyList(),
    val history: List<SessionRecord> = emptyList(), val historyTotal: Int = history.size)
