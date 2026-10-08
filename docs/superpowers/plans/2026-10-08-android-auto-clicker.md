# 安卓固定位置连点器 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付中文、无需 Root 的安卓单点持续点击应用及构建成功的 debug APK。
**Architecture:** 原生 Kotlin Activity 提供授权说明及间隔设置；AccessibilityService 管理悬浮窗口和手势。独立的可注入时钟与手势接口的调度器负责串行点击、取消及迟到回调隔离。
**Tech Stack:** Kotlin、Android SDK、Gradle Wrapper、JUnit；原生 Views，避免额外 UI 框架。
**Spec:** C:\project\AutoClicker\docs\superpowers\specs\2026-10-08-android-auto-clicker-design.md

## Global Constraints
- minSdk 24；中文界面；单点；不联网、不广告、不账号、不 Root。
- 默认间隔 100 毫秒；合法整数范围 50–60000；短点击持续 20 毫秒。
- 用户手动启用无障碍；TYPE_ACCESSIBILITY_OVERLAY；不读取节点、文本或截图。
- 不保存坐标、不恢复运行；锁屏、关闭屏幕、旋转、服务断开、手势拒绝或取消均停止。
- 手势不重叠、不补发积压；停止后无新手势，已分发的手势可能完成。
- 项目根目录为 C:\project\AutoClicker。以下文件均相对此根目录；命令在此执行。
- 当前不是 Git 仓库；不擅自初始化、提交或建立 worktree。各任务以验证和审查作为检查点。

## Review Focus
1. 极长数字、空白、小数及溢出输入不得引起崩溃或启动。
2. 停止后立即重新开始，旧手势回调不得驱动新循环或造成重叠。
3. 屏幕边缘、状态栏与导航栏坐标必须对应实际下层点击位置。
4. 目标与控制条相交，必须拒绝开始而不是点击自身按钮。
5. 窗口创建部分失败、服务重连时不得残留窗口或自动执行。

---

## Task 1：可构建工程与设置校验
**Files:** settings.gradle.kts、build.gradle.kts、gradle.properties、gradlew、gradlew.bat、gradle/wrapper/*、.gitignore、app/build.gradle.kts、app/src/main/AndroidManifest.xml、app/src/main/res/values/strings.xml、app/src/main/res/values/styles.xml、app/src/main/java/com/example/autoclicker/settings/IntervalValidator.kt、SettingsRepository.kt、app/src/test/java/com/example/autoclicker/settings/IntervalValidatorTest.kt。
**Interfaces:** IntervalValidator.parse(text: String): Long?；SettingsRepository.intervalMs: Long（默认 100）。
- [ ] 检查其他 SDK 路径和官方兼容性文档；确定固定 SDK/AGP/Kotlin/Gradle 版本并记录版本表。需要下载时仅使用官方工具源；SDK 安装先展示许可证，要求用户接受，不代替用户接受未授权条款。
- [x] 配置工程、Wrapper、minSdk 24 和 JUnit；local.properties 仅记录本机 SDK，加入忽略列表。
- [x] 编写 parseAcceptsBoundsAndDefault：50、100、60000 返回对应 Long；parseRejectsInvalid：空串、空白、49、60001、-1、1.5、字母、超长数字返回 null；运行 `.\gradlew.bat :app:testDebugUnitTest`，确认因未实现校验而失败。
- [x] 实现安全整数校验及 SharedPreferences 间隔读写；存储损坏或非法值回退 100。
- [x] 重跑测试确认 PASS；检查 manifest 无 INTERNET 权限。工具缺失导致未运行时如实记录，不假报测试通过。

## Task 2：可测试的串行点击调度器
**Files:** app/src/main/java/com/example/autoclicker/click/ClickScheduler.kt、ClickPoint.kt、app/src/test/java/com/example/autoclicker/click/ClickSchedulerTest.kt。
**Interfaces:** ClickPoint(x: Float, y: Float)；SchedulerClock.nowMs(): Long、postDelayed(delayMs: Long, action: () -> Unit)、cancelPending(): Unit；GestureDriver.dispatch(point: ClickPoint, durationMs: Long, complete: (Boolean) -> Unit): Boolean；ClickScheduler.start(point: ClickPoint, intervalMs: Long): Boolean、stop(): Unit、isRunning: Boolean。
- [x] 用 fake clock/driver 编写不重叠、重复开始拒绝、20 毫秒手势、间隔下界及慢回调不补发测试，运行筛选测试确认 RED。
- [x] 编写 stopIgnoresLateCallback、restartWaitsForOldGestureCompletion、dispatchRejectedStops、cancelledGestureStops、stopIsIdempotent 测试；断言旧回调不能派发新手势、立即重启不会重叠。
- [x] 实现调度器：保留尚未返回的手势状态；运行代次隔离旧回调；以最近实际分发时间计算下次延迟；回调返回之前禁止派发。拒绝和取消使执行状态停止，并通过 onStopped(reason: String) 通知服务。
- [x] 运行 `.\gradlew.bat :app:testDebugUnitTest --tests "*ClickSchedulerTest"` 确认 PASS。

## Task 3：悬浮窗口及定位
**Files:** app/src/main/java/com/example/autoclicker/overlay/OverlayController.kt、OverlayGeometry.kt、app/src/test/java/com/example/autoclicker/overlay/OverlayGeometryTest.kt。
**Interfaces:** OverlayController.show(): Unit、hideTarget(): Unit、restoreTarget(): Unit、close(): Unit；回调 onStart(point: ClickPoint)、onStop()、onClose()；OverlayGeometry.clamp(point, screenBounds)、overlapsTarget(target, panelBounds): Boolean。实际屏幕坐标使用 getLocationOnScreen，不混用窗口偏移和触摸局部坐标。
- [x] 测试边缘坐标限制、负坐标、大坐标、控制条与目标重叠及不相交；运行对应单元测试确认 RED。
- [x] 实现几何函数并运行测试确认 PASS。
- [x] 实现可拖动控制条和靶心；拖动靶心才标为已定位；开始前检查定位与控制条遮挡。运行中隐藏靶心且锁定设置，停止后恢复；close 幂等清理全部窗口。
- [x] 加入可注入窗口操作接口，测试第二个窗口创建失败时清理第一个窗口，以及重复关闭安全；运行全部单元测试确认 PASS。

## Task 4：服务与中文设置页面集成
**Files:** app/src/main/java/com/example/autoclicker/MainActivity.kt、service/AutoClickAccessibilityService.kt、service/ServiceState.kt、app/src/main/res/xml/accessibility_service_config.xml、app/src/main/res/values/strings.xml、app/src/main/AndroidManifest.xml、app/src/test/java/com/example/autoclicker/service/ServiceStateTest.kt。
**Interfaces:** ServiceState 发布 connected/running 状态；服务 showController(): Boolean、stopClicking(): Unit。MainActivity 不持有失效服务引用；服务实例仅在连接期对应用内部可用。
- [x] 写状态测试：服务断开重置运行状态、重连保持停止且不显示控制器；非法间隔不调用 start；亮屏但锁定时拒绝开始；运行中拒绝更改设置。确认 RED 后实现状态规则。
- [x] 声明 BIND_ACCESSIBILITY_SERVICE 与 canPerformGestures，仅请求必需能力，使用 dispatchGesture 适配 GestureDriver、Handler 适配 SchedulerClock。
- [x] 注册并在销毁时解除屏幕关闭监听；配置变化时停止并关闭窗口；解绑/销毁时清理调度和窗口；开始前检查 PowerManager 与 KeyguardManager。
- [x] 实现中文页面：服务状态、权限用途说明、进入系统无障碍设置、间隔输入及保存、显示控制器、使用说明。保存间隔不保存坐标；服务未连接时展示授权提示。
- [ ] 将失败原因显示为中文状态或提示；系统取消/拒绝不自动重试；执行 `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug`，确认通过或修复实际诊断。

## Task 5：构建、设备验证与交付
**Files:** README.md、docs/verification.md；产物 app/build/outputs/apk/debug/app-debug.apk。
- [x] 执行 `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`；记录命令、退出码、测试结果与工具版本。检查 APK 实际存在，并输出大小与 SHA256。
- [x] 运行 `adb devices` 检查设备；有已授权设备时安装 debug APK，无设备则明确标记设备测试未执行。
- [ ] 设备验收：开启权限、拖动靶心、测试页面计数、停止/关闭、控制条遮挡拒绝、目标靠近屏幕边缘、锁屏、旋转、关闭服务和重连；检查下层应用确实收到点击且无残留窗口。任何未验证的情况必须列出。
- [x] README 写明构建环境、安装命令、权限用途、使用步骤、间隔非实时保证、系统限制及停止语义；docs/verification.md 只记录真实结果。
- [x] 对照设计审查全部文件；最终给出源码与文档绝对路径、APK 路径（仅构建成功时）、已完成验证和剩余限制。不宣称未经设备验证的功能已真机通过。

## 执行状态 — 2026-10-08（更新）

用户同意标准 SDK 许可后所需 SDK 已安装。完整构建、lint、32 项核心测试成功；app 测试为 NO-SOURCE。APK 签名与元数据已检查，产物实际存在。adb devices 设备列表为空，设备验收清单仍未完成；不宣称真机通过。Lint 8 项警告原样保留并记录。详见 docs/verification.md 与 docs/execution-ledger.md。

## 后续 1.1.0 增量（对话批准）

低间隔、按下时长、倒计时、次数/时长自动停止、保存配置和可选实测频率已实现。新增回归后 52 项核心测试通过，完整 APK/lint 构建通过（0 错误，12 警告）。独立审查的截止回调计数 P2 已复现、修复并复核。产物版本 1.1.0 (2)，APK 签名已验证。设备列表为空，安卓运行时验收仍未完成。本轮增量尚未提交推送。详情见 docs/verification.md 当前章节。
