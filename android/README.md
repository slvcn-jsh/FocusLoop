# FocusLoop native Android

Open this android/ directory in Android Studio, select JDK 17, install SDK Platform 35 and Build Tools 35.0.0, and sync. The parent repository contains the approved research, product specifications and design references. Application ID remains `dev.focusloop.spike` to preserve installed data across updates. Version **0.3-native-ui**, code 3; minimum API 26, compile/target API 35. This is a development build.

Pinned tooling: Gradle 8.11.1 (checksum pinned), AGP 8.9.1, Kotlin/Compose compiler 2.0.21, Compose BOM 2024.10.00, Navigation Compose 2.8.5, Activity 1.9.3, Lifecycle 2.8.7, coroutines 1.9.0, Room 2.7.2 and KSP 2.0.21-1.0.28. Exported Room schemas 1 and 2 stay version-controlled. No destructive migration fallback or main-thread database queries are enabled. Release targeting/dependency review remains pending.

PowerShell, from this directory:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
# Only with an explicitly selected connected test device/emulator:
.\gradlew.bat :app:connectedDebugAndroidTest
```

If Android Studio has not created local.properties, set ANDROID_HOME to the SDK directory or create an ignored local.properties with sdk.dir=C:/path/to/Android/Sdk. Do not commit machine paths.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Reports: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html`; connected Gradle runs also produce `app/build/reports/androidTests/connected/debug/index.html`. Direct ADB runner evidence is documented in the results report.

## Implemented behavior

- Native Compose Home/Usage/Focus/Settings, optional onboarding/goals/privacy/permissions and native back navigation. Compact bottom navigation, an expanded rail and paired Home panels adapt to available width; large text stacks content. Editor/navigation state uses saved state. Existing installs enter Home with their record; fresh installs see onboarding.
- Optional Usage Access; denied/revoked/empty/unavailable states remain distinct from measured zero. Today/yesterday/7/14/28/custom filters inspect raw Android daily foreground buckets, with requested/returned bounds and unknown coverage. Custom selections reject future/reversed ranges and more than 366 days. App detail, missing-aware charts, local package labels/icons and optional target preselection are connected. No exact calendar totals, daily averages, screen-on totals, app-opening counts or task outcomes are invented.
- Reusable intentions, optional app targets and reminder choice. Saving a plan does not start a session. Starting and optionally saving are atomic. A unique nullable active-slot index and Room transactions protect the single-open-session invariant.
- Manual pause/resume, duration extensions, voluntary checkpoint, wording revision, finish and cancel. Original title/duration remain immutable; extension/revision events append. Same-boot recovery uses monotonic anchors. After reboot/unknown boot identity the gap stays unknown and resumption is explicit. No timer service, wake lock or per-second SQLite writes.
- Optional reflection is stored separately. Absent, explicitly skipped, completed, partial and not completed remain distinct. Finishing never assigns an outcome. Summary/history expose original plans, timing, extensions, revision events and the user's own report. History loads in batches without deleting older records.
- Local goals/defaults and optional inexact session reminders. Pause/cancel/finish cancels the pending checkpoint; resume/extend reschedules eligible opted-in sessions. Delivery rechecks current session state and permission, uses generic notification text and never changes session/outcome. Idle, battery restrictions, reboot and force-stop can delay or lose reminders. A separate one-minute test reminder does not change the session. No exact alarm, service or boot rescheduling.
- Settings expose separate confirmed deletion of session history versus templates. History clearing removes sessions/events/reflections and the preserved legacy record, keeping a tombstone against reimport. Templates and Android usage remain. Deleting templates retains session snapshots. Storage failures are visible and disable mutations until retry/recovery.
- Additive Room v1→v2 migration introduces nullable event text for revisions; original user rows remain intact. Legacy feasibility.db remains a preserved recovery snapshot until explicit deletion. Core v1 software cannot open v2; downgrading is unsupported. Corruption never triggers automatic file deletion.
- No Internet permission, analytics SDK, account, cloud, AccessibilityService, interception or blocking. Usage rows remain memory-only and clear when backgrounded. App-owned records live in Room; preferences hold goals/defaults/permission/reminder metadata. Databases and preferences have backup/transfer exclusions; actual OEM transfer remains unverified.

See [current results](../docs/engineering/NATIVE_UI_RESULTS.md), [implementation blueprint](../docs/design/UI_IMPLEMENTATION_BLUEPRINT.md) and [historical Fold5 checklist](../docs/qa/PHASE_1_FOLD5_CHECKLIST.md). Exact usage accuracy, real notification delivery under restrictions, TalkBack and the full physical resize/reboot matrix require separate evidence.
