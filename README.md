# 冲浪阅读

冲浪中文网（xiaoswz.vercel.app）的原生安卓阅读客户端。

## 定位

- 纯阅读 APP：**不登录、不写库**，只通过网站公开只读 API 获取数据
- 数据源与「阅读3.0」书源相同，但走底层 JSON API，不爬网页
- 核心目标：原生沉浸式阅读体验

## 技术栈

Kotlin · Jetpack Compose (Material 3) · Retrofit + kotlinx.serialization · Coil · Navigation-Compose

- minSdk 26 / targetSdk 35 / compileSdk 35
- Gradle 8.11.1 · AGP 8.7.3 · Kotlin 2.0.21

## 数据来源（全部为公开只读接口）

| 用途 | 接口 |
|---|---|
| 书城列表（分页/排序/搜索） | `GET /api/books?page&limit&sort&search` |
| 书籍详情 + 目录 | `GET /api/book-source?action=detail&bookId={slug}` |
| 章节正文（纯文本） | `GET /api/book-source?action=content&chapterId={id}` |

BASE_URL 在 `app/build.gradle.kts` 的 `buildConfigField` 中配置。

## 构建

```powershell
# 需要本机安装 Android SDK（local.properties 中配置 sdk.dir）
.\gradlew assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

## 当前状态（v0.5.7）

- ✅ M1–M3 全部完成：书城 → 详情 → 阅读器全流程、本地书架（Room）、章节离线缓存、书籍更新检测
- ✅ 封面显示修复：主站封面为 `data:` URI，Coil 不加载 → 解码为 `ByteBuffer` 交 `ByteBufferFetcher`
- ✅ 书架崩溃修复：收藏封面整串 data URI 撑爆 SQLite 游标窗口 → 压缩存储 + 旧数据按 slug 自动恢复
- ✅ 健康治理：清理 13.9MB 未引用美术素材（APK 33MB→~20MB）；封面调试日志加 `BuildConfig.DEBUG` 守门
- ⏳ M4 书城增强（分类/热度榜）：主站 `/api/books?category`、`/api/books/ranking`、`/api/categories` 接口已就绪，纯 APP 侧接入
- 💡 远期：账号登录与进度/书架云同步（需主站新增 APP token 端点，现有 `ReadingProgress`/`Bookmark` 表够用）、月票/积分

> 定位不变：**不登录、不写库**，只调主站公开只读 API。

## 构建

```powershell
# 需要本机安装 Android SDK（local.properties 中配置 sdk.dir）
.\gradlew assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk（脚本另复制到 builds\app_N 隔离输出）
```

## GitHub 备份

- 仓库：`https://github.com/lkj314/xiaoswz-reader`
- 本地分支 `main` 已跟踪 `origin/main`
- ⚠️ 本机 `git-credential-manager` 在 push 时会崩溃杀掉 shell，推送必须禁用它：

```bash
GIT_TERMINAL_PROMPT=0 git -c credential.helper= push -u origin main
```
