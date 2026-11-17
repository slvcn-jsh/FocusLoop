# FocusLoop — Android Development Starter

Research-backed, native Android digital wellbeing application. The supplied documentation and visual-reference starter is preserved in Git. `android/` contains the **Phase 2 core prototype**, following the initial feasibility spike. See the [audit](docs/engineering/PHASE_0_AUDIT.md), [historical Phase 1 findings](docs/engineering/PHASE_1_RESULTS.md), [Phase 2 scope](docs/engineering/PHASE_2_PLAN.md), and [current device/core results](docs/engineering/PHASE_2_RESULTS.md).

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
The user authorized continuation after Phase 1 and connected an SM-F946B Fold5 running Android 15/API 35. The next core slice adds saved intentions, durable Room session/event history, and separate optional reflection while preserving the existing Phase 1 record. Full Canva production UI and later analytics/integration phases remain separate work. See the current results for actual test outcomes and remaining physical checks; prototype code is not a production-readiness claim.
