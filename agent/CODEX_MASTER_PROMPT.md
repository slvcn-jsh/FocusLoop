# FocusLoop — Codex CLI master kickoff

You are the lead Android implementation agent for FocusLoop. Work inside this repository; read `README.md`, `AGENTS.md`, the research register, traceability matrix, product spec, screen flow, Canva handoff, feasibility plan and QA plan **before coding**.

## Mission
Create a reliable native Android application faithful to the user's current multi-page Canva design, while respecting research-supported product boundaries and Android platform realities. Use Kotlin, Jetpack Compose, Android Studio, local-first design; Samsung Galaxy Z Fold5 is the primary device. iOS is a later project.

## Reference inspection
1. Inventory files and identify any existing Android project; never overwrite unrelated code.
2. Attempt to inspect https://focusloopjosh.my.canva.site/ using any browsing capability available to you. Follow all screens and interactions if possible. If inaccessible, report the limitation and inspect **every nonblank local export** in `design/canva_exports/` and the wireframe PDF. Do not claim complete interaction coverage from static screenshots.
3. Produce a screen inventory, interaction/navigation graph, visual token list, and gap list. Separate observed Canva visuals from inferred behavior.
4. Compare each proposed screen/control to `docs/product/PRODUCT_SPEC.md` and `docs/research/TRACEABILITY.md`; flag conflicts rather than inventing capabilities.

## Architecture
- Kotlin, Compose Material 3, ViewModels, coroutines/Flow, Room/SQLite for app-owned data; platform adapters for UsageStatsManager and notifications.
- Separate device usage, intention/session records, and self-reported outcomes in models and presentation.
- One active session; original planned duration immutable; append extension events; reflection optional.
- Local-first, no account, no cloud, no AI coaching, no AccessibilityService or aggressive blocking in MVP.
- Permissions are optional and revocable; show unavailable data rather than fabricated zero.
- Adaptive layouts for cover/unfold and window resizing; accessible text scaling, semantics and touch targets.

## Staged work, with approval gates
**Phase 0 — Audit:** inspect sources and local environment; output a brief verified screen map, evidence/requirements crosswalk, risks, and implementation plan. Do not silently rewrite specs.
**Phase 1 — Feasibility spike (authorized):** initialize a minimal Android Studio project under `android/` if absent; implement Usage Access state and a diagnostic per-app usage view, plus a minimal persistent session timing experiment. Add tests and report actual build/lint/test outcomes. Provide precise Fold5 manual test steps. Do not implement full production UI yet.
**STOP after Phase 1.** Wait for explicit user authorization before Phase 2 onward.
**Phase 2 — Core domain:** intentions, sessions, event history, reflection and Room, with tests.
**Phase 3 — Analytics:** accurate available stats, historical filters, coverage indicators, per-app detail, provenance.
**Phase 4 — Canva UI:** implement approved Home/Usage/Focus/Settings and onboarding with responsive Compose components, matching screenshots; verify navigation states.
**Phase 5 — Integration/QA:** notifications, recovery, accessibility, privacy, Fold5 validation, release readiness.

## Deliverables every phase
Files changed, design/requirement IDs, commands and results, assumptions, screenshots where possible, unresolved issues, known Android API limitations, and next-step recommendation. Never report unrun tests as passed or pretend to have accessed the user's physical device.

## Immediate action
Perform Phase 0 and Phase 1 only. Respect time, dependencies, existing files, and no-boilerplate-in-chat preference. Write code in the repository rather than presenting snippets. Provide a concise final report and stop.
