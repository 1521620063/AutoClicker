# 验证记录 — 2026-10-08

## 当前结果概览（1.2.0）

| 检查 | 实际结果 |
|---|---|
| 核心 JUnit 测试 | 52 项，0 失败，0 错误；已强制重新运行 |
| 安卓资源 / Kotlin / DEX 编译 | 当前完整 APK 构建通过；旧 API-only 检查为历史辅助证据 |
| XML 结构/权限静态检查 | 5 个 XML 格式正确；manifest 无 uses-permission；不启用界面内容读取 |
| Gradle 下载校验 | 官方发行包 SHA256 匹配，已固定在 Wrapper 配置 |
| Android lint | 完成：0 errors，13 warnings；未隐藏警告 |
| 完整 debug APK | 1.2.0 (3) 构建成功；APK v2 签名验证通过 |
| 真机/模拟器验收 | adb devices 已执行，设备列表为空；设备验收未执行 |

## 1.0.0 自动化测试明细（历史）

- `ClickSchedulerTest`：12 项，失败 0，错误 0。
- `IntervalValidatorTest`：3 项，失败 0，错误 0。
- `OverlayGeometryTest`：5 项，失败 0，错误 0。
- `SafetyMonitorTest`：4 项，失败 0，错误 0。
- `ServiceStateTest`：6 项，失败 0，错误 0。
- `WindowGroupTest`：2 项，失败 0，错误 0。

## 1.0.0 命令与证据（历史）

命令在 `C:\project\AutoClicker` 执行。本机 Gradle 8.11.1 解压至 `.tools/gradle-8.11.1`，使用本机系统代理的临时 JVM 参数；不修改用户全局配置。

```powershell
.\.tools\gradle-8.11.1\bin\gradle.bat -PcoreOnly -PandroidCheck :core:test :tools:android-check:compileKotlin --rerun-tasks --console=plain
```

退出码 0，`BUILD SUCCESSFUL`，5 个任务执行。原始输出：`.tools/final-verification.log`；JUnit XML：`core/build/test-results/test/`。

```powershell
.\.tools\gradle-8.11.1\bin\gradle.bat :core:test :app:lintDebug :app:assembleDebug --console=plain
```

退出码 1。实际错误：`Failed to install ... as some licences have not been accepted`；缺失 `build-tools;35.0.0`、`platforms;android-35`。原始输出：`.tools/final-apk-attempt.log`。
SDK 工具已下载；许可仅展示并保存，没有代替用户接受。适用标准许可文本：`docs/android-sdk-license.txt`；全部展示记录：`docs/android-sdk-license-review.txt`。

## RED → GREEN 与代码审查

- 首批 26 项测试在空实现下 19 项失败；实现后全部通过。
- 独立审查发现靶心窗口全可见约束导致屏幕边缘不能定位，以及长点击间隔下不能及时观察交互式锁屏。
- 新增 6 项回归测试，在缺失修复时全部失败；修复后 32 项全部通过。
- 靶心改为约束中心位置，允许图形部分延伸到可用边缘外；控制条仍全可见。
- 增加独立 100 毫秒运行期安全检查和窗口状态事件触发的设备就绪检查。只读取锁屏/亮屏状态，不读取事件内容；观察到不安全状态后锁定为停止，解锁不恢复。
- 编译检查发现系统服务返回值可空；采用窗口管理器显式检查和锁屏/电源服务缺失时拒绝执行，之后编译通过。

## 未验证的设备行为与限制

API 编译检查使用 `tools/android-check`、Robolectric Android API jar 和 R 占位类。它不构建资源，不生成 DEX 或 APK，不验证 manifest 在设备上的绑定，也不替代 Android lint。

以下尚需真实设备或模拟器：授权/受限设置流程、悬浮窗口显示、真实屏幕坐标（特别是屏幕边缘、状态栏、导航栏和刘海）、靶心隐藏后的下层应用点击、停止/关闭、系统取消、锁屏、旋转、服务禁用及重新连接。

安全检查不是硬实时保证：主线程可能被系统延迟，极短且未被事件或轮询观察到的锁屏转换仍需设备验证。每次手势分发前另行检查设备就绪状态。没有声称系统级瞬时停止或所有厂商兼容。

## 1.0.0 完整构建与交付 — 2026-10-08（历史；旧 APK 已被新构建替换）

用户明确同意适用标准 Android SDK 许可后，仅安装 Platform 35、Build Tools 35.0.0、Platform Tools。许可证目录仅有 android-sdk-license；未接受其他产品许可。历史失败记录保留如上，已不再是当前阻碍。

实际执行：`:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain`。退出码 0，BUILD SUCCESSFUL，50 个任务执行。日志：`.tools/apk-verification.log`。32 项核心测试，0 失败、0 错误；app 单元测试任务为 NO-SOURCE，不能计为额外测试通过。

Lint 报告：`app/build/reports/lint-results-debug.txt`，0 errors、8 warnings：旧系统状态栏内部尺寸资源及反射 API（2）、API31 才生效的属性（1）、构建插件版本提示（1）、备份规则配置提示（1）、中文 UI 数字/字符串本地化（2）、绝对屏幕坐标采用 LEFT 的 RTL 提示（1）。没有压制这些警告；旧系统状态栏/边缘定位仍需设备验收。

- APK：`C:/project/AutoClicker/app/build/outputs/apk/debug/app-debug.apk`
- 大小：871327 字节。
- SHA256：`bc8316cd2d7861987a30289819cd5fc933b6512e3253149f7c6e5d504b9d5afa`。
- apksigner verify --verbose：退出码 0，Verifies，v2 签名通过；开发调试签名。
- aapt dump badging：包名 com.example.autoclicker，版本 1.0.0 (1)，minSdk 24，targetSdk 35，debuggable。
- adb devices：退出码 0，设备列表为空；未安装到设备，未进行授权、窗口、真实点击或停止行为验收。

## 1.1.0 当前构建与验证 — 2026-10-08

用户批准低间隔/运行配置方案及实时频率方案后实施；没有新增权限或依赖。

- 间隔下限 10 毫秒，按下时长 1–100 毫秒且不大于间隔，默认 5。
- 倒计时 0/1/3/5 秒，期间可取消；次数/时长/持续三种模式互斥。
- 时长限制从倒计时结束起算；到期可在未收到手势回调时仍停止，派发前也检查截止时间。
- 频率默认关闭并持久化，仅记录当前运行成功回调，窗口 `(now - 1000, now]`，每 500 毫秒刷新。停止清零，旧回调不污染重启，悬浮统计区固定高度。

实际命令：

```powershell
.\.tools\gradle-8.11.1\bin\gradle.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain
```

退出码 0，BUILD SUCCESSFUL，50 个任务全部执行。日志 `.tools/v11-frequency-verification.log`。

JUnit XML 实际汇总：52 tests，0 failures，0 errors：ClickScheduler 25、ClickFrequency 5、RunOptions 2、IntervalValidator 3、OverlayGeometry 5、SafetyMonitor 4、ServiceState 6、WindowGroup 2。app 单元测试仍为 NO-SOURCE，不计为额外通过测试。

RED/GREEN 证据：低间隔边界测试在旧下限时失败；禁用新增倒计时/限制/时长行为的可编译实现时 42 项中 5 项失败；频率空实现时 5 项中 4 项失败；调度器频率未接入且刷新仍为 100 毫秒时 24 项中 4 项失败。恢复并实现后，最终 52 项全部通过。原始日志分别见 `.tools/new-red.log`、`.tools/v11-behavior-red.log`、`.tools/frequency-red.log`、`.tools/frequency-integration-red.log`。

Lint：0 错误、12 警告。旧系统内置状态栏尺寸及反射（2）、API31 属性（1）、插件版本提示（1）、备份规则（1）、数字/中文字符串本地化（6）、绝对屏幕坐标 LEFT 的 RTL 提示（1）。未压制警告。

- APK：`C:/project/AutoClicker/app/build/outputs/apk/debug/app-debug.apk`
- 大小：881871 字节。
- SHA256：`357310da9d99d44f256ebc3f5c00f7ad29c613a143d293d6fdbcbe9006626758`。
- apksigner：退出码 0，Verifies，APK v2 签名通过，开发调试签名。
- aapt：com.example.autoclicker，版本 1.1.0 (2)，minSdk 24，targetSdk 35，debuggable。
- adb devices：退出码 0，设备列表为空；没有手机或模拟器安装验收。

审查发现主线程延迟时截止后的回调先于定时器执行会被计数；新增回归测试在旧实现失败，修复为计数前检查截止时间后通过。覆盖恰好截止与晚 1 毫秒两种顺序，旧运行回调仍仅释放手势，不计入新运行。计时模式只统计截止之前被观察到的完成回调；已派发手势可能仍在截止后实际完成。日志 `.tools/deadline-callback-red.log` 与 `.tools/deadline-callback-green.log`。

**未完成验证：**真实手机上的短手势识别、实测频率数值与下层应用计数的差异、UI 显示/系统字体、配置保存与重开、自动停止、倒计时取消、锁屏旋转及旧版升级路径。核心测试不覆盖安卓权限授权、SharedPreferences 运行时或实际悬浮窗口。无真机性能或每秒 100 次的保证。

## 下一步

将 APK 安装到安卓手机，按 README 的设备清单验证固定点点击、停止、锁屏及旋转。当前交付是可安装的 debug 构建，不是已完成真机验收的正式发布版本。

## 1.2.0 界面改版验证

- 用户确认雾白与翡翠绿方向；本次不改点击引擎。
- 全量重新运行 `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain`：成功，50 个任务执行。52 项核心测试无失败/错误；安卓单元测试仍为 NO-SOURCE。
- `python tools/test_ui_contracts.py`：先观察 3 项失败，再实现共享样式、分组与开关；最后 3 项通过。这些是源码结构约束，不是设备 UI/交互测试。
- APK v2 签名验证通过；SHA256：34d05bb5d53ea68ee168d2ec34d2a33718e9ba2025c7758fcfb55582e73b81fa。日志 `.tools/ui-verification.log`。
- lint 0 错误 / 13 警告；没有隐藏警告。
- adb 设备列表为空；没有实际渲染截图，小屏、字体放大、键盘、悬浮窗视觉效果仍须设备验收。
- 自查：主要操作前置；持续模式隐藏停止值及标题；保留数字校验、运行禁用、权限说明、原生下拉指示和固定 56dp 统计区域；控制器停止使用警示色。
- 构建时发现说明字符串换行未转义，修复为 Kotlin 换行转义后全量构建成功。
- 改动未提交、未推送。此前 1.1.0 APK 哈希为历史记录，当前 APK 已覆盖。
