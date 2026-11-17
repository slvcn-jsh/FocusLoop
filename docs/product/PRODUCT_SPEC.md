# FocusLoop product specification — baseline v1.2

**Purpose:** Help users understand app usage and manage unwanted continuation relative to their own intentions, not simply reduce all screen time.

**Audience:** Students and general Android users. **Platform:** native Android first (Kotlin + Compose), future iOS requires separate feasibility and Apple entitlements.

**Interaction:** Hybrid: voluntary manual intentions + optional device usage awareness. No forced blocking or assumptions about the purpose of a foreground app.

## Product pillars
1. Awareness: rich per-app foreground usage, daily/weekly averages, history and filters (subject to Android data coverage).
2. Personal goals: optional multi-select onboarding (reduce distractions, understand usage, focus on study/work, reduce unwanted scrolling, healthier habits, other); editable later; not session intentions.
3. Intentions: reusable templates, optional app target, duration and reminders.
4. Reflection: optional, neutral, low-effort checkpoints and post-session outcomes.
5. Voluntary commitments: extend, revise, finish, pause, override without shame or inescapable lockout.
6. Improvement: compare recorded intentions, session durations, extensions and outcomes with separately sourced device usage.

## Scope
**MVP:** onboarding, goals, privacy, optional usage permission, home, usage overview, app details, intention library/editor, one active session, pause/resume/extend/finish, reflection, summary/history, settings, deletion, permission/error/empty states, Fold5 adaptive layout.
**Conditional:** Android per-app analytics and date ranges only after feasibility testing; fallback to manual mode when denied/unavailable. Accurate custom ranges may be limited.
**Deferred:** real-time app interception/blocking, AccessibilityService, AI coaching, gamification, cloud, accounts, social features, iOS.

## Requirements
- FR-01 onboarding and optional personal goals; skip/edit later.
- FR-02 local-first without account; clear privacy and data deletion.
- FR-03 optional Usage Access, robust denied/revoked/unavailable states.
- FR-04 device per-app foreground usage, ranking, history and data coverage indicators where supported.
- FR-05 today/yesterday/7/14/28-day and custom date range subject to data reliability.
- FR-06 reusable intentions with title, optional target app, planned duration, reminder choice.
- FR-07 single active session; start/pause/resume/finish/cancel/recover.
- FR-08 preserve original plan and every extension as events.
- FR-09 optional reflection: completed/partial/not completed/skipped; skipped != failure.
- FR-10 separate device observations, session data, self-reported outcomes in UI and storage.
- FR-11 optional dismissible notifications; no guarantee of exact delivery.
- FR-12 adaptive narrow/expanded/multi-window UI and accessible touch targets.
- FR-13 settings, preferences, local history deletion and appropriate confirmations.
- FR-14 no punitive language, no automatic inference of user intention, no forced lockouts.

## Quality requirements
Data integrity, explicit coverage, no fabricated zeroes, offline core, minimized permissions, accessible labels/contrast/scaling, deterministic state transitions, battery-aware scheduling, tested recovery, and maintainable separation of UI/domain/platform.
