# Codex working agreement

- Read `README.md` and all relevant documentation before modifying files.
- Inspect workspace and assets first; never assume Android project or Canva source code exists.
- Use Kotlin, Jetpack Compose, Android Studio, local-first architecture. iOS is deferred.
- Prioritize correctness, user autonomy, privacy, accessibility, and research traceability over visual flourish.
- Maintain separate device-usage, app-owned session, and self-reported outcome data sources.
- Never invent usage statistics, intentions, study findings, successful tests, or device verification.
- Avoid AccessibilityService, blocking, AI coaching, accounts, cloud, and payments in MVP.
- Request Usage Access only when relevant; allow manual mode without it.
- Design for narrow cover and unfolded Fold5 windows and multi-window resizing.
- Preserve Canva visual identity, but map to native Compose components and adaptive navigation.
- Execute work in phases; STOP after phase 1 feasibility and report results unless the user authorizes further implementation.
- For every change: identify affected requirement IDs, tests, limitations, and files changed.
- Do not overwrite references; commit incrementally. Do not upload private device usage data.
- If a reference is inaccessible, state that and rely on supplied local exports; do not claim complete site traversal.
