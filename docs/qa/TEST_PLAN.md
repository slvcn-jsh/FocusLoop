# Verification and quality gates

## Automated
Unit tests for state transitions, single active session, extension history, duration math, reflection skip, aggregation boundary handling, permission-state mapping, and missing vs zero data. Build, lint, tests run where toolchain exists.

## Physical Fold5 (pending)
Usage access grant/revoke; per-app totals vs StayFree; today/yesterday/custom ranges; cover/unfold/split-screen; process death and restart; notification permission denied; battery/idle restrictions; font scaling and TalkBack.

## UX
All screens and navigation reachable; back preserves filters; active session shown on Home; no fake analytics; every button acts; goal selection skippable; manual sessions without permission; deletion confirmation; no shame-based language.

## Reporting
For each test: ID, environment, steps, expected, observed, pass/fail/not-run, evidence and limitations. Distinguish automated pass from device verified.
