# 快捷录音

简洁的本地 Android 录音工具。锁屏或熄屏时，双击音量下键开始录音，再次双击停止保存。也可以在应用首页手动录音。

**当前版本：0.4-test · Android 10+ · targetSdk 36**

## 0.4 图标更新

深海蓝底、白色三段声波和两点冰蓝标记，支持自适应图标及 Android 13+ 单色主题图标。通知仍使用独立单色小图标。本次不改变录音行为；同签名版本可覆盖升级。

图标矢量母版与1024px预览位于 `branding/`。预览采用圆角方形遮罩，实际桌面轮廓由启动器决定。

## 功能

- 本地 AAC/M4A 录音、试听、按文件导出和删除。
- 锁屏 / 熄屏双击音量下键；不主动点亮或解锁屏幕。
- 低饱和蓝灰界面，跟随系统深浅色主题。
- 录音持续通知和停止操作，启动/保存成功短震一次，失败三次震动。
- 无账号、无联网权限、无云同步、无广告或统计 SDK。

## 兼容性说明

熄屏快捷键是**需要实机验证的功能**，不是系统通用保证。部分厂商系统不会转发熄屏按键；Android 对后台麦克风也有限制。启动失败时会保留本机诊断，不会切换到其他启动方式。可以解锁后在首页手动开始录音。

录音器启动成功并不保证音频有声音：系统麦克风隐私开关、其他应用抢占、来电、存储不足或进程被系统结束都可能影响结果。请先录一小段并试听，不要将测试版作为重要场合的唯一录音方式。

## 使用

1. 安装应用，允许麦克风和通知权限。先在首页录音约10秒，停止并试听。
2. 进入 **设置 → 启用双击快捷键**，阅读用途说明。再进入系统无障碍设置，自行授权本应用。
3. 锁屏后短按两次音量下键。两次按下间隔不超过450毫秒，每次按住少于250毫秒；再次双击停止保存。
4. 录音卡片可播放；点击 **更多** 导出或删除。异常可查看 **设置 → 诊断与帮助**。

按键仍正常调节音量，可能与相机等系统快捷键冲突。只有锁屏或熄屏时识别快捷键。无障碍服务仅申请按键过滤，不读取窗口或屏幕文字；可随时在应用和系统设置中关闭。

系统若阻止安装或权限配置，请遵循系统安全提示，不需要关闭安全防护。仅录制有权录制的内容。

## 隐私与数据

录音存放在应用私有目录，禁用系统备份。只有主动使用系统分享面板导出时，选定的接收应用才获得该文件的读取权限。诊断仅保留本机最近约7000字符，不含音频。

**卸载或清除应用数据会删除全部录音。** 更新时请使用相同签名的新版覆盖安装，重要内容请先导出。0.3保留此前录音和快捷键开关；旧版的已移除启动选项会被清理且不再影响录音路径。

## 构建

需要官方 Android SDK Platform 36、Build Tools 36.0.0，以及 JDK 17 或更高版本。请自行阅读并接受 [Android SDK 许可](https://developer.android.com/studio/terms)。项目不捆绑 SDK、JDK 或第三方依赖。

### 不依赖 Gradle 的本地构建

```sh
export JAVA_HOME=/path/to/jdk
export ANDROID_HOME=/path/to/android-sdk
./build-local.sh
```

输出 `build-local/QuickRecorder-0.4-test.apk`。脚本使用 aapt2、javac、D8、zipalign 和 apksigner，并校验签名与对齐。

首次构建会在 `.test-signing/` 创建本机测试签名密钥。**不要提交或分享签名私钥。** 不同机器生成的密钥不同，因此不能直接覆盖另一签名安装的应用。维护者应安全保管原签名密钥；正式发布不应使用临时测试签名策略。

### Gradle 项目

也提供 Gradle 8.13 / Android Gradle Plugin 8.13.0 配置。在可正常访问 Google/Maven 仓库的环境中设置 SDK 路径后运行：

```sh
gradle :app:assembleDebug :app:lintDebug
```

此轮构建使用上述官方 SDK 工具链脚本。当前构建环境的 Gradle 插件解析失败，lint 未执行，不能将手动编译成功视为 lint 通过。

## 测试

```sh
mkdir -p build-tests
"$JAVA_HOME/bin/javac" -d build-tests app/src/main/java/cn/local/quickrecorder/DoublePress.java tests/DoublePressTest.java
"$JAVA_HOME/bin/java" -cp build-tests DoublePressTest
python3 tests/static_checks.py
python3 tests/ui_checks.py
python3 tests/screen_off_checks.py
python3 tests/icon_checks.py
```

包含手势时序测试、权限/数据安全静态断言、配色对比度断言，以及不主动点亮屏幕路径的静态检查。本轮未执行手机或模拟器上的0.4运行测试；这类检查不能替代真实机型的录音/声音/锁屏测试。

## 许可

尚未指定开源许可证。本仓库公开可见不等于授予开源使用许可；在许可明确前，请勿将其视为已采用 MIT、Apache 或其他开源许可证。
