# Phase 1 physical Fold5 verification

This checklist retains its initial statuses as the baseline. See [current device/core results](../engineering/PHASE_2_RESULTS.md) for later executed checks and remaining gaps.

All physical tests below start as NOT RUN. Emulator evidence in PHASE_1_RESULTS.md does not change their status. Use synthetic intention text; keep real per-app usage, device identifiers and screenshots private. Record OS/build locally, but redact serials before sharing.

## Setup and evidence template

Open `android/` in Android Studio and run the debug app on a selected Fold5. Or, in PowerShell from `android/`:

```powershell
$adbPath = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
& $adbPath devices -l
# Replace the example with the selected device serial; do not run against another device.
$deviceSerial = 'REPLACE_WITH_DEVICE_SERIAL'
& $adbPath -s $deviceSerial shell getprop ro.product.model
& $adbPath -s $deviceSerial shell getprop ro.build.version.release
& $adbPath -s $deviceSerial shell getprop ro.build.version.sdk
& $adbPath -s $deviceSerial shell getprop ro.build.display.id
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
& $adbPath -s $deviceSerial install -r app/build/outputs/apk/debug/app-debug.apk
& $adbPath -s $deviceSerial shell am start -n dev.focusloop.spike/.MainActivity
```

For each case record: ID; date; model/OS/build; orientation; window dimensions/font scale; permission state; exact steps; expected; observed; PASS/FAIL/NOT RUN; private evidence location; caveats. Never use a blank observation as a pass. Measure requested and actual reminder times independently. Do not upload usage exports or private screenshots to Git.

## Cases

| ID / requirements | Steps | Expected | Initial status |
|---|---|---|---|
| F01 / FR-03/07/10/14 | Fresh install, leave Usage Access denied; enter Synthetic study / 1 minute, Start, wait, Pause/Resume/Finish | Session works; denied analytics show unavailable, no fake zero; finishing reports no task-success outcome | NOT RUN |
| F02 / FR-03/04 | Tap Open Usage Access settings, grant for FocusLoop Feasibility, return, inspect each filter | Access shows granted; only returned device records displayed; requested/returned bounds and unknown coverage remain visible | NOT RUN |
| F03 / FR-03/10 | With data shown and session running, revoke in system Settings; return, then try Refresh. Repeat grant/revoke while paused | Old usage removed; revoked label after a previously observed grant; manual session untouched | NOT RUN |
| F04 / FR-04/05/10 | Choose Today, Yesterday, 7 days. Compare same local dates and a few known apps with StayFree on this phone, without exporting private data | Record discrepancies and boundary differences; no claim of equality or exact range without evidence. Foreground sums may overlap in multi-window | NOT RUN |
| F05 / FR-04/05 | Select a day/window with no retained data if available; inspect before first unlock after reboot once phone permits app launch | Empty/unavailable distinguished from zero; retained history is not assumed continuous. Note if locked-state test cannot be performed | NOT RUN |
| F06 / FR-07 | Start synthetic 5-minute session, wait, pause ~10 seconds, resume, extend twice, finish, reload | Original plan 5 min; two +5 min events; active excludes pause, elapsed includes it; final values stop changing; second open session impossible | NOT RUN |
| F07 / FR-07/08 | Start session, note time, leave with Home; from host use process-death command below; relaunch after ~20 seconds. Repeat while paused | Same-boot record recovered; running interval includes absence, paused active does not; extensions remain. No forced completion at target | NOT RUN |
| F08 / FR-07/10 | Start session, persist an extension/pause, reboot phone manually with consent and return after unlock | Recovery required; only last persisted checkpoint survives, gap clearly unknown. Resume starts counting from now; unknown label remains after Finish | NOT RUN |
| F09 / FR-07/10 | During synthetic session, temporarily change wall time/timezone through Settings; restore automatic date/time afterward | Same-boot active/elapsed remain monotonic, not wall-clock derived; date filters follow new local calendar on Refresh | NOT RUN |
| F10 / FR-11/14 | Keep notifications denied; tap Schedule probe; then request permission and deny, run session | No delivery promise, no session failure; permission request alone does not schedule | NOT RUN |
| F11 / FR-11 | Grant notifications, explicitly Schedule probe; note target +60 s, background app and observe. Refresh delivery status. Repeat screen-off and battery-restricted/idle conditions | No early alarm; late/absent delivery recorded honestly; receiver lag is not notification visibility. No exact-alarm permission request | NOT RUN |
| F12 / FR-11/14 | Schedule then Cancel; schedule then disable notification channel; schedule then reboot/force-stop | Cancel clears alarm/notification; disabled channel yields no notification; reboot/force-stop may lose alarm. No reliability guarantee | NOT RUN |
| F13 / FR-12/07 | Fold/unfold during running and paused sessions; rotate; drag split-screen divider through narrow/medium/expanded widths | Current record survives; no duplicate Start; responsive panels, scrolling and all actions reachable; keyboard/insets do not hide fields | NOT RUN |
| F14 / FR-12 | Increase system font to 200%, largest display size, narrow split window; enable TalkBack and traverse all diagnostic controls | Text wraps, no clipped essential actions, clear labels/read order, >=48 dp targets; timer text does not steal focus | NOT RUN |
| F15 / FR-02/13 | Clear experimental session, first dismiss confirmation, then confirm; relaunch | Dismiss preserves; confirm removes session/events; Android-managed usage remains; fresh manual session available | NOT RUN |
| F16 / FR-02/10 | Inspect manifest/source and OS backup/transfer behavior on an appropriate test setup | No Internet/Accessibility/blocking/account integration; database/preferences excluded from backup/transfer. This is not an audited security or transfer guarantee | NOT RUN |

Process death while backgrounded (synthetic session only):

```powershell
& $adbPath -s $deviceSerial shell input keyevent KEYCODE_HOME
& $adbPath -s $deviceSerial shell am kill dev.focusloop.spike
# Wait approximately 20 seconds, then relaunch.
& $adbPath -s $deviceSerial shell am start -n dev.focusloop.spike/.MainActivity
```

`am kill` targets background process death. `am force-stop` also changes alarm/background behavior and is a separate stronger interruption; do not equate the two. Do not use `pm clear`, uninstall, or wipe as a process-death test because those intentionally delete the record. Reminder probe is independent of session state; cancelling a session does not cancel a separately scheduled probe.

On a selected device, instrumentation can be run without Gradle choosing every attached device:

```powershell
& $adbPath -s $deviceSerial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adbPath -s $deviceSerial shell am instrument -w dev.focusloop.spike.test/androidx.test.runner.AndroidJUnitRunner
```

Production reflection-skip tests, full navigation/back behavior, custom filters, Room history, goals and production deletion are outside phase 1 and remain NOT IMPLEMENTED rather than passed.
