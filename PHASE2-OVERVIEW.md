# 冲浪阅读 v0.4.4 — Phase 2 美术资产全量应用

> 目标：把已生成的二次元美术资产（小鲸角色 + 深海背景）一口气集成进 App，做出可真机测试的 0.4.4 版本。

## 版本
- `versionName = 0.4.4`，`versionCode = 23`（`app/build.gradle.kts`）

## 新增文件
| 文件 | 作用 |
|------|------|
| `ui/components/AssetImage.kt` | `artAsset(path)`（`androidAsset://art/...`，Coil 2.7.0）+ `ArtImage` 封装，统一从 `assets/art/` 加载 |
| `ui/components/WhaleBackground.kt` | 全局深海背景（`bg_deep_ocean` + 0.45 深海底色叠加），套在 AppShell 内，**阅读器路由排除** |

## 关键改动
- **Splash 重写**（`AppRoot.kt`）：`bg_splash` 魔法背景 + `character_full` 小鲸立绘（入场渐显+上浮动画）+ 品牌字标「冲浪阅读 / 畅读每一页」
- **全局沉浸式背景**：`Scaffold` 改 `containerColor = Color.Transparent`，深海纹理透出；阅读器保留独立主题不受影响
- **书城**：新增 `WelcomeHero` 玻璃卡（欢迎文案 + `character_sitting` 陪伴）；空状态→`character_empty`；错误态→`character_error`
- **书架**：欢迎头图（欢迎回来 + `character_sitting`）；空状态→`character_empty`
- **设置**：资料头卡用 `avatar_circle`（圆形）+ 版本信息；`SettingsCard` 改玻璃风格
- **详情**：错误态→`character_error`

## 已落地资产
`character_full` / `character_sitting` / `character_empty` / `character_error` / `avatar_circle` / `bg_deep_ocean` / `bg_splash`

## 暂未应用（留交互打磨）
`character_half`、`character_celebrate`、`expr_happy`、`expr_shy`、`whale_tail_only` —— 分别对应阅读器陪伴菜单、阅读成就庆祝、成功 Toast、设置害羞态、加载动画尾巴。

## 构建
`gradlew assembleDebug` → BUILD SUCCESSFUL（仅预存 Material 图标弃用警告）。
APK：`builds/app_45/outputs/apk/debug/app-debug.apk`

## 安装（vivo 实测）
- `adb install -r` 被 vivo「USB 安装」拦截（`INSTALL_FAILED_ABORTED`）
- 已改用：`adb push` 到 `/sdcard/Download/` + 拉起系统安装器，走常规「未知来源」授权
- **请在手机弹出的安装界面点「安装」**（若提示允许安装未知应用，点允许即可）
