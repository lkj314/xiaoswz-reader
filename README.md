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

## 路线图

- **M1（当前）**：书城 → 详情 → 阅读器 全流程打通（列表/搜索/排序/分页/正文/上下章/字号/日夜间）
- **M2**：阅读器体验打磨（翻页模式、更多主题、排版细节）
- **M3**：本地书架（Room）、章节预加载与离线缓存
- **远期**：账号登录与进度同步（需后端新增 token 端点）、榜单接口
