# Canva-to-Compose handoff

**Published prototype:** https://focusloopjosh.my.canva.site/

**Local exports:** `design/canva_exports/` are user-supplied screenshots of a multi-page Canva prototype, not source code. `9.png` and `10.png` appeared blank in earlier review; verify visually before using. `FocusLoop Design System.png` contains a preliminary design system. `design/wireframes/` contains an earlier wireframe PDF.

**Access protocol:** Attempt to inspect the published site through available browser tools if feasible. If site content is canvas-rendered or inaccessible, do not claim full inspection. Use screenshots and ask user for updated exports when designs change. Record which screens are confirmed and any unresolved inconsistencies.

**Visual goals:** Calm teal/light neutral aesthetic; compact information-rich layouts; readable hierarchy; subtle cards; clear navigation; no huge hero or long scrolling; native Compose interactions, not a webview. Use Material 3 and accessible touch targets, dynamic window-size layouts, system insets and text scaling.

**Screen-to-code mapping:** onboarding (Welcome/Goals/Privacy/Permission), Home (normal/active/unavailable), Usage (filters/chart/app list/custom range/app details), Focus (library/editor/active/checkpoint/reflection/summary/history), Settings (goals/permissions/reminders/data deletion). Include empty, denied, revoked, error, paused, recovered states.

**Visual fidelity is conditional:** match layout, colors, typography, hierarchy, component patterns and transitions where they do not conflict with verified Android capability or requirements. Log any deviations with reason.

**Design deliverables:** screenshot inventory, screen map, design token extraction, reusable component inventory, navigation graph, state matrix, foldable adaptations and unresolved visual questions.
