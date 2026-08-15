# 冲浪阅读后端 — 架构与版本拆解方案

> 版本：v0.1（方案稿，待拍板）
> 日期：2026-08-15
> 定位：**冲浪阅读专属后端 / 独立产品后端**，主站仅作为其"内容源/书源"。

---

## 〇、先拍板的五条铁律（本次方案的根基）

1. **主站业务数据库绝对不动**：不共享连接、不共享表、不共享凭据、不跑主站迁移。
2. **内容归书源，业务归冲浪阅读**：小说正文、作者库、章节库、分类库**永不进入**冲浪阅读数据库。
3. **冲浪阅读数据库 = App 业务库**：只存用户、书架、进度、收藏、配置、公告、反馈、统计。
4. **独立账号体系**：不复用主站 Auth，避免反向耦合主站。
5. **AI 开发权限隔离**：后端仓库/项目与主站物理分离，AI 在该仓库工作时**不持有主站任何凭据**。

> 核心判断（已确认）：书源已能稳定提供 书名/作者/简介/封面/章节列表/章节正文/更新状态，
> 因此冲浪阅读只需"消费书源 + 存自己的 App 业务数据"，完全没必要重建小说库。

---

## 一、系统拓扑（三层）

```
                INTERNET
                   │
                   ▼
         ┌──────────────────┐
         │      主站         │
         │ 主站 API          │
         │ 主站 DB（不动）   │
         └────────┬─────────┘
                  │  书源 / 公开只读 API
                  ▼
   ┌──────────────────────────────────────┐
   │           冲浪阅读 App                │
   │  UI · 阅读器 · 书源管理 · 本地缓存    │
   │  （Retrofit 调书源 + 调 Reader API） │
   └───────────────┬──────────────────────┘
                   │  HTTPS（带 token）
                   ▼
         ┌──────────────────┐
         │ 冲浪阅读 Backend  │  ← 新项目（独立仓库/独立 Vercel）
         │ Reader API        │
         └────────┬─────────┘
                  │
                  ▼
         ┌──────────────────┐
         │ 冲浪阅读 Database │  ← 独立库 + 独立 role
         │ users            │
         │ bookshelf        │
         │ reading_progress │
         │ favorites        │
         │ app_config       │
         │ announcements    │
         │ feedback         │
         └──────────────────┘
```

要点：**App 同时消费"书源"和"Reader API"两条线，但两者职责不同**——
书源给内容，Reader API 给"我的数据"。

---

## 二、隔离方案（推荐 vs 备选）

### 推荐：物理隔离（独立 Neon Project + 独立 Vercel Project + 独立 Role）
- 新建 Neon project `chonglang-reader`，得到独立 `DATABASE_URL`。
- 在该 project 内建专属 role `reader_app`（非 owner），只 `GRANT` 到 reader 库。
- 新建独立 Git 仓库 `xiaoswz-reader-backend` + 独立 Vercel 项目。
- 优点：计算资源、凭据、部署全隔离；即使凭据泄漏也碰不到主站 project。
- 缺点：多一个 project 要管（成本极低，Neon 免费额度够用）。

### 备选：同 Project 逻辑隔离（独立 database + 独立 role）
- 在同一 Neon project 建 `chonglang_reader_db` 库 + `reader_user` role，
  `REVOKE ALL ON DATABASE neon_db FROM reader_user`，仅 `GRANT` 新库。
- 优点：零额外 project，运维简单。
- 缺点：同 project 共享计算资源；隔离强度弱于物理隔离。

### 否决（绝不采用）
- ❌ 在主站 schema 里加表
- ❌ 复用主站 `DATABASE_URL`
- ❌ 复用主站 Prisma client / 主站 Auth
- ❌ 让后端迁移脚本能触达主站库

---

## 三、技术栈建议（复用主站栈，降低维护成本）

| 层 | 选型 | 说明 |
|---|---|---|
| 后端框架 | **Next.js (App Router) + TypeScript** | 与主站一致，团队/AI 都熟 |
| ORM | **Prisma** | 与主站一致，迁移可控 |
| 数据库 | **Postgres (Neon)** | 独立 project |
| API 风格 | **REST JSON** | 与现有书源 API 风格一致，App 用 Retrofit 直接对接 |
| 认证 | 自签 JWT / 会话 token（见 M2/M5） | 独立账号，不碰主站 Auth |
| 部署 | **独立 Vercel 项目** | 域名/Env/凭据全隔离 |
| 缓存 | 先用内存，必要时 Upstash Redis | 与主站一致 |

> 注：框架只是建议，最终以"待拍板"决策为准。若你更想要轻量，可换 FastAPI/Express，
> 但 Next.js 能让 AI 复用主站大量现成模式，长期更省事。

---

## 四、数据模型（只存 App 业务，不存小说内容）

```sql
-- users：设备匿名账号先行，邮箱账号后续可绑定
users (
  id            uuid pk,
  device_id     text unique null,   -- 匿名登录用
  email         text unique null,   -- M5 绑定
  password_hash text null,          -- M5 绑定
  display_name  text,
  avatar_url    text null,
  created_at    timestamptz default now(),
  last_seen_at  timestamptz
);

-- bookshelf：只存 书源标识 + 书ID + 轻量元数据，不存正文
bookshelf (
  id            uuid pk,
  user_id       fk,
  book_source_id text,             -- 例如 "xiaoswz"
  book_id       text,              -- 书源返回的书 slug
  title         text,
  author        text null,
  cover_url     text null,         -- 仅存 http(s) URL，绝不存 data: URI
  status        text default 'reading',  -- reading/finished/plan
  added_at      timestamptz,
  unique(user_id, book_source_id, book_id)
);

-- reading_progress：云端进度，换机可恢复
reading_progress (
  user_id        fk,
  book_source_id text,
  book_id        text,
  chapter_id     text null,
  chapter_index  int,
  progress_percent int,            -- 0-100
  updated_at     timestamptz,
  primary key(user_id, book_source_id, book_id)
);

-- favorites
favorites (
  id             uuid pk,
  user_id        fk,
  book_source_id text,
  book_id        text,
  created_at     timestamptz,
  unique(user_id, book_source_id, book_id)
);

-- app_config：公告/功能开关/更新提示
app_config (
  key         text pk,
  value       jsonb,
  updated_at  timestamptz
);

-- announcements
announcements (
  id           uuid pk,
  title        text,
  body         text,
  level        text,               -- info/warning/update
  published_at timestamptz,
  expires_at   timestamptz null
);

-- feedback
feedback (
  id         uuid pk,
  user_id    fk null,
  type       text,
  content    text,
  contact    text null,
  status     text default 'new',
  created_at timestamptz
);

-- crash_logs（M3+ 可选）
crash_logs (
  id          uuid pk,
  user_id     fk null,
  app_version text,
  device      text,
  message     text,
  stack       text,
  created_at  timestamptz
);
```

**关键约束**：
- `cover_url` 永远只存 http(s) 链接（来自书源），**绝不存 data: URI**——
  这正是之前书架崩溃（SQLiteBlobTooBig）的教训在云端的翻版，Postgres 虽不限游标窗口，但大字段会拖慢同步、浪费带宽。
- 小说正文、章节内容、作者库、分类库**一张表都不建**。

---

## 五、版本拆解（核心交付）

### M1 — 基础设施与隔离（后端地基）【不动主站】
| 项 | 内容 |
|---|---|
| 目标 | 一个能部署、能读写、与主站零耦合的空后端 |
| 交付 | 独立 Neon project + 独立 role；独立仓库 + Vercel 项目；Prisma schema 建好上表；`GET /health` + 占位 CRUD |
| App 端 | 暂不改（仅确认新 API 域名可达） |
| 风险 | **低**（完全不碰主站） |
| 验收 | ① POST/GET 一条 bookshelf 成功 ② 主站 DB 无任何改动 ③ 后端 env 不含主站 DATABASE_URL |

### M2 — 设备匿名账号 + 书架/进度上报
| 项 | 内容 |
|---|---|
| 目标 | 用户打开 App 自动拿到一个匿名身份，本地书架可上报云端 |
| 交付 | `POST /auth/device`（device_id → token）；`POST /bookshelf`(批量)、`POST /progress` |
| App 端 | 启动静默登录；登录后把本地 Room 书架上报到云端（首次本地→云同步） |
| 风险 | 中（首次本地→云同步的去重/冲突） |
| 验收 | 换设备前上报的数据，登录同 device 可拉回 |

### M3 — 云端拉取与多端同步
| 项 | 内容 |
|---|---|
| 目标 | 换机/重装可恢复书架与进度 |
| 交付 | `GET /bookshelf`、`GET /progress`；冲突策略 last-write-wins（按 `updated_at`） |
| App 端 | 启动时拉云端合并进本地 Room；进度实时回写 |
| 风险 | 中 |
| 验收 | 手机 A 读到第 327 章 → 手机 B 登录同账号恢复到 327 章 |

### M4 — 公告 / 配置 / 反馈
| 项 | 内容 |
|---|---|
| 目标 | App 具备运营能力（公告、开关、收集反馈） |
| 交付 | `GET /announcements`；`GET /app_config`（更新提示/功能开关）；`POST /feedback` + 简易后台查看页 |
| App 端 | 设置页/启动页消费公告与配置；反馈入口 |
| 风险 | 低 |
| 验收 | 后台发一条公告，App 下次启动可见 |

### M5 — 账号升级（邮箱/密码 或 OAuth）【安全敏感】
| 项 | 内容 |
|---|---|
| 目标 | 匿名账号可绑定邮箱，跨设备用账号登录而非 device |
| 交付 | `POST /auth/register`、`POST /auth/login`；匿名→邮箱绑定迁移 |
| 风险 | 中（密码哈希、token 安全，需谨慎） |
| 依赖 | M2 匿名体系先立住 |

### M6 — 阅读统计 + AI 阅读报告【你提的 AI 化】
| 项 | 内容 |
|---|---|
| 目标 | 聚合阅读记录，生成阅读画像/月度报告 |
| 交付 | 统计接口（类型占比、字数、时长）；AI 月度报告（可接硅基流动） |
| 风险 | 中（涉及 AI 外部调用） |

### M7 — 远期：社区 / 成就 / 看板娘 / 个性化主页
- 风险高，留给未来；全部属于冲浪阅读自身业务，不污染主站。

---

## 六、AI 权限隔离规则（写入开发规则，强制执行）

> 后端仓库根目录放 `AI-DEV-RULES.md`，并在每次让 AI 工作时先读：

```
1. 禁止访问主站数据库。
2. 禁止修改主站数据库结构或执行主站迁移。
3. 禁止复用主站数据库连接凭据。
4. 所有数据库迁移只能作用于冲浪阅读数据库。
5. 后端仓库与主站仓库物理分离；在该仓库工作时 AI 不持有主站任何凭据。
6. 部署用独立 Vercel 项目，env 绝不注入主站 DATABASE_URL。
```

这样以后让 AI "加个收藏夹功能"，它只能在 `chonglang_reader_db` 里 `CREATE TABLE`，
**连主站数据库在哪都不知道**。

---

## 七、关键工程原则（贯穿所有版本）

1. **内容 vs 业务严格分离**：小说正文/作者库/章节库永不进 reader DB。
2. **封面只存 URL**：绝存 data: URI（对齐 SQLiteBlobTooBig 教训）。
3. **本地 Room 保留作离线缓存**：云端同步是增强，不是替代；无网也能读。
4. **versionCode 单调**：更新通道独立（`lan-update` 已有），不与主站混淆。
5. **Reader API 与书源 API 域名分离**：物理/逻辑上都是两条独立服务。

---

## 八、已确认决策（2026-08-15 拍板）

| # | 决策点 | 结论 |
|---|---|---|
| 1 | 隔离级别 | **独立 Neon Project（物理隔离）** + 独立 role |
| 2 | 后端仓库 | **独立仓库 `xiaoswz-reader-backend`** + 独立 Vercel 项目 |
| 3 | 认证起点 | **设备匿名账号先行**（device_id 静默登录，M5 再绑邮箱） |
| 4 | 后端框架 | **Next.js (App Router) + TypeScript + Prisma** |

> 决策逻辑：最大化隔离、最小化首版复杂度、复用主站技术栈降低长期维护成本。
> 确认后 M1 即可开工——M1 不动主站，风险最低，适合先跑通"独立后端 + 独立库 + 能读写"闭环。

## 九、版本号约定（2026-08-15 确立）

> 后端采用**独立版本线**，不与安卓 App 的 0.5.x 混淆。
> 安卓 App 当前 0.5.8；后端首个可用版本承接为 **0.5.9**，之后 **0.6.0~0.6.9 全部用于围绕后端的迭代**。

| 后端版本 | 对应里程碑 | 内容 |
|---|---|---|
| **0.5.9** | M1 + M2 | 后端地基（独立 Neon + Prisma 建表 + /health）+ 匿名账号 + 书架/进度上报与拉取（首个可用版本） |
| **0.6.0** | M3 | 多端同步：云端拉取、冲突策略 last-write-wins |
| **0.6.1** | M4(上) | 公告 `GET /announcements` + 配置 `GET /app_config` |
| **0.6.2** | M4(下) | 反馈 `POST /feedback` + 简易后台查看页 |
| **0.6.3** | M5(上) | 邮箱注册/登录 `POST /auth/register`、`/auth/login` |
| **0.6.4** | M5(下) | 匿名→邮箱绑定迁移 |
| **0.6.5** | M6(上) | 阅读统计接口（类型占比/字数/时长） |
| **0.6.6** | M6(下) | AI 月度阅读报告（接硅基流动） |
| **0.6.7~0.6.9** | M7 起步 | 社区/成就/看板娘/个性化主页（远期，按需） |

**GitHub 定位**（重要）：GitHub 仅作**本地稳定后的备份存档**，不是推进重心。
每完成一个本地稳定版本（如 0.5.9），再 push 备份；平时重心在本地把功能做出来并验证。

---

## 十、当前进度

- ✅ **M1 已完成**（2026-08-15）：独立 Neon project `chonglang-reader`（DB `chonglang`，Singapore，PG18）建好；独立仓库 `xiaoswz-reader-backend` 脚手架 + Prisma schema 建表 + `/api/health`；`AI-DEV-RULES.md` 隔离规则；Pooled/Direct 两条连接读写验证通过；`next build` 通过。**主站数据库零改动。**
- 🔨 **0.5.9 进行中**：M2 接口层已实现（`/api/auth/anon`、`/api/bookshelf` GET/POST/DELETE、`/api/progress` GET/PUT），待本地构建 + HTTP 全链路验证。
