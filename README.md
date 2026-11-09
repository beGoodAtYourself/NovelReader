# NovelReader

NovelReader 是由 Lix 维护的 Android 本地阅读应用，专注于导入设备中的书籍、离线阅读和本地备份。主界面保留“书架”和“我的”，日常阅读无需联网。

## 下载最新版 APK

**[前往最新正式版 Release](https://github.com/beGoodAtYourself/NovelReader/releases/latest)**，无需登录即可下载。

以下直链对应 `3.26.19-local`，需要 Android 8.0 或更高版本；后续新版可通过上方 Release 入口获取。

| 安装包 | 适用设备 | 下载 |
| --- | --- | --- |
| ARM64（推荐） | 大多数 Android 手机 | [下载 APK](https://github.com/beGoodAtYourself/NovelReader/releases/download/v3.26.19-local/NovelReader-3.26.19-local-arm64-v8a.apk) |
| 通用版 | 不确定设备架构时选择 | [下载 APK](https://github.com/beGoodAtYourself/NovelReader/releases/download/v3.26.19-local/NovelReader-3.26.19-local-universal.apk) |
| ARMv7 | 32 位 ARM 设备 | [下载 APK](https://github.com/beGoodAtYourself/NovelReader/releases/download/v3.26.19-local/NovelReader-3.26.19-local-armeabi-v7a.apk) |

可下载 [SHA-256 校验文件](https://github.com/beGoodAtYourself/NovelReader/releases/download/v3.26.19-local/SHA256SUMS.txt) 核对安装包完整性。

## 主要功能

- **本地书籍**：导入设备中的 TXT、EPUB 等书籍，管理书架、分组和本地封面。
- **阅读设置**：调整字体、背景、行距和段距，选择翻页方式。
- **阅读辅助**：保留目录、书签、正文搜索、阅读进度和阅读记录。
- **本地规则**：支持替换净化、TXT 目录规则和标签规则。
- **离线朗读**：使用系统文字转语音服务中已安装的离线音色。
- **本地备份**：将阅读数据和设置备份到所选目录，并从本地文件恢复。

## 开始使用

1. 安装适合设备架构的 APK，将需要阅读的书籍文件保存到设备。
2. 在书架中导入本地书籍，打开书籍后按需调整阅读样式。
3. 如需朗读，先在系统文字转语音设置中安装可离线使用的音色。
4. 在“我的 → 设置”中进行本地备份与恢复；后续更新通过本项目的 GitHub Release 手动下载安装。

正式 APK 沿用 NovelReader 本地版签名，支持覆盖安装同签名版本。旧版本中的远程书籍不会在本地书架展示，恢复旧设置也不会重新启用在线功能。

## 离线与数据说明

本地版不提供在线书源、发现、订阅、Web 服务、WebDAV 同步、联网 AI、翻译或云端朗读，也不进行应用内自动更新。

- 安装包不申请互联网及局域网访问权限，不包含 Firebase 统计。
- 书籍、阅读进度、书签、阅读记录和设置保存在当前设备；备份写入用户选择的位置。
- 崩溃日志保存在本地，可在关于页面查看、清除或手动保存，不自动上传。
- 项目链接由系统浏览器打开，复制和分享由接收内容的应用处理。

书籍内容由用户自行导入，应用不提供书籍资源。详细数据处理方式见[本地阅读版隐私说明](app/src/main/assets/privacyPolicy.md)。

## 从源码构建

开发使用 JDK 21 和 Android SDK，当前 `compileSdk` 为 37.1，最低支持 Android 8.0（API 26）。配置好 `JAVA_HOME` 和 Android SDK 路径后，可在 Windows PowerShell 中构建 Debug 版：

```powershell
.\gradlew.bat :app:assembleAppDebug
```

Debug APK 输出到 `app/build/outputs/apk/app/debug/`。

仓库也提供[便携环境构建脚本](tools/build-local.ps1)。使用前需自行准备 `.local-build/` 中的 JDK 21、Android SDK、Gradle 9.8.0，以及正式版签名配置，再执行：

```powershell
.\tools\build-local.ps1 -Configuration Release
```

正式 APK 输出到 `app/build/outputs/apk/app/release/`。本机构建工具、缓存和签名配置不随仓库提供；需要覆盖安装的后续版本应沿用同一签名。

## 致谢

NovelReader 基于 [gedoor/legado](https://github.com/gedoor/legado) 和 [HapeLee/legado-with-MD3](https://github.com/HapeLee/legado-with-MD3) 定制，感谢原项目作者及所有贡献者。

同时保留对以下项目的致谢：

- [Luoyacheng/legado](https://github.com/Luoyacheng/legado)：扩展功能。
- [komikku-app/komikku](https://github.com/komikku-app/komikku)：界面灵感与 Compose 控件。
- [FoedusProgramme/Gramophone](https://github.com/FoedusProgramme/Gramophone)：界面灵感与 View 系统的封面取色方法。
- [jordond/MaterialKolor](https://github.com/jordond/MaterialKolor)：基于 Jetpack Compose 的取色实现。
- [Calvin-LL/Reorderable](https://github.com/Calvin-LL/Reorderable)：基于 Jetpack Compose 的拖动排序实现。
- 其他为原项目和本地阅读版提供支持的开源项目与贡献者。

## 许可证

项目采用 **GNU General Public License v3.0（GPL-3.0）**，完整条款见 [LICENSE](LICENSE)。原项目及第三方组件的署名、版权和许可证声明继续保留，各组件遵循各自的许可证。

应用内可通过“我的 → 关于 → 开源许可”查看许可证，随应用附带的文本见 [LICENSE.md](app/src/main/assets/LICENSE.md)。
