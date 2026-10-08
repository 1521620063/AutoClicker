# 轻点 · 安卓单点连点器

原生 Kotlin 安卓应用，最低 Android 7.0 / API 24。第一版仅支持固定位置持续点击，无 Root、无联网权限、无广告和账号。

## 当前状态

已实现中文设置页、授权用途说明、间隔保存、悬浮控制条、拖动靶心、串行点击调度和异常停止。完整 debug APK 已构建，32 项核心测试通过，Android lint 0 错误 / 8 警告。未连接设备，真机验收未完成。详细结果见 `docs/verification.md`。

安装包：`C:/project/AutoClicker/app/build/outputs/apk/debug/app-debug.apk`。

**源码 API 编译不等于 APK 构建，也不等于真机验证。最终 APK 与设备验收状态以验证记录为准。**

## 使用

1. 安装成功后打开「轻点 · 连点器」。
2. 选择「开启 / 管理无障碍服务」，阅读用途说明，在系统设置中手动开启本应用服务，然后返回。
3. 输入 50–60000 的整数毫秒并保存；默认 100 毫秒。
4. 点击「显示悬浮控制器」，切换到目标应用。
5. **先拖动靶心定位**，把控制条移到不挡住目标的位置，然后按「开始」。靶心在运行时隐藏。
6. 通过控制条「停止」或「关闭」结束；主页面也提供停止并关闭按钮。

主页面提供「测试点击」计数按钮，可用于检查点击和停止。实际跨应用点击还需在设备上验证。

锁屏、熄屏、配置变化（含旋转）、服务断开、手势取消/拒绝会停止。移动正在运行的控制条也会先停止，避免挡住目标后点击自身。旋转后需重新显示控制器、重新定位。重启不会自动恢复点击或坐标。

间隔是两次手势开始之间的目标时间，系统忙碌时可能更长；不补发积压点击。停止后不派发新手势，但系统已经接收的当前手势可能完成。遇到服务中断请重新开启服务，不会后台无限重试。

## 权限与隐私

通过 Android 无障碍服务发送点击手势，使用无障碍悬浮窗口显示控制器。必须由用户手动启用；应用无法绕过授权。没有普通悬浮窗、截图、界面节点读取或联网权限，也不收集界面文字。此工具不是面向残障辅助的专用无障碍工具。

只用于你有权自动操作的场景。某些厂商、受保护界面或应用可能限制模拟点击。侧载应用遇到系统「受限设置」时，只有确认 APK 来源可信才允许相关设置，具体操作以手机系统说明为准。

## 构建环境

- 构建 JDK：21（产物 Java/Kotlin 字节码 17）
- Gradle：8.11.1（Wrapper 已包含）
- Android Gradle Plugin：8.9.2
- Kotlin：2.1.20
- Android SDK Platform：35；Build Tools：35.0.0；minSdk：24；targetSdk：35
- 单元测试：JUnit 4.13.2

这是固定版本组合，不声称使用最新工具。`local.properties` 和 `.tools/` 仅是本机环境，不应提交或复制到其他电脑。

### 仅测试核心（不需要 Android SDK）

```powershell
.\gradlew.bat -PcoreOnly :core:test
```

### 安卓源码 API 编译检查（不生成 APK）

```powershell
.\gradlew.bat -PcoreOnly -PandroidCheck :tools:android-check:compileKotlin
```

此检查使用 Robolectric 的 Android 15 API jar 和仅限检查用的 R 占位类，验证 Kotlin 类型/API 引用。不处理 Android 资源，不执行 lint，不验证窗口行为，不能安装到手机。

### 完整 APK 构建

先通过 Android Studio 或官方 sdkmanager 安装 SDK Platform 35、Build Tools 35.0.0 和 Platform Tools。安装前由你阅读并接受适用 SDK 许可；本机已在用户明确同意后接受标准 SDK 许可；其他电脑仍需自行审阅接受。参考文本位于 `docs/android-sdk-license.txt`。

```powershell
# 根据实际安装位置，写入 local.properties 的 sdk.dir。
# 下方命令会交互式展示/确认所需许可，请自行决定是否接受。
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
.\gradlew.bat :core:test :app:lintDebug :app:assembleDebug
```

成功时 APK：`app/build/outputs/apk/debug/app-debug.apk`，这是开发调试签名，不是正式发布包。

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 网络代理

Gradle 不一定自动使用 Windows 系统代理。如依赖下载失败，可在个人 Gradle 配置中设置 HTTP/HTTPS 代理，或临时用 `JAVA_OPTS`；不要将账号、密码写入项目配置。当前环境工具下载通过本机系统代理完成，不更改全局设置。

## 工程结构

- `app/`：安卓页面、无障碍服务、悬浮窗口及偏好存储。
- `core/`：无平台依赖的间隔校验、手势调度、几何规则、服务状态和窗口事务；含自动化测试。
- `tools/android-check/`：可选源码类型检查模块，不参与 APK 打包。
- `docs/`：已确认设计、实施计划、执行记录、许可参考和验证记录。

## 设备验收清单

- 手动授权、显示窗口、拖动定位、计数增长。
- 靶心隐藏时，下层应用确实收到点击。
- 停止后计数不再增长（已经分发的手势除外）；关闭后窗口消失。
- 控制条遮挡目标时拒绝开始；运行中移动控制条会停止。
- 屏幕边缘和不同导航方式下的实际坐标正确。
- 锁屏、旋转、禁用服务、重新连接不继续点击，无残留窗口。

无连接设备时，上述项目必须标记为未验证，不能以单元测试代替。
