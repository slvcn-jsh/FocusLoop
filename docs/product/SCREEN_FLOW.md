# Screen inventory and navigation contract

## Top-level destinations
Home | Usage | Focus | Settings. Bottom navigation for top-level compact layouts; adapt for expanded widths. Do not make a fifth primary tab.

## Onboarding
Launch -> Welcome -> optional multi-select Personal Goals -> Privacy -> optional Usage Access -> Home. Skip goal selection and permission. Never block core functionality on permission.

## Home
No active session: daily summary (or unavailable), quick-start saved intention, most-used apps.
Active session: restore/return card takes precedence over creating a second session.
Permission denied/revoked: informative call to action, no fake usage numbers.

## Usage
Usage overview -> filter (Today/Yesterday/7/14/28/custom) -> select app -> App Detail -> back preserves filter. App Detail -> Create Intention preselects app. Missing history, partial coverage, and revoked permission are explicit states. Device foreground time != screen interactive time.

## Focus
Focus overview -> saved templates / new intention -> editor (title, optional app, duration, reminder) -> active session -> checkpoint (finish/extend/revise/dismiss) -> reflection (optional) -> summary -> Focus history/detail or Home. Original duration never overwritten; extension events retained. Session completion distinct from reflection submission.

## Settings
Settings -> My Goals / Usage Access / Notifications / Session Defaults / Data & Privacy / About. Delete history -> confirmation -> result.

## Required edge cases
First launch, no data, loading, permission denied/revoked, incomplete usage coverage, no selected app, invalid duration, active/paused/recovered session, app process death, device reboot, reminder late/denied, skipped reflection, canceled session, deletion confirmation, fold/unfold and multi-window resize.

## Navigation invariants
No dead-end buttons; native back semantics; preserve filter state; only one active session; user can exit app without losing saved session; never imply FocusLoop knows what user did inside another app.
