# 冲浪阅读 v2.0 · Phase 1 工程概览

> 阶段目标：建立统一美术资产文件夹 + 全局视觉基调（Ocean 深海底色 + 玻璃拟态组件）
> 日期：2026-08-15 · 状态：编译通过 ✅

## 一、统一美术资产文件夹

`app/src/main/assets/art/` 下：

```
art/
├── character/   character_full / half / sitting / empty / error / celebrate
│               avatar_circle / expr_happy / expr_shy / whale_tail_only   (9 张)
├── background/  bg_deep_ocean / bg_splash                              (2 张)
├── book_cover/  (.gitkeep 预留)
├── effect/      (.gitkeep 预留)
├── icon/        (.gitkeep 预留)
└── animation/   (.gitkeep 预留)
```

- 根目录旧 `assets/` 已清空并删除。
- 放在 `app/src/main/assets/` 而非 `res/drawable/`：避免 1024×1536 大图按密度缩放糊化；Coil 用 `androidAsset://art/character/xxx.png` 直接加载。
- 后续集成示例：`AsyncImage(model = "androidAsset://art/character/character_full.png", ...)`。

## 二、全局视觉基调（Ocean 主题）

| 文件 | 改动 |
|------|------|
| `ui/theme/OceanColors.kt`（新增） | `WhaleColors`（主色/中性/文字/语义/玻璃表面/5 套渐变 Brush）+ `WhaleRadius`（xs4~full999） |
| `ui/theme/Theme.kt`（重写） | LightColors/DarkColors 改为 Ocean 深海底色：DARK=`#142836` 背景 + `whale-blue #5B9FDA` 主色；LIGHT=日间海洋变体 |
| `ui/components/WhaleGlassCard.kt`（新增） | `whaleGlassCard()` Modifier + `WhaleGlassCard` 容器（半透明深色 + 1dp 白边，毛玻璃观感） |
| `ui/components/WhaleButton.kt`（新增） | 胶囊渐变 CTA 按钮（`whale-blue → #4A8EDE`） |
| `res/values/colors.xml` | `primary` 改 `#142836`，原生状态栏/导航栏融入深海 |
| `ui/AppRoot.kt` | `SYSTEM` 分支默认强制暗色（品牌默认沉浸式暗色；设置内「浅色」仍可用） |

**铁律守住**：`Theme.kt` 中的 `ReaderColors` / `ReaderThemes` / `ReaderBodyFont`（阅读器配色）原样保留，未动。

## 三、可验证效果（现在就能看到）

- 所有页面背景、顶栏（`AppTopBar` 用 `colorScheme.primary`）、底栏、文字颜色**立即**变为深海底色 + 鲸蓝主色系。
- Splash 渐变背景自动变为 `whale-blue → whale-navy`（原 `primary→primaryContainer` 映射）。
- `WhaleGlassCard` / `WhaleButton` 组件已就绪，供 Phase 2–4 页面直接调用。

## 四、已知取舍

- **玻璃卡片暂用「半透明 + 细边框」**营造毛玻璃观感；Compose 无原生 backdrop-blur，真正的 RenderEffect 模糊留待 Phase 4（API 31+ 才稳）。
- **字体未换包**：维持系统默认字体（规范里的 Noto Sans/Serif SC、JetBrains Mono 需要打包字体文件，增 APK 体积，留待 M5 发布阶段）。
- `AppRoot` 里 `isSystemInDarkTheme` 导入不再使用（无害警告）。

## 五、下一步（Phase 2）

Splash 重写（小鲸立绘 + 入场动画）+ 首页重写为「个人阅读空间」+ 小鲸资产集成（Coil 加载 `androidAsset://art/...`）。
待办资产：`logo_wordmark`（SVG/Compose Text）、`icon_set`（SVG Vector Drawable）。
