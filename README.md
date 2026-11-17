# FocusLoop — native Android

Research-informed, native Android digital wellbeing application. The supplied research and visual references are preserved in Git. `android/` contains the **0.3 native interface**, built on the verified Room core. See the [UI implementation blueprint](docs/design/UI_IMPLEMENTATION_BLUEPRINT.md), [current UI/device results](docs/engineering/NATIVE_UI_RESULTS.md), [audit](docs/engineering/PHASE_0_AUDIT.md), [historical feasibility findings](docs/engineering/PHASE_1_RESULTS.md), and [historical core results](docs/engineering/PHASE_2_RESULTS.md).

## Read first
1. `AGENTS.md` — permanent Codex rules.
2. `agent/CODEX_MASTER_PROMPT.md` — kickoff instructions and staged execution.
3. `docs/product/PRODUCT_SPEC.md` and `docs/product/SCREEN_FLOW.md`.
4. `docs/research/EVIDENCE_REGISTER.md` and `docs/research/TRACEABILITY.md`.
5. `docs/design/CANVA_HANDOFF.md` and screenshots in `design/canva_exports/`.
6. `docs/engineering/FEASIBILITY.md` and `docs/qa/TEST_PLAN.md`.

## Reference hierarchy
Verified Android/platform behavior and safety > approved product requirements > observed Canva design > tentative ideas. Canva is a *visual* reference, not evidence that a function exists. Do not copy Canva's web implementation as Android code.

## Source assets
- Published Canva reference: https://focusloopjosh.my.canva.site/
- Local exported visual reference: `design/canva_exports/` (some images are blank exports).
- Earlier wireframe: `design/wireframes/FocusLoop_Canva_Wireframe_Reference_v1.pdf`.

## Status
The user authorized the complete native UI/flow sequence after the feasibility and core phases. Home, Usage, Focus, Settings, optional onboarding, intentions, session checkpoints, reflection, history and privacy controls are implemented. Local Canva exports guide the visual identity; live Canva interaction traversal remains unavailable. Android usage estimates retain explicit coverage limits. The connected device is an SM-F946B Fold5 running Android 15/API 35. See the current report for test evidence, upgrade preservation and remaining manual checks. This development build is not a production-release or research-effectiveness claim.
