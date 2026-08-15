# 冲浪阅读 · 产品设计蓝图（七猫式流量平台 / 风险隔离优先）

> 版本：v1.0（蓝图稿，待拍板）
> 日期：2026-08-15
> 基于：`BACKEND-PLAN.md`（5 条铁律已确认、M1 已完成、0.5.9 进行中）
> 视角：软件开发团队（SoftwareCompany）为产品方完善的产品设计蓝图

---

## 0. 蓝图定位与阅读指引

本文把产品方提出的 **「主站=起点（原创生产）× 冲浪阅读=七猫（免费阅读 + 广告变现 + 互动社区 + 多渠道分发）」** 定位，落成可落地的后端设计。

- **第一性原理（最重要）**：采用七猫式架构，首要目的是**风险隔离与稳定**，其次才是流量收益。详见 §1。
- **核心铁律不变**：内容（正文 / 作者库 / 章节库 / 分类元数据）只在主站与书源，冲浪阅读**只读消费**；业务（用户 / 行为 / 聚合 / 广告）只在冲浪阅读后端，主站**零改动**。
- **最大演进**：后端从「书架/进度云同步工具」升级为「七猫式流量平台」——新增榜单、月票、评分、评论、社区、广告变现、推荐分发等能力。
- **关键认知**：七猫式功能（月票/评分/评论/榜单/社区）本质都是对**用户行为**的聚合，数据里**不含任何小说正文**，因此与铁律完全兼容。

---

## 1. 设计第一性原理：物理隔离是为了稳定与安全

> 这一节是整份蓝图的灵魂。业务模式是表象，**风险隔离才是动机**。

### 1.1 主站的痛点：一个不敢碰的耦合单体

主站（冲浪中文网 / `novel-site`）在多年演进中，代码已经高度缠绕：

- 作者后台、书籍库、月票、排行榜、评论区、书友圈……**彼此指向性耦合**；
- 动任何一处，都可能引发连锁 bug；
- 结果：**已半年不敢更新主站**，任何大改都面临"牵一发动全身"的风险。

这不是个别 Bug，而是**架构层面的脆弱性**——所有功能挤在同一个数据库、同一套部署、同一个出错域里。

### 1.2 七猫带来的启示：两个独立后端，只共享"同一本书"

研究七猫后发现一个关键事实：

- 七猫上有大量来自其他平台的知名书籍，但它拥有**完全独立的全套小说网站后端体系**（榜单 / 月票 / 评论 / 社区都是自己的）；
- 七猫后端出问题，**只局限于七猫本身**，绝不影响起点网站后端；
- 例：七猫上一本书打不开详情页，**起点的原书照样能打开**——因为这是两套不同后端的网站，执行了**最彻底的物理隔离**；
- 两者唯一的共通点：**两个独立站点都分发同一本书**。你在起点能看到，在七猫也能看到，读者自行选择去哪。

### 1.3 我们的设计目标（按优先级）

| 优先级 | 目标 | 含义 |
|---|---|---|
| **P0** | **隔离风险** | 冲浪阅读后端是独立第二后端，任何故障被封死在自身边界内，绝不波及主站 |
| **P0** | **稳定与安全** | 不碰主站一行代码、不共享库、不共享凭据，把主站从"不敢动"中解放出来 |
| **P1** | **全新设计** | 在不触碰主站的前提下，从零设计一套现代后端（账号/互动/广告/社区） |
| **P2** | **流量收益** | 免费阅读 + 广告变现 + 多渠道分发（这是七猫商业模式的红利，顺带享受） |

### 1.4 故障域模型（隔离契约）

这是七猫式架构能成立的工程保证——**两个故障域互不可达**：

```
┌─────────────── 故障域 A：主站（起点式）───────────────┐
│  Neon 主站库 + Vercel 主站项目 + 主站 Auth            │
│  崩了 → 冲浪阅读"阅读/书架/进度"仍可用                 │
│         （只要书源 API 不挂；书源挂时 App 靠本地缓存降级）│
└───────────────────────┬─────────────────────────────┘
                         │  书源 API（公开只读契约，变更需审批）
                         │  ⛔ 冲浪阅读永不回写、永不迁移主站
                         ▼
┌─────────────── 故障域 B：冲浪阅读后端（七猫式）────────┐
│  独立 Neon project + 独立 role + 独立 Vercel + 独立仓库 │
│  崩了 → 主站照常；App 降级为本地阅读（Room 缓存）       │
│  榜单崩/评论崩/广告崩 → 全部封死在 B 内，不波及 A        │
└───────────────────────────────────────────────────────┘
```

**契约要点**：
1. 唯一连接 = 书源只读 API；冲浪阅读对主站只有"读"，没有"写"。
2. 书源 API 的字段/结构变更，须走审批（沿用 ROADMAP 铁律），因为主站改这两个接口会影响 App。
3. 两个故障域物理隔离（独立 project / role / 凭据 / 部署），即使 B 的凭据泄漏也碰不到 A。
4. 未来若要把冲浪阅读的聚合数据（榜单/评论）回流主站展示，**默认不做**；如要做，也是单向、异步、可熔断，绝不反向耦合。

### 1.5 双产品生态图

```
        ┌──────────────── 故障域 A：主站（起点式·原创生产）────────────────┐
        │  作者发书 → 分卷章节 → 审核发布                                   │
        │  产出：书名/作者/简介/封面/分类/状态/字数/章节/正文/更新           │
        └───────────────────────────┬─────────────────────────────────────┘
                                     │  书源 API（公开只读：/api/books, /api/book-source）
                                     │  ⛔ 冲浪阅读永不回写主站、永不改主站结构
                                     ▼
        ┌──────────── 故障域 B：冲浪阅读 App（七猫式·流量入口）────────────┐
        │  书城/书架/阅读器/榜单/评论/社区/广告位                           │
        └───────────────┬──────────────────────────────┬─────────────────┘
                        │  HTTPS 业务读写（带 token）      │  曝光/点击（匿名可）
                        ▼                                 ▼
        ┌──── 冲浪阅读 Backend（独立 Neon + Vercel）────┐   ┌── 广告主/买量渠道 ──┐
        │ L1 基础：账号/书架/进度/公告/反馈/统计/AI报告  │   │ 自有交叉推书（优先） │
        │ L2 平台：榜单/月票/评分/评论/社区/广告/推荐分发│◀─▶│ 第三方广告（后期）   │
        │ ⛔ 业务库中永不出现小说正文/章节/作者库         │   │ 主站导流/社媒/SEO    │
        └──────────────────────────────────────────────┘   └────────────────────┘
```

---

## 2. 产品定位：双产品（起点 × 七猫）

### 2.1 一句话定位

> 主站（xiaoswz.vercel.app）= 起点中文网式的**原创内容工厂**；
> 冲浪阅读（安卓 APP + 独立后端）= 七猫式的**免费阅读流量入口与变现平台**。

### 2.2 为什么这个模型成立

| 维度 | 七猫（参考） | 冲浪阅读（本产品） |
|---|---|---|
| 内容来源 | 聚合第三方书源，原创很少 | **完全自有原创**，零版权成本 |
| 阅读模式 | 免费 + 广告，无付费墙 | 免费 + 广告，无付费墙 |
| 变现 | 广告曝光/点击差价 | 广告差价 + **自有交叉推书（零成本）** |
| 增长 | 买量（广告投放获客） | 主站导流 + 自有推书 + 社媒/SEO |
| 壁垒 | 流量与分发效率 | 自有内容零成本 → **利润率更高、无版权风险** |
| **风险** | 两套后端隔离 | **两套后端物理隔离，故障域互不可达** |

**结论**：冲浪阅读的价值不在「内容生产」，而在「内容分发效率」与「用户时长 / 广告库存」。后端要服务好这个目标，同时**绝不把脆弱性带回主站**。

---

## 3. 七猫式能力拆解 → 冲浪阅读映射

| 七猫能力 | 冲浪阅读落地 | 数据归属 | 里程碑 |
|---|---|---|---|
| 免费阅读 | 全部内容免费，无付费墙 | 书源 | 已有 |
| 广告变现（开屏/信息流/插页/激励） | 广告位系统 + **自有交叉推书** | 冲浪阅读 | M10 |
| 月票 / 推荐票榜 | `votes` + 每日免费票机制 | 冲浪阅读 | M8 |
| 综合热榜 / 新书 / 完本 / 评分榜 | `book_stats` 聚合 + `leaderboard` 快照 | 冲浪阅读 | M8 / M9 |
| 评分 | `ratings`（1–5，可改） | 冲浪阅读 | M9 |
| 评论区（楼中楼 / 点赞） | `comments` + 审核 | 冲浪阅读 | M9 |
| 书友圈 / 社区 | `community_posts` / `community_replies` | 冲浪阅读 | M11 |
| 个性化推荐流 | 行为画像 + 推荐服务 | 冲浪阅读 | M12 |
| 多渠道分发 / 买量 | `user_channels` 归因 + 自有交叉推广 | 冲浪阅读 | M12 / M13 |
| 运营后台 / 数据看板 | 内部 admin 聚合 | 冲浪阅读 | M4 / M13 |

---

## 4. 后端架构演进

### 4.1 模块分层（两层）

- **L1 基础业务层（M1–M7，已规划）**：账号 / 书架 / 进度 / 公告 / 配置 / 反馈 / 阅读统计 / AI 月报。
- **L2 流量平台层（M8–M13，本蓝图新增）**：榜单·月票 → 评分·评论 → 广告变现 → 社区 → 推荐·分发 → 运营看板。

### 4.2 隔离边界（不变，且是 P0）

- 冲浪阅读后端仍**独立 Neon project + 独立 role + 独立 Vercel 项目**（物理隔离，非逻辑隔离）。
- 书源只读；广告 / 互动 / 聚合全在自有库；部署 env 绝不注入主站 `DATABASE_URL`。
- 仓库根目录保留 `AI-DEV-RULES.md`，强制 AI 在该仓库工作时连主站数据库在哪都不知道。

### 4.3 推荐的实现顺序原则

1. 先做「展示型」能力（榜单、统计、广告位曝光）——可匿名运行，不依赖登录。
2. 再做「互动型」能力（投票、评分、评论、社区）——**强依赖 M5 账号升级**。
3. 因此 **M5 账号升级必须由「远期」提前到 L2 之前**（见 §7 依赖调整）。

---

## 5. 数据模型扩展（新增模块，全部只存业务）

> 在 `BACKEND-PLAN.md` 已有的 `users / bookshelf / reading_progress / favorites / app_config / announcements / feedback` 基础上，新增以下表。一律按 `(book_source_id, book_id)` 关联书源，**不建任何小说内容表**。

```sql
-- ╔══════════════════════════════════════════════════════════╗
-- ║ L2 流量平台层：全部只存"业务/行为/聚合"，不存任何小说正文 ║
-- ╚══════════════════════════════════════════════════════════╝

-- book_stats：每本书的聚合指标（榜单的燃料）。按 (book_source_id, book_id) 唯一
CREATE TABLE book_stats (
  book_source_id   text NOT NULL,
  book_id          text NOT NULL,
  title            text,                  -- 冗余缓存用于展示（来自书源，非正文）
  author           text,                  -- 冗余缓存（来自书源）
  cover_url        text,                  -- 仅 http(s)，来自书源
  vote_count       bigint DEFAULT 0,      -- 月票/推荐票累计
  vote_month       int    DEFAULT 0,      -- 本月票（用于月票榜）
  rating_sum       bigint DEFAULT 0,
  rating_count     int    DEFAULT 0,      -- 平均分 = rating_sum / rating_count
  comment_count    int    DEFAULT 0,
  view_count       bigint DEFAULT 0,      -- 阅读/曝光量
  favorite_count   int    DEFAULT 0,
  complete_rate    real   DEFAULT 0,      -- 完读率（由 reading_progress 聚合，未来）
  score_heat       real   DEFAULT 0,      -- 综合热度分（用于热榜）
  last_synced_at   timestamptz,           -- 最近一次从书源刷新元数据
  updated_at       timestamptz DEFAULT now(),
  PRIMARY KEY (book_source_id, book_id)
);

-- votes：月票/推荐票。每日免费票机制（七猫式，无需充值）
CREATE TABLE votes (
  id              uuid PRIMARY KEY,
  user_id         uuid REFERENCES users(id),
  book_source_id  text NOT NULL,
  book_id         text NOT NULL,
  vote_type       text NOT NULL DEFAULT 'monthly',  -- monthly / recommend
  period          text NOT NULL,                    -- 'YYYY-MM' 用于月票月榜
  created_at      timestamptz DEFAULT now()
);
CREATE INDEX idx_votes_book_period ON votes(book_source_id, book_id, period, vote_type);

-- ratings：评分（每用户每书一条，可改）
CREATE TABLE ratings (
  id              uuid PRIMARY KEY,
  user_id         uuid REFERENCES users(id),
  book_source_id  text NOT NULL,
  book_id         text NOT NULL,
  score           smallint CHECK (score BETWEEN 1 AND 5),
  created_at      timestamptz DEFAULT now(),
  updated_at      timestamptz DEFAULT now(),
  UNIQUE (user_id, book_source_id, book_id)
);

-- comments：书籍评论区（楼中楼 + 点赞），status 用于审核
CREATE TABLE comments (
  id              uuid PRIMARY KEY,
  user_id         uuid REFERENCES users(id),
  book_source_id  text NOT NULL,
  book_id         text NOT NULL,
  parent_id       uuid REFERENCES comments(id),   -- null=主楼，否则=楼中楼
  content         text NOT NULL,
  like_count      int DEFAULT 0,
  status          text DEFAULT 'pending',         -- pending/approved/hidden
  created_at      timestamptz DEFAULT now()
);
CREATE INDEX idx_comments_book ON comments(book_source_id, book_id, status, created_at DESC);

-- community_posts：书友圈/社区
CREATE TABLE community_posts (
  id              uuid PRIMARY KEY,
  user_id         uuid REFERENCES users(id),
  category        text,                            -- 水区/书评/同人...
  title           text,
  content         text NOT NULL,
  like_count      int DEFAULT 0,
  reply_count     int DEFAULT 0,
  status          text DEFAULT 'pending',
  created_at      timestamptz DEFAULT now()
);

-- community_replies：社区回复（支持楼中楼）
CREATE TABLE community_replies (
  id              uuid PRIMARY KEY,
  post_id         uuid REFERENCES community_posts(id),
  parent_id       uuid REFERENCES community_replies(id),
  user_id         uuid REFERENCES users(id),
  content         text NOT NULL,
  like_count      int DEFAULT 0,
  status          text DEFAULT 'pending',
  created_at      timestamptz DEFAULT now()
);

-- ── 广告/变现系统 ──
-- ad_creatives：广告创意（自有推书 or 第三方）
CREATE TABLE ad_creatives (
  id              uuid PRIMARY KEY,
  kind            text NOT NULL,        -- 'cross_promo'（推自有书） / 'third_party'
  title           text,
  image_url       text,
  target_url      text,                 -- cross_promo→书源详情；third_party→广告主落地
  book_source_id  text,                 -- cross_promo 时指向被推的书
  book_id         text,
  advertiser      text,
  weight          int DEFAULT 100,      -- 曝光权重
  start_at        timestamptz,
  end_at          timestamptz,
  status          text DEFAULT 'active'
);

-- ad_placements：广告位（开屏/信息流/插页/激励/横幅）
CREATE TABLE ad_placements (
  id              uuid PRIMARY KEY,
  slot            text NOT NULL UNIQUE, -- splash / feed / interstitial / rewarded / banner
  enabled         boolean DEFAULT true,
  strategy        text DEFAULT 'weighted',  -- weighted / round_robin
  max_per_session int
);

-- ad_impressions：曝光（匿名也可，靠 device_id 归因）
CREATE TABLE ad_impressions (
  id              uuid PRIMARY KEY,
  creative_id     uuid REFERENCES ad_creatives(id),
  placement       text,
  user_id         uuid REFERENCES users(id),
  device_id       text,
  channel         text,
  created_at      timestamptz DEFAULT now()
);

-- ad_clicks：点击
CREATE TABLE ad_clicks (
  id              uuid PRIMARY KEY,
  impression_id   uuid REFERENCES ad_impressions(id),
  user_id         uuid REFERENCES users(id),
  created_at      timestamptz DEFAULT now()
);

-- user_channels：安装/来源归因（买量 ROI 衡量）
CREATE TABLE user_channels (
  user_id         uuid PRIMARY KEY REFERENCES users(id),
  install_channel text,                 -- xiaoswz_site / app_store / social / partner_xxx
  referrer        text,
  campaign        text,
  attributed_at   timestamptz DEFAULT now()
);

-- leaderboard_snapshots（可选）：榜单每日快照，避免实时重算
CREATE TABLE leaderboard_snapshots (
  id              uuid PRIMARY KEY,
  board           text NOT NULL,        -- popularity / monthly / rating / new / finished
  rank            int,
  book_source_id  text,
  book_id         text,
  title           text,
  metric          real,                 -- 该榜排序指标值
  snapshot_at     timestamptz DEFAULT now()
);
CREATE INDEX idx_lb_board ON leaderboard_snapshots(board, snapshot_at DESC, rank);
```

**关键约束（对齐铁律）**：
- 所有业务表只引用 `(book_source_id, book_id)`，绝不存储正文 / 章节 / 作者内容。
- `title/author/cover_url` 仅作展示冗余缓存，且 `cover_url` 永远只存 http(s) 链接（绝不 `data:` URI，对齐 SQLiteBlobTooBig 教训）。
- `comments` / `community_*` 含 `status` 审核字段，UGC 上线前需审核机制（见 §9）。

---

## 6. API 设计草案（核心端点）

### L2 展示型（可匿名）

| 方法 | 端点 | 说明 |
|---|---|---|
| GET | `/api/leaderboards/:board?period=` | 榜单（popularity/monthly/rating/new/finished） |
| GET | `/api/books/:src/:id/stats` | 单书聚合（均分/月票/评论数） |
| GET | `/api/ads?slot=feed` | 返回创意（匿名靠 device_id 归因） |

### L2 互动型（需登录，依赖 M5）

| 方法 | 端点 | 说明 |
|---|---|---|
| POST | `/api/votes` | 投月票/推荐票（校验当日余票） |
| GET | `/api/votes/balance` | 我的剩余票 |
| POST | `/api/ratings` | 评分（可改） |
| GET | `/api/ratings?book=` | 平均分 + 我的评分 |
| GET | `/api/books/:src/:id/comments?page=` | 评论列表 |
| POST | `/api/books/:src/:id/comments` | 发评论（含 parent_id 楼中楼） |
| POST | `/api/comments/:id/like` | 评论点赞 |
| GET | `/api/community/posts` | 社区帖子流 |
| POST | `/api/community/posts` | 发帖 |
| GET/POST | `/api/community/posts/:id/replies` | 社区回复 |

### 广告/归因

| 方法 | 端点 | 说明 |
|---|---|---|
| POST | `/api/ads/impressions` | 上报曝光 `{creative_id, slot}` |
| POST | `/api/ads/clicks` | 上报点击 `{impression_id}` |
| POST | `/api/attribution` | 启动上报来源 `{install_channel, referrer, campaign}` |

---

## 7. 里程碑路线图（演进版）

### L1 基础业务层（沿用 BACKEND-PLAN，含一处依赖调整）

| 里程碑 | 内容 | 后端版本 | 状态 |
|---|---|---|---|
| M1 | 基础设施与隔离（独立 Neon + Prisma 建表 + /health） | — | ✅ 已完成 |
| M2 | 设备匿名账号 + 书架/进度上报与拉取 | 0.5.9 | 🔨 进行中 |
| M3 | 云端拉取与多端同步（last-write-wins） | 0.6.0 | 待做 |
| M4 | 公告 / 配置 / 反馈 + 简易后台 | 0.6.1 / 0.6.2 | 待做 |
| **M5** | **账号升级（邮箱/密码/OAuth）+ 匿名绑定迁移** | 0.6.3 / 0.6.4 | **⚠️ 提前**：因 L2 互动强依赖登录，建议紧接 M3 即做 |
| M6 | 阅读统计 + AI 月度报告 | 0.6.5 / 0.6.6 | 待做 |

### L2 流量平台层（七猫式，本蓝图新增）

| 里程碑 | 内容 | 说明 |
|---|---|---|
| M8 | 榜单与月票 | `book_stats` + `votes`（每日免费票）+ `leaderboard` 快照。展示榜可匿名先做；投票需 M5 |
| M9 | 评分 + 评论区 | `ratings` + `comments`（楼中楼 + 审核） |
| M10 | 广告 / 变现 | `ad_*` 表 + **自有交叉推书优先** + 第三方广告位预留 |
| M11 | 社区 / 书友圈 | `community_posts` / `community_replies` |
| M12 | 推荐 + 多渠道分发归因 | `user_channels` + 行为画像推荐服务 |
| M13 | 运营数据看板 + 买量 ROI 闭环 | 内部 admin 聚合看板 |

> **依赖调整（重要）**：原 `BACKEND-PLAN.md` 把 M5 排在 M6 之后（远期）。本蓝图因 L2 互动功能（M8 投票 / M9 / M11）强依赖账号，建议 **M5 提前到 M3 之后**，否则 L2 无法开工。请在拍板时确认。

---

## 8. 商业模型与变现闭环

- **自有内容零成本** → 同样的"免费 + 广告"模型，利润率高于七猫（无版权采购）。
- **自有交叉推书（cross_promo）= 零成本广告**，应优先做：在书城/章节间推其他自有书，提升分发效率与用户时长，不依赖第三方。
- **第三方广告（穿山甲 / 优量汇 / AdMob）** = 后期接入，作为增量收入；先建好 `ad_*` 表与曝光/点击上报，再接 SDK。
- **买量增长飞轮**：主站 SEO/社媒导流 + 自有推书 → 拉新 → 免费阅读产生广告库存 → 广告收益反哺买量。
- **数据看板（M13）** 衡量每渠道 ROI，让"分发效率"可量化。

---

## 9. 风险与开放问题（待拍板）

| # | 决策点 | 本蓝图的默认建议 | 需你拍板 |
|---|---|---|---|
| 1 | 广告策略优先级 | 自有交叉推书先，第三方 SDK 后 | □ |
| 2 | 月票机制 | 每日免费赠送票（七猫式，无充值） | □ |
| 3 | 主站回流 | 默认不回流；如做则单向异步可熔断 | □ |
| 4 | **M5 账号提前** | 提前到 M3 之后 | □（建议确认） |
| 5 | UGC 审核合规 | 评论/社区先审后放（status 流）或先放后审 | □ |
| 6 | 推荐系统复杂度 | M12 先做轻量规则推荐，模型推荐远期 | □ |
| 7 | 分类来源 | 只读书源分类，冲浪阅读只存分类级聚合计数 | □ |

---

## 10. 下一步建议

1. **收尾 L1 的 0.5.9（M2）**——当前已在做，先把"匿名账号 + 书架/进度上报拉取"闭环跑通验证。
2. **推进 M5 账号升级**——为 L2 铺路（互动功能强依赖登录）。
3. **L2 从 M8 榜单月票起步**——纯聚合，展示榜可匿名运行，先做出"热榜/月票榜"提升分发效率，投票等登录后再开。
4. 每个里程碑独立可用、独立验证，保持**故障域隔离**——任何一步出问题都不波及主站。

---

> 本文档随设计讨论演进维护。所有改动均遵守「内容归主站、业务归冲浪阅读、主站零改动」铁律。
