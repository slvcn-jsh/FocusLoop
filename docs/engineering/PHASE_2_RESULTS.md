# Connected Fold5 and Phase 2 core results

2026-11-17. User authorized continuation after the initial feasibility report and connected a physical Samsung Fold5. Scope followed [PHASE_2_PLAN.md](PHASE_2_PLAN.md): complete available device evidence and implement the next bounded core slice. No phase 3 analytics expansion, production Canva screen rollout or full release integration was undertaken.

## Completed core path

- Saved intentions: create, edit, reuse and confirmed delete; optional target package and reminder preference. Saving does not start a session. The target is user-entered and does not monitor/block another app. The reminder preference is stored, with an explicit notice that production session reminders are not yet scheduled.
- Room session history: multiple persisted records; one open session protected by a transaction and a unique nullable active-slot index. Session start snapshots the title, original duration, target and preference. Template edits/deletion cannot rewrite a recorded session. Existing monotonic timing, pause/resume, finish/cancel and deliberate extension behavior is reused.
- Append-only session events: transitions update state and append new events in the same transaction. Prior events and original plan cannot be overwritten by normal transitions. Ended sessions survive the next start. Diagnostic history shows the latest 100 ended sessions while retaining older records in storage.
- Separate optional reflections: absent, explicitly skipped, completed, partial and not completed remain distinct. Finishing never assigns an outcome. Only a finished session accepts reflection; an explicit later submission can change it. No completion percentage or effectiveness result is inferred.
- Confirmed deletion: core sessions/events/reflections and the preserved phase 1 record are cleared; saved intentions and Android-managed device usage remain. Import tombstone prevents erased history from reappearing. A failed source deletion is reported and retryable.
- Storage recovery: Room schema v1 is exported, with no destructive schema fallback or main-thread queries. A preserving SQLite helper reports corruption instead of deleting the database automatically. Severe core corruption may require explicit OS-level storage recovery/reset; the app does not silently replace it.
- Native diagnostic UI: connected the above path to the existing Compose/ViewModel/Flow surface. Adjusted paired panels to >=600 dp available width, with a single-column fallback for narrow windows or font scale above 1.3. This addresses the observed unfolded framebuffer width; full fold/resize accessibility behavior still requires physical checks. Production Home/Usage/Focus/Settings and onboarding remain later work.

## Existing record and migration

The phone contained an active session not identifiable as the scripted synthetic test. Its content was not logged, committed, overwritten, paused, finished or deleted. Automated tests instead used uniquely named synthetic databases.

The application ID and signing identity were retained for an in-place update, version 0.2-core/code 2. The new `focusloop-core.db` imports the old `feasibility.db` once, with marker and copied events committed transactionally. Import failures roll back and can retry; the original database remains the recovery snapshot. Actual on-phone comparison established:

1. The old record remained logically unchanged after the update.
2. The imported record preserved the original plan, stored timing state and every event.
3. No self-reported outcome was invented during import.
4. The record survived backgrounding, an actual `am kill` of the background app process, and a successful cold relaunch. The process was confirmed absent before relaunch; persisted state/events matched afterward.

This proves record preservation across the measured update/restart. Automated visual timer continuity was **not asserted**: host accessibility dumps could not obtain idle state while the active view updated. That tooling result is not recorded as a UI pass. The user separately confirmed "Same session; folding and unfolding work" after checking the installed update. This is user-reported physical evidence, not an automated layout/timer-accuracy assertion. The phone was not rebooted, force-stopped for alarm testing, or subjected to global clock/font/display changes.

Rollback boundary: an older Phase 1 build can read the retained old snapshot, but will not see later Room records. No downgrade synchronization is claimed. Retaining the source is an intentional compatibility step; it is removed only by explicit confirmed history deletion. Confirmed core deletion commits its tombstone before clearing the source, so source-clear failure cannot resurrect history.

## Actual verification

Device measured through ADB: **Samsung SM-F946B, Android 15/API 35**, build `AP3A.240905.015.A2.F946BXXS5EYI1`, physical framebuffer 1812x2176, density 420, font scale 1.0. Device serial and private records are omitted. Usage Access initially default/not granted, then reported allowed after the user explicitly enabled it in Settings. Permissions were not forced through shell commands.

Host: Windows, JDK 17, existing AGP 8.9.1/Gradle 8.11.1/Kotlin 2.0.21 chain. Added Room 2.7.2 and matching KSP 2.0.21-1.0.28; Room 2.7 is compatible with Kotlin language 2.0. These are pinned compatible versions, not a latest-dependency claim.

Final static command from `android/`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain
```

Result: exit 0, **BUILD SUCCESSFUL in 1m 32s**, 79 tasks (22 executed, 57 up-to-date). This was incremental verification. **29 JVM tests passed**, zero failures/errors. Lint reported **zero errors, 13 warnings and one informational suggestion**: eight dependency notices, four SharedPreferences KTX suggestions, one older-target notice, and an informational primitive-state suggestion. Warnings remain visible; no lint baseline was added.

Installed both APKs on the selected physical phone and ran the instrumentation runner with the device selector:

```powershell
& $adbPath -s $deviceSerial install -r android/app/build/outputs/apk/debug/app-debug.apk
& $adbPath -s $deviceSerial install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adbPath -s $deviceSerial shell am instrument -w dev.focusloop.spike.test/androidx.test.runner.AndroidJUnitRunner
```

Result: **OK (15 tests)**, runner time 1.358s. Updated activity then cold-launched successfully, observed total launch time 901ms. These are observations for this run, not performance/battery benchmarks.

| Proof | Observed result | Requirements / boundary |
|---|---|---|
| SessionTest | 18 JVM tests passed | FR-07/08/10/14; deterministic transition/timing rules with synthetic clocks |
| UsageTest | 8 JVM tests passed | FR-03/04/05/10; mapping, missing/zero, raw bounds, calendar/DST behavior |
| IntentionTest | 3 JVM tests passed | FR-06/09/10; validation, optional target, distinct skipped/unreported semantics |
| CoreRepositoryTest | 11 physical Android tests passed | FR-02/06/07/08/09/10/13; reopen/history, concurrent callers, unique index, reflection, template snapshot, rollback, idempotent import, import retry, reboot-clock simulation, corrupt-file preservation, deletion retry |
| SessionDatabaseTest | Original 3 physical tests passed | FR-07/08/10/13; legacy SQLite reopen and codec paths now actually executed |
| UsageDeviceTest | 1 physical test passed; each of Today/Yesterday/7 days returned accessible explicit data states | FR-03/04/05/10; granted API access works on this phone; app packages/durations were not exported. This does not establish exact totals or continuous coverage |
| Live update/restart preservation | Source unchanged; imported plan/state/events match; no reflection; actual background process kill and cold relaunch preserve stored record | FR-07/08/10; exact timer continuity remains unmeasured |
| User visual/fold check | User confirmed same session and usable controls through folding/unfolding | FR-07/12; manual user-reported pass for this basic sequence, not full resize/accessibility coverage |
| Original source assets | All original design files still byte-identical to initial baseline | FR-12/design traceability; no reference overwrite |

Saved evidence: [structured verification](../qa/evidence/phase2-verification.json), [build log](../qa/evidence/phase2-checks.txt), [physical test log](../qa/evidence/phase2-device-tests.txt). These contain synthetic test results and redacted metadata only. Raw device snapshots, usage views, XML and the private comparison record stay under ignored `tmp/`; no private device usage was committed or uploaded.

APK: `android/app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `6c7bd01bf34e833e5f00f97d8c56308782fc034b3ba3a94d45499eef6f50601b`. Android Studio setup and test commands are in [android/README.md](../../android/README.md).

## Files and affected requirements

| Changed file group | Requirements | Validation / limitation |
|---|---|---|
| `android/build.gradle.kts`, `android/app/build.gradle.kts` | Core build quality, FR-02/06/07/09 | Room/KSP, code/version bump; build/lint passed; older target/dependency notices retained |
| `android/app/src/main/java/dev/focusloop/spike/domain/Intention.kt` | FR-06/09/10 | Separate intention, session-record and self-reported reflection models; JVM/domain and Android repository tests |
| Same source root: `data/CoreDatabase.kt`, `data/CoreRepository.kt`; `android/app/schemas/dev.focusloop.spike.data.CoreDatabase/1.json` | FR-02/06/07/08/09/10/13 | Exported Room schema, transactions, events, import/clear markers, preserving corruption helper; 11 physical tests |
| Same source root: `SpikeViewModel.kt`, `ui/SpikeScreen.kt` | FR-06/07/08/09/10/12/13/14 | Core actions, error versus stale-record state, history/reflection, native adaptive diagnostics; compiled/lint checked; full UI workflow/accessibility not automatically verified |
| `android/app/src/test/java/dev/focusloop/spike/domain/IntentionTest.kt`; `android/app/src/androidTest/java/dev/focusloop/spike/data/CoreRepositoryTest.kt`, `platform/UsageDeviceTest.kt` | FR-02/03/04/05/06/07/08/09/10/13 | New meaningful validation, storage/race/import and optional actual API availability tests |
| `README.md`, `android/README.md`, `docs/engineering/PHASE_2_PLAN.md`, `PHASE_2_RESULTS.md`, historical-report/checklist pointers, `docs/qa/evidence/phase2-*` | All affected IDs / traceability | Scope, commands, evidence and limits updated; previous report retained as history |

Original SessionRules and legacy database code were reused. Approved product/research/design specifications and reference assets were not rewritten. The implementation continues to request only optional usage special access and notification permission; no Internet, AccessibilityService, blocking, account or cloud capability was added.

## Remaining physical checks and next phase

The initial [F01-F16 checklist](../qa/PHASE_1_FOLD5_CHECKLIST.md) remains the manual protocol. Current evidence establishes installation/launch, real storage tests, granted recent usage availability, upgrade preservation and persisted-record recovery. Basic fold/unfold and visible session restoration are now user-confirmed. It does **not** establish complete F01-F16 passes. Still pending: real revoke/return states; same-range StayFree comparison and retention/bucket discrepancies; paused-session visual process recovery; actual reboot and wall-clock changes; notification denial, delivery, cancellation and battery restrictions; rotation/split-screen and large-font/TalkBack checks; OEM backup/transfer.

User visual response received in this session: "Same session; folding and unfolding work". F13 therefore has a user-reported pass for the basic fold/unfold sequence while the broader resize/rotation matrix remains pending. No screenshot, timing measurement or TalkBack verification was supplied with that response.

Next recommendation: complete the remaining targeted Fold5 observations, then Phase 3 analytics with honest requested/returned bounds, missing-data coverage and per-app details. Exact 14/28/custom ranges remain conditional on device evidence. Phase 4 should translate the approved Canva screens into native adaptive navigation and reusable components; the live Canva site was inaccessible in the initial audit, so local exports remain the observed visual source. Phase 5 should integrate optional best-effort session reminders and broader recovery/accessibility/release checks. Do not market the diagnostic surface as the final Canva UI or claim research effectiveness.

Current work stops after this verified core slice. The user can continue the preserved session in the installed prototype.

## Documentation sources

Current docs were fetched with Context7 (Android library resolution and two focused queries) and checked against primary sources: [Room 2.7 release notes](https://developer.android.com/jetpack/androidx/releases/room#2.7.0), [RoomDatabase transactions](https://developer.android.com/reference/androidx/room/RoomDatabase), [migration and data preservation](https://developer.android.com/training/data-storage/room/migrating-db-versions). Some Context7/general setup results described Room 3; this implementation uses the compatible `androidx.room` 2 APIs. No research claim or effect size was added.
