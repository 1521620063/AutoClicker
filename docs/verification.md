# 验证记录 — 2026-10-08

## 结果概览

| 检查 | 实际结果 |
|---|---|
| 核心 JUnit 测试 | 32 项，0 失败，0 错误；已强制重新运行 |
| 安卓源码 API 编译 | 成功；Android 15 API jar 类型检查，不是 APK 构建 |
| XML 结构/权限静态检查 | 5 个 XML 格式正确；manifest 无 uses-permission；不启用界面内容读取 |
| Gradle 下载校验 | 官方发行包 SHA256 匹配，已固定在 Wrapper 配置 |
| Android lint | 完成：0 errors，8 warnings；未隐藏警告 |
| 完整 debug APK | 构建成功；APK v2 签名验证通过 |
| 真机/模拟器验收 | adb devices 已执行，设备列表为空；设备验收未执行 |

## 自动化测试明细

- `ClickSchedulerTest`：12 项，失败 0，错误 0。
- `IntervalValidatorTest`：3 项，失败 0，错误 0。
- `OverlayGeometryTest`：5 项，失败 0，错误 0。
- `SafetyMonitorTest`：4 项，失败 0，错误 0。
- `ServiceStateTest`：6 项，失败 0，错误 0。
- `WindowGroupTest`：2 项，失败 0，错误 0。

## 命令与证据

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

## 本次完整构建与交付 — 2026-10-08

用户明确同意适用标准 Android SDK 许可后，仅安装 Platform 35、Build Tools 35.0.0、Platform Tools。许可证目录仅有 android-sdk-license；未接受其他产品许可。历史失败记录保留如上，已不再是当前阻碍。

实际执行：`:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain`。退出码 0，BUILD SUCCESSFUL，50 个任务执行。日志：`.tools/apk-verification.log`。32 项核心测试，0 失败、0 错误；app 单元测试任务为 NO-SOURCE，不能计为额外测试通过。

Lint 报告：`app/build/reports/lint-results-debug.txt`，0 errors、8 warnings：旧系统状态栏内部尺寸资源及反射 API（2）、API31 才生效的属性（1）、构建插件版本提示（1）、备份规则配置提示（1）、中文 UI 数字/字符串本地化（2）、绝对屏幕坐标采用 LEFT 的 RTL 提示（1）。没有压制这些警告；旧系统状态栏/边缘定位仍需设备验收。

- APK：`C:/project/AutoClicker/app/build/outputs/apk/debug/app-debug.apk`
- 大小：871327 字节。
- SHA256：`bc8316cd2d7861987a30289819cd5fc933b6512e3253149f7c6e5d504b9d5afa`。
- apksigner verify --verbose：退出码 0，Verifies，v2 签名通过；开发调试签名。
- aapt dump badging：包名 com.example.autoclicker，版本 1.0.0 (1)，minSdk 24，targetSdk 35，debuggable。
- adb devices：退出码 0，设备列表为空；未安装到设备，未进行授权、窗口、真实点击或停止行为验收。

## 下一步

将 APK 安装到安卓手机，按 README 的设备清单验证固定点点击、停止、锁屏及旋转。当前交付是可安装的 debug 构建，不是已完成真机验收的正式发布版本。
