# AGENTS.md

## 范围与协作

本文件适用于整个仓库。默认用中文沟通，先阅读相关代码和已有文档，再做范围明确的修改。保留用户已有未提交改动，不执行破坏性清理或强制推送。只有用户明确要求时才提交、推送或发布；生成 APK 不代表发布 GitHub Release。

## 项目定位与结构

「轻点」是原生 Kotlin 安卓固定位置连点器，无 Root、无联网权限、无广告和账号。不要未经确认加入联网、屏幕内容读取、截图、登录或付费体系。

- `app/`：安卓应用；`MainActivity.kt` 是原生 View 设置页，不是 Compose/Web 前端。
- `app/src/main/java/com/example/autoclicker/ui/UiStyle.kt`：共享视觉样式。
- `overlay/OverlayController.kt`：靶心与悬浮控制面板、拖动和窗口生命周期。
- `service/AutoClickAccessibilityService.kt`：无障碍手势执行与核心调度接入；`ServiceStatus.kt` 发布状态。
- `settings/SettingsRepository.kt`：SharedPreferences 配置保存与损坏值回退。
- `core/`：无 Android 依赖的调度、频率、校验、几何、安全与状态规则；JUnit 测试在 `core/src/test/`。
- `tools/test_ui_contracts.py`：界面源码结构约束，不能代替设备 UI 测试。
- `tools/android-check/`：可选源码类型检查，不生成 APK。
- `docs/verification.md`、`docs/execution-ledger.md`：验证证据和执行记录；设计/计划在 `docs/superpowers/`。

## 工具链与构建

当前固定工具链：Gradle 8.11.1、AGP 8.9.2、Kotlin 2.1.20；JDK 21，JVM 字节码目标 17；minSdk 24，compileSdk/targetSdk 35。不要为无关任务升级依赖。

使用仓库自带的 Gradle Wrapper，在仓库根目录执行；不要依赖某台机器预先解压的 Gradle、SDK 或 Python 路径。

Windows：

```powershell
# 不需要 Android SDK 的核心测试
.\gradlew.bat -PcoreOnly :core:test

# 完整验证
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain

# 界面源码约束（使用当前环境的 Python 3）
python tools/test_ui_contracts.py
```

macOS / Linux：

```sh
./gradlew -PcoreOnly :core:test
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain
python3 tools/test_ui_contracts.py
```

开发环境自行安装兼容的 JDK、Android SDK 和 Python 3。SDK 位置通过当前环境的 `ANDROID_HOME` 或不提交的 `local.properties` 中的 `sdk.dir` 配置，不在仓库中硬编码安装路径。SDK 许可应由用户审阅接受，不静默接受新许可。

如需网络代理，通过开发者私有 Gradle 配置或环境变量配置，不在共享文档中固化代理地址、端口或凭据。不要假设 `.tools/` 下存在可用工具；该目录仅用于忽略的本地辅助文件。

读写中文文件显式指定 UTF-8，避免平台默认编码破坏文本。仓库文档使用相对路径，不记录开发者用户名、盘符或机器专属目录。

## 打包与签名（用户已确认）

- 调试包必须带版本号与 `debug`：`AutoClicker-<versionName>-debug.apk`，不要去掉 `debug`。
- `:app:assembleDebug` 自动生成交付副本：`app/build/outputs/distributions/debug/`。
- 保留 AGP 原始产物 `app/build/outputs/apk/debug/app-debug.apk`；不要把交付副本写进 AGP 输出目录，以免引入任务依赖/元数据冲突。
- 文件名取自 `app/build.gradle.kts` 的版本配置；改变命名时同步更新 Copy 任务的 `deliveryName` 输入，避免错误的 UP-TO-DATE。
- 实际版本变更同步调整 `versionName` 与递增的 `versionCode`；只改文件名不必提升版本。
- 当前未配置正式 Release 签名。Release 未签名包必须保留 `unsigned` 标识；不能将重命名的调试包称为正式版。
- 密钥库、密码、证书私钥不能提交，也不要要求用户把密码发到聊天里。正式签名须单独确认并配置。
- 交付前用 SDK 的 `apksigner verify` 验证签名，用 `aapt dump badging` 核对内部包名和版本；交付时提供当前环境可访问的安装包链接，不在仓库文档中硬编码机器路径。

## 必须保留的点击与安全规则

- 只有用户主动点击「开始」才执行；授权服务、打开窗口或重启不得自动点击。
- 保存配置，不保存点击坐标、不自动恢复运行；开始时使用配置快照，运行/倒计时中禁止编辑。
- 手势串行派发，不重叠、不补发积压；使用单调时钟，不依赖系统日期。
- 停止后不得派发新手势；已经交给系统的手势不能保证立即撤销，文案必须如实说明。
- 旧运行回调不能计入新运行；到达计时截止点先判断截止再统计回调。
- 锁屏、熄屏、旋转、服务断开、手势拒绝/取消及运行中拖动控制条应安全停止。
- 点击位置被控制面板遮挡时拒绝开始；点击时隐藏靶心；不创建全屏触摸拦截层。
- 保留无障碍用途说明、用户手动授权流程和不读取界面内容的服务配置。

配置边界以 `RunOptions` 和 `IntervalValidator` 为准：间隔 10–60000 ms；按压 1–100 ms 且不超过间隔；倒计时 0/1/3/5 秒；次数或时长停止互斥。短间隔不保证系统执行速度或目标应用识别率。

实时频率统计最近 `(now - 1000ms, now]` 内系统成功完成的手势回调，每 500 ms 刷新，以固定一秒为分母。不是理论频率或目标应用成功操作次数；倒计时等待，停止清零，新运行不混入旧数据。

## 界面与代码约定

- 保持雾白底色、翡翠绿强调色、深墨文字和共享样式；避免加入无关导航和装饰。
- 主操作突出；配置分为点击节奏、启动与停止、统计显示；选择、禁用、运行/停止状态清晰。
- 悬浮统计区域保持固定高度（当前 56dp），不要随计数/频率文本变化扩大面板。
- 兼顾触控尺寸、小屏滚动、字体放大、系统边缘与键盘；不要以源码检查推断视觉验收通过。
- 核心逻辑留在 `core`，Android 平台细节留在 `app`；遵循现有 Kotlin 结构，不为小改动引入大型框架。
- 行为修复先增加可复现回归测试，确认失败后修复；覆盖计时边界、取消与重启隔离。

## 验证与提交

- 宣称完成、测试通过或构建成功前，实际运行相应命令并检查退出码。区分执行、UP-TO-DATE 与 NO-SOURCE，不能将后两者说成新增测试已运行。
- 不隐藏 lint 警告；记录错误/警告数量、验证范围和未验证事项，不将历史数量当作当前结果。
- `adb devices` 无设备时，明确标注未完成真机/模拟器及视觉验收；测试、编译和签名验证都不能代替设备验收。
- 提交前运行 `git diff --check`，检查暂存范围；不提交 APK、构建目录、`.tools/`、本地 SDK 配置或凭据。
- 推送使用普通 push，不强推；成功后核对远端分支与本地 HEAD，并检查工作区状态。
