# FocusLoop Android feasibility experiment

Open this `android/` directory in Android Studio, select JDK 17, install SDK Platform 35 and Build Tools 35.0.0, and sync. The root project outside this directory contains the approved specifications and references. Package/application ID: `dev.focusloop.spike`; minimum API 26, compile/target API 35. This is an experimental debug application, not a release.

Pinned tooling: Gradle 8.11.1 (wrapper distribution checksum verified against services.gradle.org), Android Gradle Plugin 8.9.1, Kotlin and Compose compiler plugin 2.0.21, Compose BOM 2024.10.00, Activity 1.9.3, Lifecycle 2.8.7, coroutines 1.9.0. These stable versions were chosen around the available local toolchain; no upgrade to preview SDKs was needed. Release targeting and dependency review belong to a later phase.

PowerShell, from this directory:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
# Only with an explicitly selected connected test device/emulator:
.\gradlew.bat :app:connectedDebugAndroidTest
```

If Android Studio has not created local.properties, set `ANDROID_HOME` to your SDK directory for the current terminal or create an ignored local.properties with `sdk.dir=C:/path/to/Android/Sdk`. Never commit machine-specific SDK paths.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Tests: `app/build/reports/tests/testDebugUnitTest/index.html`; lint: `app/build/reports/lint-results-debug.html`; connected-test reports: `app/build/reports/androidTests/connected/debug/index.html` when run.

## Experiment boundary

- Optional Usage Access via Android Settings; AppOps rechecked on return and while foreground. Today/yesterday/last seven calendar days query raw daily Android foreground buckets. Returned bounds and unknown coverage are shown; no exact custom-range or screen-on totals are claimed. Package identifiers are used directly; no broad installed-app permission is requested.
- Manual single-session start, pause/resume, extend five minutes, finish/cancel, reload and confirmed clearing. All storage operations run off the UI thread and actions are serialized. The current session plus its extension events is stored atomically in one SQLite row; starting after an ended session replaces that experiment record. This is not a production Room/history schema.
- Active time excludes explicit pauses; elapsed includes pauses. Both include time outside FocusLoop while running as appropriate. Same-boot recovery uses monotonic anchors. After reboot or missing/changed boot identity, only last persisted known intervals remain; gap is marked unknown and user explicitly resumes or ends. No outcome/reflection is collected or inferred. Timer expiry does not force completion.
- Independent opt-in 60-second inexact notification probe with request, Settings, schedule, cancel, receiver-lag status. Granting permission alone does not schedule it. No exact alarm, persistent service, boot rescheduling, or production session checkpoints. Receiver delivery does not prove notification visibility.
- Compose Material 3, ViewModel, coroutines/StateFlow, pure Kotlin domain and Android adapters. Uses supplied palette and responsive single/paired diagnostic panels. Full Canva screens and navigation remain deferred.
- No Internet permission, analytics SDK, accounts, cloud, AccessibilityService, interception or blocking. Usage records are queried into memory only. Session record, permission history and reminder probe metadata are local, with backup/transfer exclusions. Clearing the experimental session does not delete Android-managed device statistics. Platform backup/transfer behavior still requires physical validation.

See [phase 0 audit](../docs/engineering/PHASE_0_AUDIT.md), [phase 1 results](../docs/engineering/PHASE_1_RESULTS.md) and [Fold5 checklist](../docs/qa/PHASE_1_FOLD5_CHECKLIST.md). Do not proceed into production implementation until phase 1 is reviewed and explicit authorization is given.
