# 冲浪阅读 · 安卓 APP 项目开发日志

> 最后更新：2026-08-14
> 包名：`com.xiaoswz.reader` · 当前版本：**v0.2.0**（versionCode 2）
> 仓库：`U:\xiaoswz-reader`（独立 Git 仓库，未推 GitHub）

---

## 一、项目概况

冲浪阅读是一款**原生安卓小说阅读 APP**，数据全部来自「冲浪中文网」(`xiaoswz.vercel.app`) 的**公开只读 API**，不登录、不直连数据库、对主站零改动。APP 只通过 HTTP 接口连网站，把整个站点当作唯一数据源。

| 项 | 内容 |
|---|---|
| 技术栈 | Kotlin + Jetpack Compose (Material 3) + Retrofit + OkHttp + Coil + DataStore |
| 最低/目标 SDK | minSdk 26 / compileSdk 35 |
| 数据源 | `/api/books`（书库）+ `/api/book-source`（搜索/详情/目录/正文） |
| 构建 | Gradle 8.11.1 + AGP 8.7.3，Gradle 走腾讯云镜像、Maven 走阿里云镜像 |
| JDK | Android Studio 自带 JBR 21 |

---

## 二、当前状态（2026-08-14）

- **构建状态**：✅ 已完成。`assembleDebug` 一次通过（修复了 `UpdateManager.kt` 3 处空安全编译错误后）。
- **APK 产物**：`app/build/outputs/apk/debug/app-debug.apk`（18.4 MB，v0.2.0）
- **更新清单**：`app/build/outputs/apk/debug/version.json`（供局域网自动更新读取）
- **Git**：当前在 `main` 分支，最新提交 `efebb3f`（M2 全部改动已入库，工作树干净）
- **实测**：M1 已真机实测全部功能正常；M2 已构建通过，**尚未真机运行**（需你装机验证）

### 局域网下载 / 更新地址

```
http://192.168.2.4:8765/app-debug.apk     ← 直接下载安装
http://192.168.2.4:8765/version.json      ← 自动更新清单（APP 内更新功能读这个）
```

> 手机需与电脑连同一 Wi-Fi。该下载服务为本机临时进程，链接失效时重启即可（见第七节）。

---

## 三、里程碑记录

### M1 — 骨架（v0.1.0，2026-08-13，commit `b7aca7a`）

首个可用版本，**真机实测全部通过**。

- 书城：封面网格、搜索、最新/热门排序、滚动到底自动翻页
- 书籍详情：封面、简介、目录、「开始阅读」
- 阅读器（基础版）：正文阅读、上/下一章、字号 A-/A+、日/夜间切换、屏幕常亮
- 数据链路打通：全部走公开只读 API

### M2 — 阅读器打磨 + 局域网自动更新（v0.2.0，2026-08-14，commit `efebb3f`）

**阅读器重写**（核心卖点）
- `TextMeasurer` 文本精确分页 + `HorizontalPager` 覆盖翻页
- 点击屏幕三区翻页 / 呼出设置菜单，全屏沉浸式
- 翻页模式：滚动 / 覆盖 双模式可切换
- 阅读器内目录抽屉（`TocDrawer`）跳章
- 下一章预取缓存，消除翻章卡顿
- 音量键翻页（`VolumeKeyBus`）
- 上一章跳到末页继续读
- 阅读设置底部面板（`ReaderSettingsSheet`）

**阅读设置持久化**（DataStore，重启保留）
- 字号、主题（米纸 / 夜间 / 护眼绿 / 纯黑 OLED）
- 翻页模式、行距、段距、首行缩进、边距、屏幕常亮
- 更新服务器地址（可改）

**局域网自动更新**
- `UpdateManager`：拉 `version.json` → 比对 `versionCode` → 流式下载带进度 → `FileProvider` 调起安装 → 未知来源权限自动引导
- 更新对话框（`UpdateDialog`）：可编辑服务器地址
- 书城启动静默检查 + 顶栏手动「检查更新」入口
- `tools/write_update_manifest.py`：构建后一键生成 `version.json`

**配置改动**
- 版本升 `0.2.0` / `versionCode 2`
- `AndroidManifest` 加 `REQUEST_INSTALL_PACKAGES` 权限 + `FileProvider`
- 新增 `res/xml/file_paths.xml`

---

## 四、已完成功能清单

| 模块 | 功能 | 状态 |
|---|---|---|
| 书城 | 列表/搜索/排序/分页 | ✅ |
| 书籍详情 | 封面/简介/目录 | ✅ |
| 阅读器 | 分页、点击翻页、覆盖/滚动翻页 | ✅ |
| 阅读器 | 全屏沉浸、日/夜/护眼/纯黑主题 | ✅ |
| 阅读器 | 字号、行距、段距、缩进、边距 | ✅ |
| 阅读器 | 目录抽屉跳章、音量键翻页、章节预读 | ✅ |
| 设置 | DataStore 持久化、设置面板 | ✅ |
| 更新 | 局域网自动检查/下载/安装 | ✅ |

---

## 五、路线图（后续里程碑）

| 里程碑 | 计划内容 | 版本 |
|---|---|---|
| ~~M1~~ | 书城→详情→阅读器 | v0.1.0 ✅ |
| ~~M2~~ | 阅读器打磨 + 局域网自动更新 | v0.2.0 ✅ |
| **M3** | 本地书架（Room）、阅读进度记忆、章节离线缓存、更新检测 | v0.3.0 |
| **M4** | 分类浏览、搜索增强、热度榜（需动后端，单独审批） | v0.4.0 |
| **M5** | Release 签名、图标/启动页、关于页、包体积优化 | v1.0.0 |
| 远期 | 登录/云同步/月票/推送（配合主站大更新统一规划） | — |

> 原则：不登录、不急着装数据库；一期只做「看小说」这一件事，逐步扩展。

---

## 六、构建与发布流程（每次更新都走这套）

1. 改代码，`versionCode` +1、`versionName` 升一档
2. `./gradlew assembleDebug --no-daemon` 构建
3. `python tools/write_update_manifest.py "更新说明"` 生成 `version.json`
4. 在 APK 输出目录起下载服务：`python -m http.server 8765`
5. 把局域网链接发给手机，APP 内也能自动检测更新
6. `git add -A && git commit` 入库

> 注意：`build/`、`.setup/`、`build.log`、`.workbuddy/`、签名文件、本地 `local.properties` 均已 `.gitignore`，不入库。

---

## 七、本地环境关键说明

- **Android SDK**：之前被清掉，已重装到默认路径 `C:\Users\Administrator\AppData\Local\Android\Sdk`（platform-35 + build-tools 35.0.0）。
- **Gradle 镜像**：官方源拉不动，已配腾讯云 + 阿里云镜像（见 `settings.gradle.kts` / `build.gradle.kts`）。
- **下载服务**：本机临时 `python -m http.server 8765`，会话结束可能停。失效时重新执行上面第 4 步即可。
- **回收站虚惊**（2026-08-13）：本机沙箱会把删文件移入回收站而非真删，Gradle 构建产生的大量临时文件让回收站显得像"项目被删"。项目本体从未丢失。

---

## 八、已知问题 / 注意事项

- M2 仅完成编译，未真机运行，阅读器分页、翻页动画、音量键等手感需你实测反馈。
- 本地局域网更新依赖电脑端下载服务在线；离开局域网或电脑关机则自动更新不可用。
- 主题「纯黑 OLED」与「护眼绿」为新加，配色观感待你确认是否满意。
- 书架、离线缓存尚未做（M3），当前退出阅读后不记忆进度（设置项已预留，进度记忆随 M3 落地）。
