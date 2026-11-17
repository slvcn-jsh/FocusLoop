# Phase 1 feasibility findings

Historical snapshot of the initial run. The user subsequently authorized continuation with a connected Fold5; see [current device and Phase 2 results](PHASE_2_RESULTS.md). The original observations below describe the earlier environment and are retained for traceability.

Date: 2026-11-17. Status: **limited experiment implemented and automated static/JVM gates passed; device feasibility not yet established. STOP before phase 2.** Implementation commit: `5ea79a1`. Starter and phase 0 audit preserved in `7f1d203`. Git history is local; no remote upload or private usage collection was performed.

## Completed

Read the complete kickoff/instructions and supplied research, product, design, feasibility and QA documents before implementation. The [phase 0 audit](PHASE_0_AUDIT.md) records screen inventory, contract navigation graph, observed tokens, reusable components, requirements/evidence crosswalk, state matrix, conflicts, risks and the limited implementation plan. All 12 original files under `design/` were compared byte-for-byte with the Git baseline; hashes are recorded in the [verification manifest](../qa/evidence/phase1-verification.json). Approved product/research/design specifications were not rewritten.

Initialized `android/` as an Android Studio Kotlin/Compose Material 3 project with a checksum-pinned Gradle wrapper. Versions are stable and compatible with locally available AGP 8.9.1 / Gradle 8.11.1 / Kotlin 2.0.21 / JDK 17 / SDK 35; they are not the latest release toolchain. [Android build instructions](../../android/README.md) include setup, commands, outputs and limitations.

The app is visibly labelled a feasibility experiment. It contains only:

- Optional Usage Access through Settings, AppOps checking, not-granted/revoked/unavailable distinctions, and foreground return/poll rechecks. Clearing visible usage while backgrounded and rechecking before publishing a query prevents presenting a prior grant's cached data as current on return. Revocation may take up to a foreground polling interval to appear if no lifecycle transition occurs; platform enforcement still applies.
- Today, yesterday and last seven local calendar days querying daily per-package foreground buckets. Requested and returned boundaries, possible expansion and unknown coverage are always explicit. No synthetic app durations, inferred purpose, screen-on total, or exact historical coverage. Package IDs provide a fallback without requesting broad installed-app visibility. No per-app records are persisted.
- A single locally persistent manual session: validated title/duration, pause/resume, append-only five-minute extension events, finish/cancel, reload and confirmed record clearing. Original plan is preserved. A singleton SQLite record is an intentionally small phase 1 experiment; full Room history and reflection/outcome storage remain phase 2+ work. Starting another session after an ended experiment replaces that ended record, with an on-screen explanation.
- Same-boot monotonic timing from persisted anchors: active duration excludes pauses; elapsed includes pauses; both remain independent of changes to wall time. Target expiry does not force finish or imply task success. Reboot, missing boot identity or backwards monotonic readings require explicit recovery; unobserved intervals stay unknown. Resuming counts only from the new anchor and retains the incomplete-timing label. Only the last durable transition/checkpoint is guaranteed after reboot; time since that checkpoint may be unknown. No periodic background timer service is required.
- An independent, voluntary 60-second **inexact** notification probe, permission request, channel/app settings, cancellation and recorded receiver lag. Permission grant alone does not schedule it. This is not integrated production session reminders. Receiver execution is not proof of notification visibility. Reboot/force-stop may lose the alarm; delivery and battery behavior are unverified.
- Native scalable diagnostic cards using the supplied palette, 48 dp action targets, insets/keyboard padding and single/paired panels based on available width. Production Home/Usage/Focus/Settings screens, navigation, charts, goals, templates and onboarding were not implemented.

App-owned persistence is accessed off the UI thread, with serialized actions. The activity uses one task to avoid competing UI session controllers. Corrupt record decoding is reported; automatic SQLite corruption deletion is disabled. Explicitly confirmed clearing can remove a corrupt experimental database. These safeguards compile but their Android storage-failure paths remain unverified at runtime.

Privacy surface: no Internet permission, remote analytics, account, cloud, AccessibilityService, interception, exact-alarm permission or blocking integration. Device usage stays in memory; one session plus minimal permission/probe metadata stays in app-private storage. Backup and transfer exclusions are configured; actual OEM transfer behavior is not verified. Session clearing does not remove Android-managed usage or the probe's separate metadata; uninstall/application storage clearing remains an OS control.

## Actual verification

Environment: Windows PowerShell, Microsoft JDK 17.0.20, installed SDK 35 / Build Tools 35.0.0. No physical ADB device was present at initial or final enumeration. A temporary `codexApi36` AVD instance was started read-only, headless, without snapshot saving on emulator port 5580. Its configuration points at an Android 36.1 image; this is configuration evidence, not observed guest OS verification. It appeared briefly in ADB, but did not provide a completed/usable boot during the attempt and slowed the host. Only that spawned instance was stopped. No emulator app launch, screenshots or instrumentation execution are claimed.

Final command from `android/`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain
```

Final command exited 0: **BUILD SUCCESSFUL in 26s; 76 actionable tasks, 16 executed, 60 up-to-date.** This is a final incremental verification, not a claimed clean-build benchmark. [Full command output](../qa/evidence/phase1-checks.txt) and [structured results](../qa/evidence/phase1-verification.json) are committed; normal Gradle reports and APKs remain in ignored build directories.

| Test ID | Steps / expected | Observed result | Status / limits |
|---|---|---|---|
| A01 | Generate wrapper, resolve dependencies, assemble debug APK | Wrapper generated and final debug APK built | PASS; no launch/device proof |
| A02 | Run SessionTest: transitions, single open/paused session, duration validation, pause math, wall-clock changes, sleep/background intervals, extensions, target expiry, repository recreation, reboot/recovery, terminal states and write failures | 18 tests, 0 failures/errors | PASS; pure JVM/domain behavior, memory store does not establish Android process survival |
| A03 | Run UsageTest: permission mapping, missing/empty/measured-zero distinctions, bucket expansion, package ranking, invalid records, Manila dates, DST and midnight | 8 tests, 0 failures/errors | PASS; synthetic inputs, not measured Android accuracy or permission grant/revoke |
| A04 | Lint min API/platform/resource correctness | 0 errors, 11 warnings | PASS with warnings retained: six dependency-update notices, one older target SDK notice, four optional SharedPreferences KTX suggestions |
| A05 | Compile/package Android instrumentation suite | Test APK built; three tests cover SQLite reopen/events, JSON roundtrip and malformed decoding | PASS for compilation only; all three runtime tests NOT RUN |
| A06 | Boot temporary read-only AVD to run instrumentation and capture runtime UI | No usable completed boot; instance stopped | BLOCKED for runtime validation; no runtime pass or screenshot |
| A07 | Compare original assets/specs against baseline; inspect changed-file whitespace | All 12 design assets identical; approved specifications unchanged; whitespace checks clean | PASS; local source/reference integrity only |
| F01-F16 | Execute physical Fold5 checklist | No physical phone connected | NOT RUN; actual Android version/build unknown |

Initial setup attempts are not hidden: wrapper generation before a settings file existed failed and was corrected by adding settings; a build started while the source was incomplete failed on missing MainActivity, then passed after the activity was added. The first full lint run correctly failed on `windowLightNavigationBar` requiring API 27 while minSdk is 26; the unnecessary attribute was removed. Final checks were rerun after the storage/task safeguards. No lint baseline or suppression was used to conceal these failures. The protected Usage Access manifest declaration has the expected `ProtectedPermissions` suppression because it is explicitly granted through special-access Settings; that does not grant the permission automatically.

APK: `android/app/build/outputs/apk/debug/app-debug.apk` (debug only). SHA-256: `c0db20babd8e47898b93c43924870f8a23434fc6e31c5bb71e7b5fb866117d10`. Unit HTML: `android/app/build/reports/tests/testDebugUnitTest/index.html`. Lint HTML: `android/app/build/reports/lint-results-debug.html`. Android test APK: `android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## Requirement, file and test mapping

Paths below are relative to the repository root. This maps all changed file groups; test method names are listed in the structured verification manifest. No new requirement IDs were invented.

| Changed files | Affected IDs | Verification | Remaining limitation |
|---|---|---|---|
| `.gitignore`; `android/settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`; `android/app/build.gradle.kts` | FR-02/12, maintainability/build quality | A01/A04/A05/A07 | Pinned older target/dependencies, no release pipeline |
| `android/app/src/main/AndroidManifest.xml`; `res/values/styles.xml`; `res/xml/backup_rules.xml`, `data_extraction_rules.xml`; `res/drawable/ic_launcher.xml`, `ic_reminder.xml` | FR-02/03/07/11/12 | A01/A04; source review | Provisional native loop icon, not a final supplied logo; OEM backup/runtime pending |
| `android/app/src/main/java/dev/focusloop/spike/domain/Session.kt`; `platform/AndroidClock.kt`, `SessionDatabase.kt` | FR-07/08/10/13/14 | A02/A05 | One experimental session, no production Room/history; real recovery/storage failures pending |
| Same source root: `domain/Usage.kt`, `platform/AndroidUsage.kt` | FR-03/04/05/10/14 | A03/A04 | Device coverage, grant/revoke and exact boundaries unverified; 14/28/custom deferred |
| Same source root: `platform/ReminderProbe.kt` | FR-11/14 | A01/A04/A05 | Delivery/denied/idle/force-stop not run; separate test probe only |
| Same source root: `MainActivity.kt`, `SpikeViewModel.kt`, `ui/SpikeScreen.kt` | FR-03/07/08/10/11/12/13/14 | A01/A04 | Layout branches compile; Fold5 resizing, accessibility and all runtime interactions pending |
| `android/app/src/test/java/dev/focusloop/spike/domain/SessionTest.kt`, `UsageTest.kt` | FR-03/04/05/07/08/10/14 | A02/A03, 26 passing tests | No production reflection-skip tests; FR-09 deliberately unimplemented |
| `android/app/src/androidTest/java/dev/focusloop/spike/platform/SessionDatabaseTest.kt` | FR-07/08/10/13 | A05 | Three Android tests compiled, not executed |
| `README.md`, `android/README.md`, `docs/engineering/PHASE_0_AUDIT.md`, `PHASE_1_RESULTS.md`, `docs/qa/PHASE_1_FOLD5_CHECKLIST.md`, `docs/qa/evidence/phase1-checks.txt`, `phase1-verification.json` | FR-02 through FR-05, FR-07/08/10 through FR-14 and traceability | A07, reported commands/results | Audit/report do not establish effectiveness or runtime capability |

The initial local Git commit also tracks the supplied starter files unchanged. `android/local.properties`, build outputs, `.gradle`, `.kotlin`, IDE metadata, emulator logs and temporary reference renders are ignored, not deliverables or private-data uploads.

## Platform evidence and unresolved findings

Context7 was used with one Android library resolution and two documentation calls. Official Android references were also checked for details not fully resolved by those snippets:

- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager): daily aggregation may expand beyond query boundaries; locked-user/null and retained-history limitations prevent promises of exact arbitrary-range statistics. The experiment exposes raw boundaries and unknown coverage rather than prorating bucket totals. It does not establish continuous historical coverage or app-opening counts.
- [SystemClock](https://developer.android.com/reference/android/os/SystemClock) and [BOOT_COUNT](https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT): use monotonic elapsed time including sleep within a boot, with boot identity for recovery; wall time is only event/date metadata.
- [AppOpsManager](https://developer.android.com/reference/android/app/AppOpsManager): special-access state is checked separately from whether a usage query happens to return rows. Platform enforcement/OEM behavior still needs F02/F03.
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission) and [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager): permission and channel controls are separate from a best-effort inexact alarm; timing/visibility requires measurement on the target phone.
- [AGP 8.9 compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes) supports the selected Gradle/JDK/API 35 chain. [Auto Backup controls](https://developer.android.com/identity/data/autobackup) informed backup/transfer exclusions. No production release compatibility or actual transfer test was completed.

The main unresolved product decisions remain precise usage-range eligibility and retention coverage, interpretation of multi-window totals, acceptable reboot recovery loss since the last durable checkpoint, notification reliability on Samsung battery settings, and actual cover/unfold/accessibility behavior. There is no StayFree comparison result yet. Design gaps and the ambiguous illustrated Sessions metric are recorded in the audit; they were not treated as implemented capabilities. Original research bibliographic uncertainties remain as documented in the evidence register; no effectiveness study, survey, or FocusLoop outcome finding was invented.

## Recommendation and review gate

First run [F01-F16 on the actual Fold5](../qa/PHASE_1_FOLD5_CHECKLIST.md), recording model/OS/build and keeping private usage evidence local. Prioritize manual mode, real grant/revoke, date/bucket comparison with StayFree, process death/reboot, reminder denial/delay and resizing. Run the three compiled Android tests on a selected usable test target. Review whether to accept unknown reboot gaps or require a later checkpoint policy, and which date filters can be honestly offered.

Only after reviewing these feasibility findings and device observations should phase 2 be explicitly authorized. The next implementation phase would be core intentions/session/event history and production Room persistence, with a deliberate transition from this disposable single-record experiment; analytics and Canva production screens remain subsequent gates. **Work stops here.**
