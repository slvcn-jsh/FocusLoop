# Native UI and flow implementation blueprint

Authorized 2026-11-17 by the user: "now proceed with all this plans and implement them in sequence". This advances the complete interface work after verified core Phase 2; no further implementation gate is inferred from the old feasibility-only kickoff. Requirements FR-01 through FR-14 remain authoritative.

Sequence: (1) map screens/states, (2) shared visual components/adaptive shell, (3) complete flows, (4) connect real data and local preferences, (5) test and visually inspect synthetic UI screenshots, then update the connected Fold5 without deleting its session. Preserve original references and approved specifications.

Reference check: published Canva URL was retried and remains inaccessible in the web tool; in-app browser execution is unavailable. Local exports remain primary observed visual evidence. Phase 0's full inventory and PDF review remain valid. Rechecked 1.png, design-system board and editor/active/reflection exports. 9.png/10.png are blank; do not claim live interaction traversal. Standard native components and system typography replace unavailable editable font/icon/motion assets.

## Screen map and acceptance

| Family | Screens and transitions | State/requirement coverage |
|---|---|---|
| First launch | Welcome -> optional goals -> privacy -> optional Usage Access -> Home | FR-01/02/03/14; skip goals and permission; goals editable later. Existing core installs retain their session and enter Home; onboarding remains reviewable from About |
| Home | Actual today device summary or unavailable; active-session return first; quick-start Study/Messages/Social; saved plans; most-used apps -> Usage/detail | FR-03/04/06/07/10/12; no illustrative metrics in the running app; each datum has its source |
| Usage | Today/Yesterday/7/14/28/custom -> raw Android bucket summary, missing-aware daily chart, app ranking -> app detail -> create intention with optional app preselected | FR-04/05/10; long/custom windows are marked provisional and coverage unknown, not exact elapsed/calendar totals. Missing days are gaps, not fabricated zeroes. Back preserves selected filter |
| Focus | Library -> editor -> one active session -> optional checkpoint -> finish -> optional reflection -> summary -> history/detail or Home | FR-06/07/08/09/14; editor optional app/reminder/template; pause/resume/extensions/cancel; original plan never overwritten; revise means append a labelled plan-revision event, retaining the original intention and target history |
| Settings | Goals; Usage Access; notifications; session defaults; data/privacy; About/research; confirmed history/template deletion | FR-01/02/03/11/13; real persisted settings; clear deletion boundaries and result; no cloud/account/blocking controls |
| Adaptive shell | Four compact bottom destinations; native rail for expanded windows; available-width paired content; scroll and stacking at large text/narrow widths | FR-12; use native Back, insets, keyboard avoidance and >=48 dp targets; no fixed phone frames |

## Visual/component system

Exact observed palette: #0F7C88 primary, #16343A ink, #5E7074 muted, #FFFFFF surface, #F7FAFB canvas. Mint selected/navigation surface, light borders, medium rounded cards, quiet hierarchy. 8 dp spacing rhythm; body 12/14/16 sp, section 20 sp, screen 30 sp. Shared app bar, provenance badge, metric/card, action row, application avatar, empty/permission/error state, adaptive paired panels, selection controls, chart and confirmation dialog. Material 3 is mapped to this palette; no webview or screenshot-as-screen implementation.

## Data and lifecycle rules

- Device usage, app-owned intentions/timing/events and self-reported reflection remain distinct models and labels. Home always uses today, independently of the Usage filter. Package labels/icons are resolved locally when visible, with package fallback and no broad installed-app permission.
- Android daily aggregate buckets can exceed requested bounds; show that caveat and requested/returned bounds. Charts show only supplied raw buckets and explicit gaps. Do not invent app-opening/session counts or screen-interactive totals. Date-range support is inspection of available bucket data, not proof of exact historical accuracy. Custom ranges are limited to 366 days to bound work and reject future/reversed dates.
- Local onboarding/goals/default preferences do not change the Room session schema or imported record. Form/navigation state uses saved state; preferences and usage labels never become inferred outcomes.
- Editor changes to a running plan are appended as revision events without overwriting the original title/plan. Store new text in event metadata using a schema-safe additive event-kind encoding already supported by the existing event table; existing event rows remain unchanged. Old software may not decode new event kinds, so forward updates retain data but downgrade is unsupported once new events exist.
- Optional session reminders reuse inexact AlarmManager scheduling, are dismissible and never finish sessions. Explicitly opted-in sessions can reschedule on pause/resume/extend; settings can disable reminders. Permission/channel denied states remain visible. Reboot/force-stop/idle can lose or delay delivery; no exact-alarm or blocking service is added.
- Session ending and reflection submission are separate actions. Back/leave-unanswered do not manufacture success; explicit skip stays distinct. History deletion requires confirmation and clears the retained legacy copy without resurrection; saved intentions have a separate confirmed deletion action.

## Verification

Reuse current core/domain tests; add only meaningful range/gap/revision/settings/reminder-state proofs and navigation/UI tests. UI tests inject synthetic states/actions into the real app shell; never mutate the live user's database or grant/revoke permissions silently. Capture synthetic Home/Usage/Focus/Settings and important flows at compact, Fold5-like and large-text sizes; inspect rendered screenshots. Run build/lint/unit and physical Android suites. Install in place, compare existing session/legacy source without exporting private content, and report exact results plus unrun OEM/reboot/accessibility/delivery checks. Commit incrementally; keep private snapshots ignored.
