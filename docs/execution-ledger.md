# Execution ledger — 2026-10-08

- Design and plan approved; direct implementation selected.
- Ruling: pure logic and tests live in an SDK-independent `core` module — Android SDK is absent and this permits meaningful tests without accepting SDK licenses — cost if wrong: module dependency restructuring.
- Ruling: keep ledger in docs, no worktree or commits — directory is not a Git repository — cost if wrong: no Git rollback history.
- Tool pins: Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, compile/target SDK 35, min SDK 24, Java/Kotlin bytecode 17 (build JDK 21). Official documentation access attempted; network verification currently unavailable, compatibility remains subject to an actual build.

- Core RED: 26 tests, 19 failures against empty implementation. GREEN: :core:test BUILD SUCCESSFUL (26 tests); all expected failures resolved.

- Ruling: add optional `tools/android-check` source/API compilation while SDK installation awaits license consent — allows type checking of real app sources — cost if wrong: this does not verify resources, packaging or runtime and must not be described as an APK build.
- Ruling: dragging the running panel stops clicking first — prevents moving it over the target and clicking its own controls — cost if wrong: user must explicitly restart after moving the panel.
- Review fixes: target center rather than entire marker constrained to usable bounds; event-triggered readiness checks plus independent 100ms SafetyMonitor. Six regression tests observed RED; 32 tests observed GREEN. Edge-window behavior and sub-100ms unobserved lock transitions remain device-validation risks, not claimed solved at OS level.
- Final fresh verification: `:core:test :tools:android-check:compileKotlin --rerun-tasks`, exit 0; 32 tests, zero failures/errors. XML/permission checks passed. Gradle archive matches official SHA256.
- Full APK/lint attempt, exit 1: SDK license not accepted; platform 35 and build-tools 35.0.0 cannot install. No APK exists. License text displayed and saved; no license accepted. Awaiting user's consent before installation.
- Deferred minors: none reported by the independent reviewer.

- Focused reviewer follow-up: both original source defects addressed; OEM edge positioning remains unverified, and lock/unlock transitions entirely between readiness observations remain a documented sampling limitation. No additional proven source defect reported.

- 2026-10-08: user explicitly consented to standard SDK license. Installed only platform 35, build-tools 35.0.0 and platform-tools; only android-sdk-license recorded.
- Full fresh build exit 0: 50 tasks executed; 32 core tests, 0 failures/errors; app tests NO-SOURCE; lint 0 errors / 8 warnings (retained and documented). Debug APK 871327 bytes; v2 signature verified, package metadata checked. adb devices exit 0, empty device list; runtime acceptance not performed.
