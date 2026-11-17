# Native UI device review

Version 0.3-native-ui. Automated results are recorded in [the implementation report](../engineering/NATIVE_UI_RESULTS.md). This checklist is for remaining human/device observations; unchecked items are not passes. Do not upload personal usage screenshots or session text.

| ID | Sequence / expected behavior | Status |
|---|---|---|
| U01 | Launch upgraded app; same current session returns on Home; tap all four tabs and primary controls | Automated install/row preservation and semantic navigation passed; new visual/physical tap confirmation pending |
| U02 | Fold/unfold on Home, Usage, editor and active session; then rotate and split-screen; controls remain reachable and drafts survive | Constrained 320/360/680 dp tests passed; actual transitions pending |
| U03 | Set Android large font; inspect all forms, native date picker, checkpoint sheet and timer; use TalkBack to navigate labels/chart summaries | Compose 2× font fixture passed; system-font/TalkBack pending |
| U04 | Fresh install on a separate test profile: skip goals and Usage Access, start manual session; later edit goals and grant optional permission | Synthetic onboarding/manual flow passed; clean physical profile pending; do not clear the user's app |
| U05 | Change Usage filter, open app detail, Back, then Create intention; filter and app selection remain correct | Synthetic real-navigation test passed; manual review pending |
| U06 | Choose custom dates, including current day; reject future/reversed/>366 days; inspect returned bounds and gaps | Domain/device-query tests and picker-open UI passed; manual selection/input-mode review pending |
| U07 | Compare matched Today/Yesterday/7-day/custom ranges with StayFree; record only discrepancy/coverage conclusions without app lists/durations | Not run |
| U08 | Deny/revoke/regrant Usage Access; totals clear and manual sessions remain; notification denial also leaves session controls usable | Synthetic denied/revoked/unavailable/empty usage passed; actual permission transitions pending |
| U09 | On a separate test record, pause/resume/extend/revise/finish, leave reflection unanswered or skip explicitly; inspect original plan, events and outcome | Synthetic UI and isolated real storage passed; manual review pending |
| U10 | Dirty editor Back/cancel; switch destinations; process restart with editor/filter/active record; verify draft/navigation and timing semantics | Stored user rows survived upgrade; full new navigation/process review pending |
| U11 | Explicitly enable reminders on a test session; allow notifications yourself; verify delivered generic content, dismiss, pause/cancel/resume/extend, idle/battery behavior | Pure eligibility/delay tests passed; real alarm delivery/visibility/cancellation pending |
| U12 | Reboot and force-stop on an intentional test record; no fabricated time/outcome; recovery gap visible, explicit resume, no exact-delivery promise | Synthetic clock/recovery tests passed; actual device sequence pending |
| U13 | Cancel deletion then confirm on disposable records; template deletion retains sessions; history deletion retains templates and prevents legacy resurrection | Isolated storage/UI tests passed; manual review pending; do not delete personal history for this check |
| U14 | Review dependency/target versions, release signing, accessibility/contrast and OEM backup/transfer exclusions before distribution | Not a release gate pass |

Record date/build, steps, expected/observed behavior, pass/fail/not-run and nonprivate evidence for each completed item. The previous basic fold/unfold user confirmation was for the Phase 2 core screen; it is not automatically carried over to this UI.
