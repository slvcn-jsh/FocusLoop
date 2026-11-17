# Technical feasibility — phase 1 only

Target: Samsung Galaxy Z Fold5 (actual Android version must be measured, not guessed). Use Android Studio + Kotlin + Compose. Codex CLI can operate from VS Code terminal; Android Studio is the build/debug/physical-device environment.

1. Initialize minimal Android Studio project in `android/` only after inspecting workspace; use stable compatible Gradle/AGP/Kotlin/Compose versions available locally.
2. Test `UsageStatsManager` with `PACKAGE_USAGE_STATS` and explicit special access via Settings. Check permission using AppOps; denied/revoked must not break manual functionality.
3. Query recent per-app foreground usage; test today/yesterday/7 days, document aggregation boundary effects and history coverage. Event queries are limited; never promise exact arbitrary date history.
4. Test Fold5 cover/unfold/multi-window; app foreground sums may not equal total screen-on time.
5. Test local session persistence and recovery after process death; distinguish elapsed and active time; handle reboot/clock changes without fabricating records.
6. Test notification permission and reminder timing; do not assume exact alarms or background reliability.
7. Document API/permission behavior, results, test device details, discrepancies versus StayFree, and privacy/battery implications.

**Stop gate:** no production app feature expansion until feasibility report reviewed. Never claim physical device tests passed without user-provided results.
