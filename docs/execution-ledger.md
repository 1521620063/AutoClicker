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

## 1.1.0 bounded enhancement — 2026-10-08

- User approved the in-chat design for 10ms minimum, configurable press duration, cancellable countdown, mutually-exclusive continuous/count/time stop modes and saved settings; separately approved optional real-time frequency.
- Frequency: current-run successful completion callbacks only, rolling (now-1000, now] window, 500ms UI refresh, default-off persisted toggle; reset at start/stop, no stale callback contribution. Fixed-height overlay statistics reserve avoids runtime window growth.
- Added regression tests; observed failing compilable feature-disabled behavior and frequency stubs before final GREEN. Fresh build: 52 core tests, 0 failures/errors; app unit tests NO-SOURCE; lint 0 errors / 12 warnings. Full APK builds with version 1.1.0 (2); v2 signature and package metadata verified. adb reports no device; phone acceptance remains unperformed.
- No new permissions or dependencies; no automatic restart, overlap or catch-up dispatch. Config mutations remain disabled throughout countdown and execution. No commit/push performed for this enhancement without a new request.

- Review P2 reproduced: a completion callback could be counted after deadline before the delayed timer ran. Added failing regression for exact/late deadline, then guarded current-run callback before counting. 52 core tests now pass; final full build rerun with all 50 tasks executed, signing rechecked.
- Focused independent reviewer confirmed the deadline guard and regression address P2 without breaking old-generation isolation; no further proven source issue reported. Runtime acceptance remains pending.

## 2026-10-08 界面改版 1.2.0

用户确认雾白/翡翠绿方向。完成主页面分组、品牌/状态、主要操作前置、选中快捷项、频率开关、统一按钮状态和悬浮控制器改版；引擎未改。52 项核心测试、3 项源码 UI 约束通过；全量构建、APK 签名通过；lint 0 错误/13 警告。无连接设备，视觉验收待完成。未提交或推送。
