# Phase 2 bounded core continuation

2026-11-17. User continuation: "its now connected to my samsung fold sm device so lets now continue with the next best plans for all this". This authorizes continuing device validation and the next core phase; it is not treated as permission to silently implement all later phases. Original phase 1 findings remain a historical record. The connected SM-F946B reports Android 15/API 35, build AP3A.240905.015.A2.F946BXXS5EYI1. Initial unfolded framebuffer is 1812x2176 at density 420. Existing three Android storage tests passed on the phone and cold launch succeeded. Current on-device session is user-owned and must remain unchanged apart from migration/recovery projection; tests use separate database names.

Scope: FR-02/06/07/08/09/10/12/13/14. Reuse pure SessionRules, platform clock, optional usage and reminder adapters, ViewModel/Flow and diagnostic Compose entry point. Add only saved intention CRUD, multi-session Room history, per-session events, explicit optional self-reported outcomes, and confirmed history deletion. No production Canva screen rollout, charts/custom history claims, new permissions, blocking or production reminder scheduling.

Acceptance:

1. Saving/editing/reusing an intention snapshots title, original duration, optional target package and stored reminder preference into a new session; later template edits cannot alter past sessions. Saving a template does not start a session or monitor a target app.
2. At most one open session is enforced transactionally and with a unique nullable active-slot index, including concurrent callers. Existing monotonic/pause/reboot rules and original plan/event history remain intact. Finished records survive subsequent starts.
3. Reflection is a separate table. Absent, explicitly skipped, completed, partial and not completed are distinct. Ending a session never writes an outcome. Only ended sessions can receive a reflection.
4. Room writes persist state and new events atomically; events are appended and never overwritten by transitions. Schema export is checked in. Storage errors are visible; no destructive migration fallback.
5. Existing phase 1 database is retained as the rollback snapshot. An idempotent, transactional one-time import copies its complete record/events into a new core database. Import failure leaves the source and import marker unchanged. No simultaneous legacy writer remains after app upgrade. Rollback to the old app can read the preserved phase 1 snapshot, but cannot see later Room records; do not promise lossless downgrade synchronization.
6. Explicit confirmed history clearing removes core sessions/events/reflections and the preserved legacy session, while retaining the import tombstone to prevent resurrection. Saved intentions and device statistics are unaffected and the confirmation says so. Failure clearing the source is surfaced and retryable.
7. Build/lint/JVM checks plus real-device tests with synthetic, isolated databases prove reopen/history, single-session races, reflection semantics, event integrity and import/deletion retries. Live user record comparison checks preservation without committing its content. Private usage/screenshots remain ignored local files; committed evidence contains only redacted environment and test statuses.

Toolchain: retain Kotlin 2.0.21/AGP 8.9.1 and add Room 2.7.2 with matching KSP 2.0.21-1.0.28. Room 2.7 targets Kotlin 2.0; this avoids adopting a new Room major or upgrading the whole toolchain. Current docs were fetched through Context7 and primary Android references; some Context7 results discuss Room 3, so only applicable androidx.room 2 APIs are used.

Device findings may justify narrow corrections, such as using the actual unfolded window width rather than a fixed screenshot width for paired diagnostics. Physical fold/unfold, reboot, clock changes, notification opt-in and StayFree comparison remain distinct manual cases; no automated pass is inferred.

Stop after this core path is implemented, verified and documented. Recommend the next gated analytics/UI work from measured results; do not expand to all production phases in this turn.
