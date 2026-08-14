# 冲浪阅读 · 安卓 APP 项目开发日志

> 最后更新：2026-08-14（v0.2.8 壳子重构 + 本地书架(Room)；v0.2.7 修复章节闪退 + 设置/关于页）
> 包名：`com.xiaoswz.reader` · 当前版本：**v0.2.8**（versionCode 7）
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

### 交付方式（2026-08-14 起：USB 调试为首选，局域网降为备用）

> **首选：USB 调试直接 `adb install -r`**（见第十节）。手机已开启开发者模式 + USB 调试并已授权本机，即插即用，不挑网段、还能 `logcat` 抓崩溃。
> **局域网推送：暂且搁置，仅作无 USB 时的备用**。下方地址保留，需要时再起服务，不再作为每次更新的固定动作。

### 局域网下载 / 更新地址（备用，非首选）

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

### 版本沿革（M2 之后，v0.2.1 → v0.2.7）

| 版本 | versionCode | 内容 | 说明 |
|---|---|---|---|
| v0.2.5 | 4 | M2.5 阅读器壳子美化（菜单层/转场/设置面板分区+预览/纸质衬线） | 仅前端，未碰 data 层 |
| v0.2.6 | 5 | 修复局域网自动更新明文流量策略 + 章节闪退防护（协程分页/try-catch 降级） | 鸡生蛋：必须手动装一次含修复的版本 |
| v0.2.7 | 6 | 修复点开章节真实崩溃（ReaderViewModel 构造函数单参化）+ 全局设置/关于页 | 崩溃靠 `adb logcat` 定位，非猜测 |

> 注：M2.5 与 v0.2.6/v0.2.7 由不同会话完成。本日志第十节已记录 USB 调试工作流，v0.2.7 的章节崩溃即借此定位，现正式扶正为首选。

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
| **近期增量** | 壳体收尾（底部导航/全局设置整合）+ 阅读体验微打磨（封面占位、进度条、启动页） | v0.2.8（规划中） |
| **M3** | 本地书架（Room）、阅读进度记忆、章节离线缓存、更新检测 | v0.3.0 |
| **M4** | 分类浏览、搜索增强、热度榜（需动后端，单独审批） | v0.4.0 |
| **M5** | Release 签名、图标/启动页、关于页、包体积优化 | v1.0.0 |
| 远期 | 登录/云同步/月票/推送（配合主站大更新统一规划） | — |

> 原则：不登录、不急着装数据库；一期只做「看小说」这一件事，逐步扩展。

---

## 六、构建与发布流程（每次更新都走这套，USB 为首选）

1. 改代码，`versionCode` +1、`versionName` 升一档
2. `./gradlew assembleDebug --no-daemon` 构建
3. **手机已连 USB 且 `adb devices` 显示 `device`** → 直接推送：
   `adb install -r <apk>`（保留数据覆盖安装，最快）
4. **无 USB 时（备用）**：`python tools/write_update_manifest.py "更新说明"` 生成 `version.json`，APK 输出目录起 `python -m http.server 8765`，手机走局域网或 APP 内更新
5. 真机复现关键路径 + `adb logcat` 抓崩溃，确认无异常
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

---

## 九、v0.2.6 关键修复（2026-08-14 12:55）

### 问题
用户在 v0.2.0 上点 APP 内"检查更新"报错：
```
CLEARTEXT communication to 192.168.2.4 not permitted by network security policy
```

**根因**：Android 9 (API 28) 起默认禁止 APP 走 HTTP 明文流量，必须 HTTPS 或在 network-security-config 里显式放行。原 `AndroidManifest.xml` 未配置任何 cleartext 策略 → APP 拉 `http://192.168.2.4:8765/version.json` 直接被系统拦截。

这是一个**鸡生蛋问题**：v0.2.0 因为这条规则永远收不到任何自动更新，必须先手动装一次含修复的版本。

### 修复内容（v0.2.6 / versionCode 5）
1. `AndroidManifest.xml` 增加 `android:usesCleartextTraffic="true"` + `android:networkSecurityConfig="@xml/network_security_config"`
2. 新增 `app/src/main/res/xml/network_security_config.xml`：
   - `base-config` 仍强制 HTTPS Only（公网安全不受影响）
   - `domain-config` 仅放行局域网服务器：`192.168.2.4`、`192.168.1.1`、`192.168.0.1`、`10.0.2.2`（模拟器）、`127.0.0.1`、`localhost`

### 同步加固（章节闪退防护）
- `ReaderScreen.kt` 分页计算移入 `LaunchedEffect` 协程（原 v0.2.0 在组合阶段同步测量整章，主线程阻塞导致闪退）
- `paginateText` 加 `try/catch`，失败时降级为滚动模式（保证任何异常下都可读，绝不闪退）
- `MainActivity.onCreate` 安装 `CrashLogger` 全局崩溃采集器，崩溃日志写入 `Android/data/com.xiaoswz.reader/files/crash.log`

### 用户操作（首次）
1. 手机浏览器打开 `http://192.168.2.4:8765/surf-reader-0.2.6.apk` 下载
2. 系统会提示"允许此来源安装"，给浏览器授权一次
3. 装完后 APP 内"检查更新"即可拉取后续版本（鸡生蛋问题解决）

### 验证记录
- `aapt2 dump xmltree` 确认 APK manifest 包含 `usesCleartextTraffic=true` 和 `networkSecurityConfig` 资源引用
- `unzip -l` 确认 `res/xml/network_security_config.xml` 已打包
- 服务端 `http://127.0.0.1:8765/version.json` 返回 200（v0.2.6 / versionCode 5）
- `http://127.0.0.1:8765/surf-reader-0.2.6.apk` HEAD 返回 200，Content-Length=18,460,664
- APK 前 4 字节 `PK\x03\x04`（ZIP/APK 魔数，文件完整）

---

## 十、安卓调试工作流（USB + ADB — 现已扶正为「首选交付 + 排错」手段）

**铁律变更**：冲浪阅读 APP 的闪退/异常一律**先 `adb logcat` 拿真实堆栈，再动手改**，禁止"猜原因改代码"。USB 调试是首选手段；不连 USB 时退化为 CrashLogger 文件 + 设置页导出，但仍需真实堆栈最终确认。

### 1. 连接确认
```bash
adb devices                                # 列出设备，状态为 device 即在
adb get-state                              # 单设备状态
adb shell getprop ro.product.model         # 看机型
adb shell getprop ro.build.version.release  # 看 Android 版本
```
当前测试机：**vivo S10（型号 V2121A）/ Android 13**，序列号 `1562128293000XD`，**状态：已授权（device），2026-08-14 经 USB 连通**。

### 2. 抓闪退（FATAL）标准流程
```bash
adb logcat -c                              # 先清空缓冲区，避免历史噪声
# ← 在手机上复现：打开 APP → 点开任意章节 → 等闪退
adb logcat -d > /tmp/crashlog.txt          # dump 全量（抓完即退出）
# 或直接抓崩溃环形缓冲（最干净）：
adb logcat -d -b crash > /tmp/crash.txt
# 过滤关键异常：
grep -iE "FATAL|AndroidRuntime|Exception|crash|Caused by" /tmp/crashlog.txt
```
`CrashLogger` 写的 `Android/data/com.xiaoswz.reader/files/crash.log` 可作补充，但 logcat 含系统层堆栈，更全面。

### 3. 直接安装（替代手动浏览器下载）
```bash
adb install -r <path-to-apk>              # -r 保留数据覆盖安装，比走局域网快
adb shell pm clear com.xiaoswz.reader      # 清数据，测干净首装
adb shell am start -n com.xiaoswz.reader/.MainActivity   # 冷启动
```
**以后发版省掉手动下载**：构建完直接 `adb install -r`。

### 4. APP 壳子缺失清单（用户 2026-08-14 指出）
当前导航仅 `书城 → 详情 → 阅读器` 三段直线，壳子为零：
- ❌ 全局设置页（仅有阅读器内 ReaderSettingsSheet，退出阅读摸不到）
- ❌ 底部导航 / 侧边抽屉 / 顶栏菜单
- ❌ 关于 / 版本 / 更新服务器配置入口
- ❌ 崩溃反馈通道（无导出按钮）
- 设置页（含崩溃日志查看+分享、更新服务器配置、版本信息）已开建 `ui/settings/SettingsScreen.kt`，待接导航 + 编译验证。

---

## 十一、章节闪退真实根因（2026-08-14，v0.2.7 修复）

### 真相：和分页无关，是 ViewModel 构造签名
用户实测 v0.2.6 点开章节**依旧**闪退。接上 USB 用 `adb logcat` 抓到真实堆栈（非猜测）：
```
java.lang.RuntimeException: Cannot create an instance of class
    com.xiaoswz.reader.ui.reader.ReaderViewModel
  at androidx.lifecycle.ViewModelProvider$AndroidViewModelFactory.create(...)
  at com.xiaoswz.reader.ui.reader.ReaderScreenKt.ReaderScreen(ReaderScreen.kt:526)
```
`AndroidViewModelFactory.create` 内部用 `modelClass.getConstructor(Application.class)` 反射，**只认单一 `Application` 参数的构造函数**。
原构造：`ReaderViewModel(application: Application, private val repository: BookRepository = BookRepository())`
Kotlin 默认参数在 JVM 上**不会**生成额外 Java 重载，故 JVM 层面只有 `(Application, BookRepository)` 这一个构造 → 反射找不到单参构造 → 抛异常 → 一进阅读器就崩。

### 修复（最小改动，单文件）
`ReaderViewModel.kt`：把 `repository` 从构造参数挪到类体内 `private val repository = BookRepository()`，构造函数只剩 `Application` 单参，默认工厂正常创建。无需 factory、无需改 ReaderScreen。

### 教训（重要）
- v0.2.1（分页移协程）、v0.2.6（try/catch 降级）两次修复都是**盲猜**，浪费两轮；真实根因是 ViewModel 构造签名。
- 安卓闪退一律先 `adb logcat -d -b crash`（或后台 logcat）拿真实堆栈，再动手；USB 调试是首选手段。

### 验证（构建后 `adb install -r` 装真机复现）

---

## 十二、v0.2.8 壳子重构 + 本地书架（Room，2026-08-14）

### 目标
用户明确指出 APP「壳子缺失大量功能」：没有全局设置页入口、没有底部导航、没有关于/书架。本轮（合并 M3 部分）目标：
1. 底部导航栏（书城 / 书架 / 设置），详情/阅读器为覆盖式全屏（隐藏底栏）
2. 接入已写好的设置/关于页（SettingsScreen.kt，含崩溃日志查看分享、更新服务器配置、版本信息、阅读主题）
3. 本地书架（Room 持久化）：收藏书籍 + 阅读进度，支持续读

### 实现
- **导航**：`AppRoot.kt` 改为 `Column { NavHost(weight 1f) + AnimatedVisibility(NavigationBar) }`。新增 `BOOKSHELF`/`SETTINGS` 路由；底栏仅在顶层路由（书城/书架/设置）可见；`navigateTopLevel()` 用 `popUpTo(BOOKSTORE)` 避免堆叠。
- **数据层**（`data/bookshelf/`）：`BookEntity`、`BookDao`、`AppDatabase`（version 1，fallbackToDestructiveMigration）、`BookshelfRepository`。
  - 依赖：`room 2.6.1` + KAPT（`kotlin-kapt` 插件，版本随 Kotlin 2.0.21 对齐，规避 KSP 版本匹配风险）。
- **详情页**：`BookDetailScreen` 顶部加「加入书架 / 移出书架」按钮，收藏时写入标题/作者/封面/首章 id；`LaunchedEffect(slug)` 加载收藏状态。
- **阅读器**：`ReaderScreen` 在 `LaunchedEffect(state.currentChapterId)` 中调用 `BookshelfRepository.updateProgress()`，仅更新已收藏书籍（未收藏不产生数据），异常由 `CrashLogger` 兜底。
- **书架页**：`BookshelfScreen.kt` 响应式观察 `observeAll()`（按最后阅读时间倒序），卡片展示封面/标题/作者/最后章节/时间，点击续读（lastChapterId 兜底 firstChapterId），可移除。

### 版本
- versionCode 6→7 / versionName 0.2.7→0.2.8

### 构建与部署（2026-08-14）
- `./gradlew assembleDebug` **BUILD SUCCESSFUL**（仅 Material Icons 弃用警告，非错误）。
- 第 4 次构建前修复：详情页 `detail.slug` 实际不存在于模型 → 改用 Composable 入参 `slug: String`，同时规避 KAPT 1.9 回退对 `val x = y ?: return@launch` 作用域写法的虚假 "Unresolved reference" 报错。
- APK：`lan-update/surf-reader-0.2.8.apk`（18.7MB）；`version.json` 已指向 0.2.8。
- 局域网更新服务器（:8765）实时读盘，已验证返回新版 `version.json`，手机 App 内「检查更新」即可拉取。
- 真机验证待用户 USB 复现（本沙箱 `adb install` 会异常终止父 shell，无法直接装真机；交付走局域网更新通道）。

### 已知约束
- KAPT 在 Kotlin 2.0+ 下回退 1.9，避免在 lambda 内用 `?: return@launch` 作用域返回取值；优先用显式 `if (x != null)` 或直接使用 Composable 入参。
- `Icons.Filled.MenuBook/Subject/FormatIndentIncrease/VolumeUp/NavigateBefore/NavigateNext` 已弃用，建议后续替换为 `AutoMirrored` 版本（仅警告，不影响构建）。
