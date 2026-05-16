# FitCoach 技术文档

## 1. 文档说明

### 1.1 文档目的

本文档系统化说明 `FitCoach` 项目的技术实现方案、系统架构、模块划分、核心流程、接口设计、数据结构、部署方式、测试建议与当前实现边界。文档面向以下读者:

- 项目开发者
- 指导教师或评审人员
- 后续接手维护的同学
- 需要基于该系统继续扩展功能的团队成员

文档中所有声明都已经与代码逐项核对,标注出"已实现"与"占位/未闭环"两类边界,避免读者对功能成熟度产生误判。

### 1.2 项目定位

FitCoach 是一个面向居家健身场景的 AI 陪练系统,采用前后端分离架构,核心特点如下:

- 浏览器端实时姿态识别,无需上传视频
- 支持训练计数、动作纠错与多维评分(节奏/稳定/深度/对称/完成率)
- 支持训练数据本地缓存与离线优先使用
- 支持训练记录管理、计划系统、排行榜、社交动态、徽章和管理后台
- 预留 AI 教练接口,用于训练后生成个性化反馈
- 前端具备 PWA 安装和离线缓存能力

### 1.3 项目目录

```text
一级项目/
├─ home-fitness-fullstack/                主项目(前后端分离全栈版)
│  ├─ frontend/                            Vue 3 前端 + Vite
│  ├─ backend/                             Spring Boot 3.2 后端
│  ├─ docker-compose.yml                   容器编排(backend + frontend)
│  └─ 启动.bat                              Windows 一键启动脚本
├─ home-fitness-pwa/                       早期纯前端 PWA 参考实现(默认不修改)
├─ picture/                                页面截图与验收素材
├─ README.md
├─ 项目完成度清单.md
└─ FitCoach技术文档.md                     本文件
```

### 1.4 当前结论

从代码仓库实际状态看,本项目已经具备完整的"系统设计与实现类"项目基础,尤其训练核心链路、前后端骨架、数据结构和展示模块已经成型。但有以下几条需要明确:

- 训练核心链路(摄像头 → MediaPipe → 计数 → 评分 → 本地落库)是真正可运行的
- 后端绝大多数业务接口是真实落地的(基于 JPA 持久化与查询),并非纯占位
- 前端部分页面(登录、动态、排行榜、计划、管理后台、个人统计)仍使用 hardcoded mock 数据,未真正调用后端接口
- AI 教练、短信验证码、邮箱验证码、微信登录、挑战赛持久化等模块仍处于 mock 或 501 占位状态

因此,本文档对每一个模块都同时标明"已实现能力"和"当前边界"。

## 2. 系统总体概述

### 2.1 业务目标

系统希望解决居家健身中常见的几个问题:

- 用户独自训练时缺乏动作指导
- 训练节奏、稳定性和完成度难以量化
- 缺少连续激励机制,难以长期坚持
- 传统健身 App 在线依赖强,弱网或离线体验差

FitCoach 的设计思路是将"浏览器端视觉识别 + 本地数据能力 + 轻量后端服务 + AI 反馈"组合为一个可演示、可扩展的智能陪练系统。

### 2.2 总体技术路线

| 类别 | 选型 |
|---|---|
| 前端框架 | Vue 3.4(Composition API)+ Vite 5 |
| 路由 / 状态 | Vue Router 4 + Pinia 2 |
| 网络 | Axios 1.7 |
| 姿态识别 | MediaPipe Pose 0.5(浏览器端,CDN 加载) |
| 摄像头 | MediaPipe `camera_utils` 0.3 |
| 语音与节拍 | Web Speech `SpeechSynthesis` + Web Audio `AudioContext` |
| 本地存储 | IndexedDB(训练记录)+ LocalStorage(配置/登录态) |
| 离线能力 | Service Worker(缓存策略路由)+ Web App Manifest |
| 后端框架 | Spring Boot 3.2.5(Java 17)+ Spring MVC |
| 安全 | Spring Security + JJWT 0.12.5(无状态 JWT) |
| ORM | Spring Data JPA + Hibernate(`ddl-auto: update`) |
| 数据库 | 开发环境 H2(内存)、生产环境 MySQL |
| 接口文档 | SpringDoc OpenAPI 2.3.0 / Swagger UI |
| AI 接口 | 占位 Provider:`mock` / `claude` / `openai`,默认 `mock`,模型默认 `claude-haiku-4-5-20251001` |
| 容器化 | Docker + docker-compose(多阶段构建,Maven/Node 构建后产物分别放入 jre/nginx 镜像) |

### 2.3 系统总体架构

```text
                ┌────────────────────────────────────────┐
                │                  浏览器                  │
                │ ┌────────────────────────────────────┐ │
                │ │  Vue 3 Application (SPA + PWA)     │ │
                │ │  · views / components / router      │ │
                │ │  · stores: auth · config · training │ │
                │ └────────────────────────────────────┘ │
                │ ┌──────────────┐  ┌──────────────────┐ │
                │ │ 训练核心模块   │  │ 本地数据层         │ │
                │ │ pose.js      │  │ IndexedDB         │ │
                │ │ exercise.js  │  │  └ sessions       │ │
                │ │ voice.js     │  │  └ sync_queue     │ │
                │ │ poster.js    │  │ LocalStorage      │ │
                │ │              │  │ Service Worker    │ │
                │ └──────────────┘  └──────────────────┘ │
                │            │                │           │
                │            ▼                ▼           │
                │   MediaPipe Pose CDN   Axios + JWT      │
                └────────────────────────────────────────┘
                                        │
                                        │ /api  (Vite dev proxy / nginx 反代)
                                        ▼
                ┌────────────────────────────────────────┐
                │          Spring Boot 后端 (8080)        │
                │  JwtAuthFilter ─► SecurityFilterChain    │
                │  ┌───────────────────────────────────┐  │
                │  │ Controller (15 个域,带 Swagger Tag) │  │
                │  │ ─ auth · user · session · coach    │  │
                │  │ ─ plan · badge · leaderboard       │  │
                │  │ ─ social · exercise · admin        │  │
                │  └───────────────────────────────────┘  │
                │  Service ──► Repository (JPA)            │
                │  GlobalExceptionHandler ──► ApiResult   │
                └────────────────────────────────────────┘
                                        │
                                        ▼
                              H2(dev) / MySQL(prod)
```

## 3. 技术栈说明

### 3.1 前端技术栈

前端项目根目录为 `home-fitness-fullstack/frontend/`。`package.json` 实际依赖:

```jsonc
"dependencies": {
  "vue":        "^3.4.27",
  "vue-router": "^4.3.2",
  "pinia":      "^2.1.7",
  "axios":      "^1.7.2"
},
"devDependencies": {
  "@vitejs/plugin-vue": "^5.0.5",
  "vite":               "^5.2.11"
}
```

特别说明:**MediaPipe Pose 与 camera_utils 不通过 npm 依赖**,而是在 `index.html` 中通过 CDN script 直接加载:

```html
<script src="https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.5.1675469404/pose.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@mediapipe/camera_utils@0.3.1675466862/camera_utils.js"></script>
```

`pose.js` 模块内部以 `window.Pose` / `window.Camera` 访问。这种做法的好处是首次访问后 Service Worker 会缓存 CDN 资源,加快后续启动速度;缺点是首屏依赖外网。

`vite.config.js` 关键配置:

- 路径别名 `@` → `./src`(`fileURLToPath(new URL('./src', import.meta.url))`)
- 开发服务器监听 `0.0.0.0:5173`,代理 `/api → http://localhost:8080`(`changeOrigin: true`)
- 构建目标 `es2018`,关闭 sourcemap,chunk 大小警告阈值 1500KB

### 3.2 后端技术栈

后端项目根目录为 `home-fitness-fullstack/backend/`。`pom.xml` 关键依赖:

| 依赖 | 版本 | 说明 |
|---|---|---|
| spring-boot-starter-parent | 3.2.5 | Spring Boot 父 BOM |
| spring-boot-starter-web | (BOM) | Spring MVC + Tomcat |
| spring-boot-starter-validation | (BOM) | Bean Validation |
| spring-boot-starter-security | (BOM) | Spring Security |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.5 | JWT 签发/校验 |
| spring-boot-starter-data-jpa | (BOM) | JPA + Hibernate |
| h2 | (BOM) | H2 内存数据库(dev) |
| mysql-connector-j | (BOM) | MySQL 驱动(prod) |
| springdoc-openapi-starter-webmvc-ui | 2.3.0 | OpenAPI 3 + Swagger UI |
| lombok | (BOM) | 注解简化代码 |

Java 版本 17,`UTF-8` 源码编码。

### 3.3 Web 平台能力

本项目较多使用浏览器原生能力,无 polyfill:

- `getUserMedia` / `<video>` :摄像头采集,经 `camera_utils.Camera` 调度
- `<canvas>` 2D Context:骨架与关键点绘制
- `SpeechSynthesis`:中文 TTS 语音播报(rate / pitch / volume 可调)
- `AudioContext`:节拍器音频调度,使用 lookahead scheduler 提前 100ms 安排 tick
- `IndexedDB`:训练记录离线持久化(DB 版本 2,object store: `sessions` + `sync_queue`)
- `localStorage`:配置项与登录态序列化
- `Service Worker`:fetch 拦截 + 缓存策略路由
- `Web App Manifest`:PWA 安装 + 桌面快捷方式

## 4. 系统架构设计

### 4.1 前后端分离架构

系统采用前后端分离模式:

- 前端负责交互、视觉识别、状态管理、本地缓存和评分等核心计算
- 后端负责账户体系、业务数据持久化、聚合统计、社交关系和接口统一管理

设计的优点:

- 训练时延极低,识别与评分完全在浏览器端完成,无需服务器推理
- 后端压力轻,适合教学项目与小流量场景
- 弱网或离线场景下仍可正常训练并暂存
- AI 反馈模块可热替换(provider 配置切换),不破坏训练主链路

### 4.2 前端分层

```
src/
├── api/          后端接口封装(基于 axios 实例)
│     auth.js  client.js  coach.js  exercise.js
│     plan.js  session.js  social.js  user.js
├── assets/css/   全局样式,在 main.js 中按 base → themes → animations → responsive 顺序加载
├── components/   可复用组件
│   ├── charts/   ProgressChart  HeatmapCalendar 等
│   ├── common/   EmptyState 等
│   ├── layout/   App 外壳/底部导航/侧栏
│   └── training/ HUD  ActionCard  TargetStepper  CountdownOverlay  ReportModal
├── modules/      框架无关的核心业务逻辑(可在非 Vue 环境复用)
│     pose.js  exercise.js  voice.js  storage.js  poster.js
├── stores/       Pinia 状态管理
│     auth.js  config.js  training.js  app.js
├── views/        页面级组件
│     Login Train Records RecordDetail Plans
│     Leaderboard Feed Profile Settings Admin
├── router/       Vue Router + 简易守卫
└── main.js       入口,挂载 Pinia / Router,注册 Service Worker
```

### 4.3 后端分域设计

后端按业务领域(bounded context)拆分包结构,每个包通常包含 Controller / Service / Entity / Repository。`com.fitcoach` 实际子包:

```
admin/         BadgeController    DashboardController(并入 AdminController)
auth/          AuthController     AuthService
badge/         Badge / UserBadge Entity + Service + Repo
coach/         CoachController(纯 mock)
common/        ApiResult  PageResult  (无业务逻辑)
config/        CorsConfig  DataInitializer
exception/     BusinessException  GlobalExceptionHandler
exercise/      Exercise Entity + ExerciseController + Repo
leaderboard/   LeaderboardController(基于 SQL 聚合)
plan/          Plan / UserPlan Entity + Controller + Repo
security/      SecurityConfig  JwtUtil  JwtAuthFilter  SecurityUtil
session/       Session Entity + Controller + Service + Repo
social/        Post / PostComment / PostLike + SocialController + Repo
user/          User / UserFollow + UserController + UserService + Repo
```

约定:

- 每个 Controller 方法返回 `ApiResult<T>`,使用 `ApiResult.ok()` / `ApiResult.fail()` 静态构造,不允许自定义响应包装
- 所有业务接口前缀均为 `/api/**`(SocialController 用 `/api` 作为基址,因其同时挂载 `/posts/**` 与 `/challenges/**` 两组路径)
- Entity 通过 `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` / `@LastModifiedDate` 自动维护时间戳
- 使用 Lombok 的 `@Data` / `@Builder` / `@RequiredArgsConstructor` 减少样板代码
- AI Provider 通过 `application.yml` 中的 `ai.coach.provider` 切换,目前只有 `mock` 实现

## 5. 前端系统设计

### 5.1 页面路由设计

路由定义见 `src/router/index.js`,实际 11 条路由:

| 路径 | name | 页面 | meta |
|---|---|---|---|
| `/login` | Login | Login.vue | `{ layout: 'none', public: true }` |
| `/` | — | 重定向到 `/train` | — |
| `/train` | Train | Train.vue | `{ tab: 'train' }` |
| `/records` | Records | Records.vue | `{ tab: 'records' }` |
| `/records/:id` | RecordDetail | RecordDetail.vue | `{ tab: 'records' }` |
| `/plans` | Plans | Plans.vue | `{ tab: 'plans' }` |
| `/leaderboard` | Leaderboard | Leaderboard.vue | `{ tab: 'social' }` |
| `/feed` | Feed | Feed.vue | `{ tab: 'social' }` |
| `/profile` | Profile | Profile.vue | `{ tab: 'profile' }` |
| `/settings` | Settings | Settings.vue | `{ tab: 'profile' }` |
| `/admin` | Admin | Admin.vue | `{ tab: 'profile', admin: true }` |
| `/:pathMatch(.*)*` | — | 通配重定向到 `/train` | — |

路由守卫(`router.beforeEach`)实际逻辑:

```js
if (to.meta.public) return next();                           // 公开页放行
if (!auth.isLogin && to.path !== '/login' && !auth.guestMode) {
  auth.enterGuest();                                          // 未登录自动进入游客模式
}
next();
```

注释里写得很清楚:**"后续真实落地时可改为 `next('/login')`"**。也就是说当前的访问控制是完全开放的,只是把状态打成 `guestMode = true`,任何页面(包括 `/admin`)都不会被前端拦截。`admin` meta 字段目前没有被守卫使用。

### 5.2 状态管理设计

#### 5.2.1 认证状态(`stores/auth.js`)

| 字段 | 类型 | 说明 |
|---|---|---|
| `token` | string | JWT,持久化到 localStorage(`fitcoach_token`) |
| `user`  | object | 用户信息,持久化到 localStorage(`fitcoach_user`) |
| `guestMode` | boolean | 游客模式标记(仅内存,不持久化) |

派生计算:

- `isLogin = !!token && !!user`
- `isAdmin = user?.role === 'ADMIN'`
- `displayName = user?.nickname || (guestMode ? '游客' : '未登录')`

行为:

- `setAuth(token, user)`:写入 token/user 并落 localStorage
- `logout()`:清空 token/user/guestMode 与 localStorage
- `enterGuest()`:仅切换 `guestMode = true`,不发起任何请求
- `updateProfile(partial)`:本地浅合并 user 字段并回写

#### 5.2.2 配置状态(`stores/config.js`)

DEFAULT_CONFIG 的实际字段如下:

```js
{
  squat:   { down: 90,  up: 160 },   // rep 类:阈值用于上下状态机
  stretch: { down: 60,  up: 160 },
  pushup:  { down: 80,  up: 160 },
  lunge:   { down: 100, up: 170 },
  bridge:  { down: 150, up: 175 },
  plank:       { down: 0, up: 0 },   // timed 类:阈值不参与判定
  jumpingJack: { down: 0, up: 0 },   // timed/rep 混合,不依赖角度阈值
  bpm: 30,                            // 节拍器节拍/分钟
  ttsRate: 1,                         // 语音语速
  theme: 'light',                     // light / dark / ocean / forest / sunset / purple-night
  weeklyGoal: 50,                     // 周训练次数目标
  voiceEnabled: true,
  metronomeEnabled: false,
  autoPauseEnabled: true,
  coachEnabled: true
}
```

提供 `loadFromLocal()` / `save()` / `reset()` / `applyTheme(name)` 四个方法。`applyTheme` 会同步更新 `<meta name="theme-color">`,以匹配 PWA 状态栏。

#### 5.2.3 训练状态(`stores/training.js`)

| 字段 | 类型 | 说明 |
|---|---|---|
| `action` | string | 当前动作 code,默认 `'squat'` |
| `targetReps` | number | 目标次数,默认 10 |
| `reps` | number | 已完成次数 |
| `score` | number | 综合分 |
| `rhythmScore` / `stabilityScore` / `depthScore` / `symmetryScore` | number | 子分项 |
| `currentAngle` | number\|null | 当前关节角度(度) |
| `elapsedMs` | number | 已训练毫秒数 |
| `isTraining` / `isPaused` | boolean | 训练 / 暂停状态 |
| `statusText` | string | UI 提示文本 |

派生 `progress = min(100, reps / targetReps * 100)`。注意 store 中**不包含 `completionScore`**,该字段仅在 `stopTraining` 完成时由 `Exercise.getResult()` 临时取出后写入 session 对象,不暴露到 UI。

### 5.3 训练核心模块设计

训练主链路由以下文件构成:

- `views/Train.vue`:页面级组装,生命周期与回调
- `modules/pose.js`:MediaPipe Pose 封装(初始化、骨架绘制、回调)
- `modules/exercise.js`:角度计算、状态机计数、多维评分
- `modules/voice.js`:TTS 播报 + Web Audio 节拍器
- `modules/storage.js`:IndexedDB 训练记录 + 同步队列

#### 5.3.1 姿态识别(`pose.js`)

`PoseDetector` 类封装 MediaPipe Pose,职责:

- 初始化 `Pose` 实例,设置 `modelComplexity=1`、`smoothLandmarks=true`、`minDetection/Tracking=0.5`
- 初始化 `Camera`,分辨率 320×240(轻量,保证手机端 30fps)
- 在 `onResults` 回调中:
  1. `_drawSkeleton(results)`:在 canvas 上绘制 25 条骨架线 + 33 个关键点
  2. 通过 `actionRef()` 取出当前动作类型,从 `HIGHLIGHT_MAP` 查找需要高亮的关节索引集合
  3. 高亮关节使用 accentColor(默认 `#00d4ff`),其余关节灰色
  4. 把原始结果转给 `onResult` 回调,交给上层(Train.vue)处理

`HIGHLIGHT_MAP` 完整定义如下:

| 动作 | 高亮关键点 |
|---|---|
| squat / lunge | 23,24,25,26,27,28(髋膝踝) |
| stretch / bridge / plank | 11,12,23,24,25,26(肩-髋-膝) |
| pushup | 11,12,13,14,15,16(肩-肘-腕) |
| jumpingJack | 11,12,13,14,15,16,25,26,27,28(全身大关节) |

模型与摄像头分别有 `start / stop / pause / resume` 接口。

#### 5.3.2 动作状态机(`exercise.js`)

`ACTION_DEFS` 定义 7 个动作:

| code | label | 类型 | 左侧关键点 [a,b,c] | 右侧 [a,b,c] | 说明 |
|---|---|---|---|---|---|
| squat   | 深蹲     | rep   | [23,25,27] 髋-膝-踝 | [24,26,28] | 锻炼下肢力量 |
| stretch | 前屈伸展 | rep   | [11,23,25] 肩-髋-膝 | [12,24,26] | 提升柔韧性 |
| pushup  | 俯卧撑   | rep   | [11,13,15] 肩-肘-腕 | [12,14,16] | 强化胸臂 |
| lunge   | 弓步蹲   | rep   | [23,25,27]          | [24,26,28] | 下肢稳定性 |
| bridge  | 臀桥     | rep   | [11,23,25]          | [12,24,26] | 臀部激活 |
| plank   | 平板支撑 | timed | [11,23,27]          | [12,24,28] | 核心力量(角度仅作姿态校验) |
| jumpingJack | 开合跳 | rep | [12,14,16]          | [11,13,15] | 有氧燃脂 |

**`Exercise` 类核心字段**:

```js
state         // idle | up | down,上下状态机当前态
reps          // 累计次数
timestamps[]  // 每次完成时刻(节奏分依据)
angleHistory[]              // 平滑后的均角时序(稳定分依据)
leftAngleHistory[] / rightAngleHistory[]   // 左右各自时序(对称分依据)
depthSamples[]              // 每次 rep 内的最低角度(深度分依据)
minAngleInRep / idealDepth  // 当前 rep 最低角 / 理想深度(thresholdDown - 10)
thresholdDown / thresholdUp // 上下阈值(从 config 注入)
```

**判定流程**(`update(landmarks)`):

1. 取出动作定义中的左右三点,验证 `visibility > 0.5`
2. 用余弦定理 + atan2 计算左右关节角,取均值 `mean`
3. 5 帧滑动均值平滑,得到 `angle`
4. 写入 angleHistory / leftAngleHistory / rightAngleHistory
5. 根据当前 `state` 与角度作如下转移:
   - `idle` 或 `up` → `down`(angle < thresholdDown):触发 `down` 事件,记录最低角
   - `up` 中段(thresholdDown < angle < thresholdUp):触发 `correction` 事件,要求"再到位一点"
   - `down` → `up`(angle > thresholdUp):**计数 +1**,推入 timestamp 与 depthSample,触发 `count` 事件

```
        thresholdUp ─────────────► 计数 +1
              ▲                       │
              │                       ▼
            up状态  ◄───────────────  down状态
              ▲                       ▲
              │  角度 < thresholdDown │
              └───────────────────────┘
```

#### 5.3.3 多维评分算法(`exercise.js`)

训练结束调用 `getResult(bpm)`,输出 5 个子分 + 综合分。每个子分都被截断在 [0, 100],综合分加权后四舍五入:

| 子分 | 公式 | 直觉 |
|---|---|---|
| **rhythmScore** | `100 × (1 − MAE / target)`,其中 `target = 60000/bpm`,MAE 为相邻两次完成时间差与 target 的平均绝对偏差 | 节奏越接近 BPM 设置,分数越高 |
| **stabilityScore** | 取 angleHistory 中落在 `thresholdDown ± 20` 区间的样本,计算标准差 σ;`100 × (1 − σ/20)` | 在最低点附近抖动越小越稳 |
| **depthScore** | 设 `dev = | mean(depthSamples) − idealDepth |`,`100 − dev × 2.5` | 实际下蹲深度越接近理想深度分越高 |
| **symmetryScore** | 左右角度时序逐帧差的 MAE × 2,从 100 减去 | 左右肢体差异越小越对称 |
| **completionScore** | `min(100, reps / targetReps × 100)` | 完成度 |
| **score** | `rhythm × 0.25 + stability × 0.25 + depth × 0.20 + symmetry × 0.15 + completion × 0.15` | 综合 |

样本不足(如 angleHistory < 10、depthSamples = 0)时,对应子分回退为 100,避免短训练被误判。

这一实现属于规则驱动型评分,不依赖机器学习训练数据,可解释性较强,适合课程设计与答辩展示。

#### 5.3.4 语音与节拍器(`voice.js`)

**TTS 部分**(基于 `window.speechSynthesis`):

- `speak(text, priority)`:`priority='high'` 会先 `synth.cancel()` 抢占
- `countVoice(n)`:高优先级播报次数(整数)
- `encourage()`:从 `['很好','继续保持','不错','加油','真棒','状态绝佳','就是这样']` 随机抽取
- `correct(msg)`:高优先级播报纠错语
- `finish(reps)`:训练结束播报"训练结束,共完成 N 次,辛苦了"
- 中文 (`lang='zh-CN'`),`rate` 和 `pitch` 来自配置

**节拍器部分**(基于 `AudioContext`):

- 使用 lookahead scheduler 模式:每 25ms 检查一次,当前时间 + 100ms 内未排定的 tick 用 `oscillator.start(time)` 提前安排
- 每个 tick 是 880Hz 正弦波,持续 50ms,gain 由 0.3 衰减到 0
- 精度优于 `setInterval`,误差 < 1ms

#### 5.3.5 自动暂停

逻辑在 `Train.vue` 的 `onPoseResult` 中,关键常量(`Train.vue` 顶部):

```js
const AUTO_PAUSE_FRAMES = 150;  // 约 5 秒(30fps)
```

每当一帧识别结果为空(`event === 'lost'`),`lostPoseCount.value++`;一旦超过 150 且配置开启 (`config.autoPauseEnabled`),调用 `togglePause()`,并 toast 提示"未检测到人体 · 已自动暂停"。检测到关键点后归零计数。

#### 5.3.6 其他训练辅助常量

```js
const CORRECTION_COOLDOWN = 3000;  // 纠错语 3 秒去抖
const ENCOURAGE_INTERVAL  = 5;     // 每 5 次喊一次鼓励
```

### 5.4 本地数据与离线优先设计

#### 5.4.1 IndexedDB 设计

`storage.js` 维护数据库:

- **DB_NAME**:`FitCoachDB`
- **DB_VERSION**:2
- **Object Store**:
  - `sessions`:训练记录,自增主键 `localId`,索引 `date`、`action`、`synced`
  - `sync_queue`:已创建但**当前未投入读写流程**(预留为后续真正自动补传机制使用)

`sessions` 单条记录的实际字段(由 Train.vue 写入):

```jsonc
{
  localId,            // IndexedDB 自增
  date,               // "YYYY-MM-DD HH:mm:ss"(前端字段名)
  action,             // 'squat' / 'pushup' / ...
  actionLabel,        // 中文动作名
  reps, targetReps, duration,
  score,
  rhythmScore, stabilityScore, depthScore, symmetryScore, completionScore,
  synced: 0,          // 0 未同步,1 已同步
  createdAt,          // Date.now()
  remoteId            // 上传成功后回填的服务端 id
}
```

`storage` 对外提供:`saveSession`、`getAllSessions`、`getSession`、`clearSessions`、`markSynced(localId, remoteId)`、`getUnsynced`、`sessionsToCSV`、`download`。

#### 5.4.2 本地优先保存策略

训练结束的实际链路(`Train.vue` 中 `stopTraining`):

1. `exercise.getResult(bpm)` 计算 5 子分 + 综合分
2. 拼装 session 对象(含 `date`、`action`、`reps`、5 个子分等)
3. `storage.saveSession(session)` 本地入库,得到 `localId`
4. 若 `auth.isLogin`,调用 `sessionApi.create(session)`
5. 上传成功后从响应取 `remote.id`,调 `storage.markSynced(localId, remote.id)`
6. 上传失败时(`catch (_) {}`)静默忽略,本地记录保持 `synced=0`

这种设计已经保证:

- 即使后端不可用,训练结果仍不会丢失
- 演示场景下无需强依赖网络
- 后续可以扩展为真正的自动补传机制(已具备 `getUnsynced()` 与 `/api/sessions/batch` 两个端点)

**字段契约差异(尚未闭环)**:

| 链路点 | 字段名 |
|---|---|
| `Train.vue` → `storage.saveSession` | `date` |
| `Train.vue` → `sessionApi.create` 请求体 | `date` |
| 后端 `SessionService.create` 读取 | `body.get("sessionDate")` ← **这里读不到 `date`** |

后端在解析时 `sessionDate` 取不到值,会回退到 `LocalDate.now().toString()`,所以请求虽然能成功,但服务端记录的日期不是客户端真实的训练时间(只精确到天,且为服务器接收时刻)。批量补传 `/api/sessions/batch` 同样使用该方法,问题相同。**修复方式**:让 `Train.vue` 上传时将 `date` 重命名为 `sessionDate`,或在后端兼容两个字段。

#### 5.4.3 Service Worker 设计

`public/sw.js`,版本 `fitcoach-v3-0-1`,缓存策略路由:

| 资源 | 策略 |
|---|---|
| 安装预缓存 (`APP_SHELL`) | `/` `/index.html` `/manifest.json` `/favicon.svg` |
| `/api/**` 请求 | 网络优先,失败返回 `{"code":-1,"message":"offline"}` JSON |
| CDN 白名单(`cdn.jsdelivr.net`、`fonts.googleapis.com`、`fonts.gstatic.com`) | 缓存优先,后台静默更新 |
| 同源 GET 静态资源 | 缓存优先,miss 时网络获取并写缓存 |
| 非 GET 请求 | 透传 |

`activate` 阶段会清理所有非当前版本的缓存,实现版本前进自动失效。注释里也提示:`bumping asset paths requires cache-version bumps in the SW`。

#### 5.4.4 Manifest 设计

`public/manifest.json` 关键字段:

- `name`:"FitCoach · AI 居家健身陪练"
- `short_name`:"FitCoach"
- `start_url` / `scope`:`/`
- `display`:`standalone`
- `orientation`:`portrait-primary`
- `background_color` / `theme_color`:`#1a1816`(深色)
- `lang`:`zh-CN`
- `categories`:`["fitness","health","lifestyle"]`
- `icons`:单一 `/favicon.svg`,声明尺寸 `192×192 512×512 any`,`purpose: any maskable`
- `shortcuts`:两个桌面快捷方式 — "开始训练" → `/train`、"训练记录" → `/records`

### 5.5 页面功能说明

#### 5.5.1 登录页(`Login.vue`)

UI 已完成:

- 邮箱登录(邮箱 + 密码)
- 手机号登录(手机号 + 6 位验证码,带 60 秒倒计时)
- 注册 Tab(切换后多一个昵称字段)
- 微信登录入口(toast 提示"暂未开放")
- 游客登录(直接 `auth.enterGuest()` 跳转)

**真实联调情况(必须强调)**:`submit()` 方法实际是前端 mock(`Login.vue` 第 37 行起):

```js
async function submit() {
  if (primaryDisabled.value) return;
  loading.value = true;
  try {
    await new Promise(r => setTimeout(r, 500));   // 假装请求
    const isAdmin = form.value.email === 'admin@fitcoach.com';
    const mockUser = {
      id: isAdmin ? 0 : 1,
      nickname: form.value.nickname || form.value.email || form.value.phone || '新用户',
      avatar: '',
      role: isAdmin ? 'ADMIN' : 'USER'
    };
    auth.setAuth('mock-token-' + Date.now(), mockUser);
    ...
  }
}
```

页面底部有明确文字:**"多种登录方式均为占位 · 后端对接后启用"**。当前的 token 不是后端签发的 JWT,带这种 token 调用任何受保护的接口都会被 `JwtAuthFilter` 拒绝(尽管前端因为代理与 mock 的存在感觉不到)。**需要真实联调时,只需将 `submit()` 替换为对 `authApi.loginByEmail` 的调用即可,`api/auth.js` 已经写好。**

#### 5.5.2 训练页(`Train.vue`)

是当前实现最完整的业务页面。功能:

- 选择动作(7 选 1,渲染自定义 SVG 图标)
- 目标次数步进器
- 开启摄像头并 3 秒倒计时
- 实时姿态识别(MediaPipe Pose)+ 骨架/关键点绘制
- 实时训练计数与角度展示(HUD)
- 节拍器(可选)、TTS 播报、纠错冷却、鼓励间隔
- 自动暂停(150 帧无关键点)
- 训练完成后弹出报告(`ReportModal`),展示综合分与 5 子分
- 本地保存到 IndexedDB,登录时尝试同步后端
- 顶部统计(今日次数 / 本周累计 / 连续天数),从 IndexedDB 实时聚合

#### 5.5.3 记录页(`Records.vue` / `RecordDetail.vue`)

- 数据来源:**完全来自本地 IndexedDB**(`storage.getAllSessions()`),不调后端
- 顶部 tab:`chart` / `heatmap`,分别展示 `ProgressChart` 与 `HeatmapCalendar`
- 周目标完成度卡片(`weeklyGoal` 取自 `config`,聚合本周记录)
- 动作筛选 + 列表
- 单条点击进入 `/records/:id`(优先用 `remoteId`,回退 `localId`)
- 一键导出 CSV(`storage.sessionsToCSV` + `storage.download`)
- 一键清空所有本地记录(带确认弹窗)

#### 5.5.4 计划页(`Plans.vue`)

UI 完成,含三个 tab:`official` / `mine` / `market`。当前完全使用 hardcoded 数组渲染:

```js
const officialPlans = [
  { id: 1, title: '新手入门 7 天', ... },
  { id: 2, title: '核心强化 14 天', ... },
  { id: 3, title: '30 天俯卧撑挑战', ... },
  { id: 4, title: '柔韧性提升', ... }
];
const myPlans = [{ id: 5, title: '新手入门 7 天', progress: 3, total: 7, today: true }];
```

`market` tab 显示 "社区计划市场即将上线"。后端 `PlanController` 已具备完整 CRUD + 采用/进度功能,`api/plan.js` 也已封装,但前端尚未切换。

#### 5.5.5 排行榜与动态页

**`Leaderboard.vue`**:`mockData = { weekly: [...8 条...], monthly: [], friends: [] }`,完全 hardcoded,且月榜/好友榜空数组。后端 `LeaderboardController` 已有真实聚合实现。

**`Feed.vue`**:hardcoded 2 条 posts(健身达人小王 / 瑜伽妹妹),仅本地点赞计数,无网络请求。后端 `SocialController` 已有完整 Post / 点赞 / 评论实现。

#### 5.5.6 个人中心与设置页

**`Settings.vue`**:支持各动作阈值、BPM、TTS 语速、主题切换、周目标、AI 教练开关、自动暂停开关 — 全部存于 `stores/config.js` 并落 `localStorage`,无需后端。

**`Profile.vue`**:大量统计仍基于本地 IndexedDB 计算;后端 `/api/users/me/stats`、`/api/users/me/calendar` 已实现,但页面尚未切换数据源。

#### 5.5.7 管理后台页(`Admin.vue`)

完全占位 UI:

```js
const dashboardData = ref({ users: 1234, sessions: 8567, todaySessions: 92, dau: 318 });
```

页面底部有红色提示框:**"当前为 UI 占位页面 · 需后端接口上线后启用实际功能"**。后端 `AdminController` 已经实现 dashboard / users 列表 / ban / unban / sessions / analytics 全部接口。

### 5.6 海报生成设计

`modules/poster.js` 使用 `<canvas>` 离屏绘制后导出 PNG。海报内容:

- 用户昵称 / 头像
- 动作名称 + 完成次数
- 用时 / 综合分 / 5 子分柱状图
- 训练日期
- FitCoach 品牌标识(渐变 logo)

适合作为分享与项目展示亮点。

## 6. 后端系统设计

### 6.1 基础配置

主配置 `application.yml`:

```yaml
spring:
  profiles: { active: dev }
  jpa:
    hibernate: { ddl-auto: update }
    show-sql: false
    open-in-view: false

server: { port: 8080 }

jwt:
  secret: fitcoach-jwt-secret-key-please-change-in-production-2026
  access-token-expire-hours: 2
  refresh-token-expire-days: 30

ai:
  coach:
    enabled: true
    provider: mock                    # mock | claude | openai
    claude:
      api-key: ""
      base-url: https://api.anthropic.com/v1
      model: claude-haiku-4-5-20251001
      max-tokens: 500

upload:
  dir: ./uploads
  max-size: 10MB

cors:
  allowed-origins: http://localhost:5173,http://localhost:4173,http://localhost:8080

springdoc:
  api-docs:    { path: /v3/api-docs }
  swagger-ui:  { path: /swagger-ui.html, tags-sorter: alpha, operations-sorter: alpha }
```

环境差异:

- `application-dev.yml`:H2 内存(`jdbc:h2:mem:fitcoach;DB_CLOSE_DELAY=-1;MODE=MySQL`)、H2 console 启用、`data.sql` 自动执行
- `application-prod.yml`:MySQL 数据源(从环境变量 `DB_HOST/DB_USER/DB_PASSWORD` 注入),`ddl-auto: validate`(由 `application.yml` 默认 `update` 在 prod 中收紧)

### 6.2 安全架构设计

`SecurityConfig.java` 真实配置:

- 关闭 CSRF(`csrf.disable()`)
- 启用 CORS(交由 `CorsConfig` WebMvcConfigurer)
- Session 策略 `STATELESS`
- 关闭 `frameOptions`(为 H2 console 的 iframe 兼容)
- `@EnableMethodSecurity`(让 `@PreAuthorize` 在 `ExerciseController` 等处生效)
- 注入 `JwtAuthFilter` 在 `UsernamePasswordAuthenticationFilter` 之前

`requestMatchers` 实际白名单:

```java
.requestMatchers(
    "/auth/**",                   // 兼容无 /api 前缀的旧路径
    "/api/auth/**",
    "/swagger-ui/**", "/swagger-ui.html",
    "/v3/api-docs/**",
    "/h2-console/**",
    "/error",
    "/actuator/**"
).permitAll()
.requestMatchers(
    "/api/exercises", "/api/exercises/**",
    "/api/plans/official",
    "/api/leaderboard/**"
).permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

**注意**:

- `/api/posts/feed` **未在白名单中**,虽然 SocialController 中的 Feed 接口设计为"公开动态流"(代码使用 `SecurityUtil.currentUserIdOrNull()` 兼容匿名),但 SecurityFilterChain 会先一步把无 token 请求拦掉(`401`)。前端目前是 mock 数据,不会触发该问题,真实联调时需要将该路径加入 permitAll。
- `/api/leaderboard/**` 已经放行匿名,这就是排行榜可以游客访问的原因。
- `JwtUtil` 使用 `Keys.hmacShaKeyFor(secret.getBytes(UTF-8))` 派生 HS256 SecretKey;access token 生命周期 2 小时,refresh token 30 天,载荷包含 `subject=userId` / `username` / `role` / `type=access|refresh` / `iat` / `exp`。

### 6.3 统一响应结构

`ApiResult<T>` 实际字段:

```java
private int code;
private String message;
private T data;
private long timestamp = System.currentTimeMillis();

public static final int CODE_OK   = 0;
public static final int CODE_FAIL = -1;
```

对外提供 `ok()` / `ok(data)` / `ok(data, message)` / `fail(message)` / `fail(code, message)` 5 个静态构造。

`PageResult<T>` 字段:`items` / `total` / `page` / `size`,提供 `of(...)` / `empty(page, size)` 静态方法。

前端 axios 响应拦截器(`api/client.js`)自动解包:

```js
if (data.code === 0 || data.code === 200) return data.data;
```

`code === 200` 是兼容历史版本的安全网。返回值就是 `data` 字段本身,业务代码看不到 `code/message/timestamp` 包装。

异常分支:

- 后端返回非 0 code → toast `data.message` + reject
- HTTP 401 → 调 `auth.logout()` + toast "登录已过期"
- 网络错误 → toast "网络异常,请稍后重试"
- 其他错误 → toast `err.response?.data?.message || '服务器开小差了'`

### 6.4 全局异常处理

`exception/GlobalExceptionHandler.java`(`@RestControllerAdvice`)统一拦截 `BusinessException`、`MethodArgumentNotValidException`、`Exception`,均转为 `ApiResult.fail(...)` 返回,保证所有错误都遵守同一响应结构。`BusinessException(int code, String message)` 是项目内部抛出受控错误的载体。

## 7. 核心业务模块设计

> 下表中"状态"的含义:**真实** = 走数据库,有业务逻辑;**占位 501** = 直接返回 `ApiResult.fail(501, ...)`;**mock** = 返回硬编码内容。

### 7.1 认证模块(`auth/`)Tag: `01. 认证`

| 接口 | Method | 状态 | 说明 |
|---|---|---|---|
| `/api/auth/login/email` | POST | **真实** | BCrypt 校验 + 双 Token 签发 |
| `/api/auth/register` | POST | **真实** | 邮箱重复校验,密码至少 6 位,默认 weeklyGoal=50 |
| `/api/auth/login/guest` | POST | **真实** | 基于 `deviceId` upsert,role=`GUEST`、weeklyGoal=30 |
| `/api/auth/refresh` | POST | **真实** | 校验 refresh token 的 `type=refresh` claim,只重新签 access |
| `/api/auth/me` | GET | **真实** | 由 `SecurityUtil.currentUserId()` 取上下文 |
| `/api/auth/logout` | POST | 真实(无服务端动作) | 无状态 JWT,客户端丢弃 token 即可,仅返回成功 |
| `/api/auth/login/phone` | POST | **占位 501** | "短信登录未接入" |
| `/api/auth/login/wechat` | POST | **占位 501** | "微信登录未接入" |
| `/api/auth/sms/send` | POST | **占位 501** | "短信服务未接入" |
| `/api/auth/email/send` | POST | **占位 501** | "邮件服务未接入" |

成功登录返回的 payload 结构:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": { "id":1, "nickname":"...", "email":"...", "avatar":"", "role":"USER", "weeklyGoal":50 }
}
```

### 7.2 用户模块(`user/`)Tag: `02. 用户`

接口位于 `UserController.java`,业务在 `UserService.java`,实体含 `User` + `UserFollow`(多对多关注关系)。

| 接口 | 说明 |
|---|---|
| `GET /api/users/me` | 当前用户资料 |
| `PUT /api/users/me` | 更新昵称、avatar、weeklyGoal 等 |
| `POST /api/users/me/avatar` | `multipart/form-data` 上传头像,落 `upload.dir`,返回 `{ url }` |
| `GET /api/users/me/stats` | 总训练次数 / 总 reps / 最高分 / 连续天数 |
| `GET /api/users/me/calendar?yearMonth=YYYY-MM` | 月历训练打卡数据 |
| `POST /api/users/{userId}/follow` | 关注 |
| `DELETE /api/users/{userId}/follow` | 取消关注 |
| `GET /api/users/{userId}/followers` | 粉丝列表 |
| `GET /api/users/{userId}/followings` | 关注列表 |

### 7.3 训练记录模块(`session/`)Tag: `03. 训练记录`

接口位于 `SessionController.java`,业务在 `SessionService.java`。所有接口需登录(`@SecurityRequirement(name="bearerAuth")`)。

| 接口 | 说明 | 关键实现 |
|---|---|---|
| `POST /api/sessions` | 提交训练记录 | `Map<String,Object>` 解析,保存 Session 实体;**读取 `body.get("sessionDate")`,缺失时回退 `LocalDate.now()`** |
| `GET /api/sessions?page=&size=&action=&startDate=&endDate=` | 分页查询 | 自定义 `@Query`,按 `sessionDate DESC, id DESC` 排序 |
| `GET /api/sessions/{id}` | 详情 | 强制校验 `userId` 归属,跨用户 403 |
| `PUT /api/sessions/{id}` | 更新 notes | 仅修改 notes |
| `DELETE /api/sessions/{id}` | 删除 | 校验归属 |
| `POST /api/sessions/batch` | 批量同步 | body: `{ sessions: [...] }`;**实现是循环调用 `create`,不是真正的 JDBC batch**;返回 `{ inserted: N }` |
| `GET /api/sessions/export/csv` | 导出 CSV | UTF-8 + BOM 由前端自加,后端只输出纯 CSV(id, date, action, reps, targetReps, duration, score, rhythm, stability, notes) |

### 7.4 AI 教练模块(`coach/`)Tag: `04. AI 教练`

`CoachController.java`,**整个控制器目前是 mock**,有完整 4 个端点但无真实逻辑:

| 接口 | 实现状态 |
|---|---|
| `POST /api/coach/feedback` | 返回 hardcoded `{ review, suggestion, encouragement, nextGoal, provider:"mock" }`,**完全没有调用 Claude / OpenAI**,也没有读取请求体中的 sessionId |
| `GET /api/coach/suggestion` | 返回 hardcoded `{ summary, advice: ['加强上肢训练','每天增加 5 分钟有氧',...] }` |
| `GET /api/coach/weekly-plan` | 返回 hardcoded 周一/三/五/日的样例计划 |
| `GET /api/coach/history` | 返回 `List.of()`(空数组) |

源码注释里写着:`// TODO: 查询 session 详情 → 拼装 prompt → 调 Claude → 持久化`。

要完成真实落地需要:

1. 引入 Anthropic SDK 或基于 `RestClient` 调用 `https://api.anthropic.com/v1/messages`
2. 拼装 system prompt(可缓存)+ 当前训练数据 user prompt
3. 利用 prompt caching 降本(系统 prompt + 动作定义命中率应 > 90%)
4. 持久化 feedback 到新表(如 `t_coach_feedback`)
5. 补充成本控制(限流 / 用户配额 / 缓存常见问题)

`application.yml` 中 `ai.coach.provider` / `ai.coach.claude.model` / `ai.coach.claude.api-key` 已经预留好。

### 7.5 训练计划模块(`plan/`)Tag: `05. 训练计划`

`PlanController.java`,实体 `Plan` + `UserPlan`(用户与计划的多对多关系)。

| 接口 | 状态 | 备注 |
|---|---|---|
| `GET /api/plans` | 真实 | 分页全部已发布,按 `adoptCount DESC` |
| `GET /api/plans/official` | 真实(白名单匿名可访问) | `findByOfficialTrueAndPublishedTrueOrderByAdoptCountDesc` |
| `GET /api/plans/market` | 真实 | 过滤掉 official=true 的计划 |
| `GET /api/plans/{id}` | 真实 | 详情 |
| `POST /api/plans` | 真实 | 用户创建,authorId=当前用户,official=false |
| `PUT /api/plans/{id}` | 真实 | 仅作者或 ADMIN 可改 |
| `DELETE /api/plans/{id}` | 真实 | 同上 |
| `GET /api/plans/mine` | 真实 | 当前用户已采用且 status=ACTIVE 的计划 |
| `POST /api/plans/{id}/adopt` | 真实 | 创建 UserPlan(progressDay=0, status=ACTIVE),计划 adoptCount+1 |
| `DELETE /api/plans/{id}/adopt` | 真实 | 把 UserPlan 的 status 置为 ABANDONED(软删) |
| `PUT /api/plans/{id}/progress` | 真实 | 更新 progressDay / status |

计划内容通过 `itemsJson` 字段保存(每日动作配置),属于轻量化实现,便于快速扩展而不需要设计复杂的计划明细表。

### 7.6 徽章模块(`badge/`)

实体 `Badge`(模板)+ `UserBadge`(解锁记录)。`BadgeService.check(userId)` 是核心方法:

1. 一次性聚合用户数据:
   - `sessions = sessionRepo.countByUserId`
   - `totalReps = sessionRepo.sumRepsByUserId`(可能为 null)
   - `bestScore` / `bestRhythm`:从全部 session 求 max
   - `distinctDays`:对 `sessionDate` 去重,`totalDays = distinctDays.size()`
   - `streakDays`:从今天回溯,日期连续命中 distinctDays 计 1
2. 遍历所有 Badge,跳过已解锁项
3. 对每个 Badge,从 `criteriaJson` 解析阈值字段(支持 6 个维度:`sessions / totalReps / bestScore / bestRhythm / streakDays / totalDays`)
4. 全部满足则 `userBadgeRepo.save(...)` 解锁,加入返回列表

接口 `GET /api/badges` / `GET /api/badges/mine` / `POST /api/badges/check`。

种子徽章(`data.sql`):

| code | name | criteria |
|---|---|---|
| first_training | 初次训练 | `{"sessions":1}` |
| hundred_reps   | 百次达成 | `{"totalReps":100}` |
| seven_streak   | 七日连续 | `{"streakDays":7}` |
| perfect_score  | 完美评分 | `{"bestScore":95}` |
| rhythm_master  | 节奏大师 | `{"bestRhythm":100}` |
| thirty_days    | 坚持不懈 | `{"totalDays":30}` |

### 7.7 排行榜模块(`leaderboard/`)Tag: `07. 排行榜`

`LeaderboardController.java`,完整真实实现:

| 接口 | 数据源 |
|---|---|
| `GET /api/leaderboard/weekly` | 最近 7 天(从今天往前 6 天)聚合 |
| `GET /api/leaderboard/monthly` | 当月 1 号至今聚合 |
| `GET /api/leaderboard/friends` | 当前用户 + 关注列表(`UserFollow.followingId`)的最近 7 天聚合 |

聚合在 SQL 层完成:`sessionRepo.aggregateSince(startDate, PageRequest)` 返回 `{ userId, totalReps, avgScore }` 接口投影,Service 再拼装 user 信息(昵称、头像)、加排名,最多 20 行。

### 7.8 社交动态模块(`social/`)Tag: `08. 社交动态`

`SocialController.java`,base path `/api`(同时挂 `/posts/**` 与 `/challenges/**`)。

**Post / 评论 / 点赞**:全部真实持久化。

| 接口 | 状态 |
|---|---|
| `GET /api/posts/feed?page=&size=` | 真实,按 `createdAt DESC` 分页;额外包含 `liked`(当前用户是否已点赞)与作者昵称头像 |
| `POST /api/posts` | 真实,可带 `sessionId` / `content` / `visibility` |
| `GET /api/posts/{id}` | 真实 |
| `DELETE /api/posts/{id}` | 真实,作者本人或 ADMIN |
| `POST /api/posts/{id}/like` | 真实,幂等(已点赞不重复 +1) |
| `DELETE /api/posts/{id}/like` | 真实 |
| `POST /api/posts/{id}/comments` | 真实,空内容 400 |
| `GET /api/posts/{id}/comments` | 真实,按 `createdAt ASC` |

**挑战赛**:简版占位:

| 接口 | 状态 |
|---|---|
| `GET /api/challenges` | 静态返回 2 条(30 天深蹲挑战、7 天俯卧撑马拉松) |
| `POST /api/challenges/{id}/join` | **不持久化**,只校验登录并直接返回成功 |
| `GET /api/challenges/{id}/rank` | 返回空 list |

### 7.9 动作库模块(`exercise/`)Tag: `09. 动作库`

`ExerciseController.java`,Path `/api/exercises`(白名单匿名可读)。

| 接口 | 权限 | 说明 |
|---|---|---|
| `GET /api/exercises` | public | 列表(`enabled=true`,按 sortOrder 升序) |
| `GET /api/exercises/{code}` | public | 详情 |
| `POST /api/exercises` | `@PreAuthorize("hasRole('ADMIN')")` | 新增 |
| `PUT /api/exercises/{code}` | ADMIN | 更新 |
| `DELETE /api/exercises/{code}` | ADMIN | 删除 |

后端动作库与前端 `ACTION_DEFS` 形成数据层互补:

- 前端写死动作定义(`landmarks` 索引、状态机 kind),保证训练演示可用
- 后端保留动作库,便于后续做后台维护和动态下发

### 7.10 管理后台模块(`admin/`)Tag: `10. 管理后台`

`AdminController.java`,所有接口 `/api/admin/**`,需 `ROLE_ADMIN`。

| 接口 | 输出字段 |
|---|---|
| `GET /api/admin/dashboard` | `users` / `sessions` / `todaySessions` / `dau` / `pv7d`(过去 7 天每日训练数列表) |
| `GET /api/admin/users?page=&size=&keyword=` | 分页 + 邮箱/昵称模糊过滤,字段 `id, email, nickname, role, status, createdAt` |
| `POST /api/admin/users/{id}/ban` | 把 status 置为 DISABLED |
| `POST /api/admin/users/{id}/unban` | 把 status 置为 ACTIVE |
| `GET /api/admin/sessions?page=&size=` | 全部训练记录分页 |
| `GET /api/admin/analytics` | `retention7d` / `retention30d`(按总用户基数的近似值)/ `avgScore`(最近 500 条均值)/ `actionDistribution`(分动作计数 Map) |

注意:文档前一版中把 dashboard 的"总用户数 / 总训练数 / 今日训练数 / 7 日活跃趋势"和 analytics 的"7/30 日留存近似值 / 平均分 / 动作分布"列在一起,这次修订按真实代码拆分。

## 8. 数据模型设计

后端 JPA 实体使用 `ddl-auto: update`,无独立迁移文件。所有实体通过 `@EntityListeners(AuditingEntityListener.class)` 自动维护时间戳。

### 8.1 用户表 `t_user`

来源:`com.fitcoach.user.User`。索引:`idx_email(email)`、`idx_phone(phone)`。

| 字段 | 类型/约束 | 备注 |
|---|---|---|
| id | PK auto | |
| email | unique, length 64 | |
| phone | length 20 | |
| passwordHash | length 128 | BCrypt |
| nickname | not null, length 32 | |
| avatar | length 256 | URL |
| role | not null, length 16 | `USER` / `ADMIN` / `GUEST` |
| loginType | length 16 | `email` / `phone` / `wechat` / `guest` |
| openId | length 128 | 微信预留 |
| deviceId | length 128 | 游客凭据 |
| status | not null, length 16 | `ACTIVE` / `DISABLED` |
| weeklyGoal | int | 默认 50 |
| createdAt / updatedAt | datetime | 由 Auditing 自动维护 |

### 8.2 训练记录表 `t_session`

来源:`com.fitcoach.session.Session`。索引:`idx_user_date(userId, sessionDate DESC)`、`idx_action(action)`。

| 字段 | 类型 | 备注 |
|---|---|---|
| id | PK auto | |
| userId | not null | |
| action | not null, length 32 | code(`squat` 等) |
| actionLabel | length 32 | 中文名 |
| reps | not null | |
| targetReps | int | 可空 |
| duration | not null | 秒 |
| score / rhythmScore / stabilityScore / depthScore / symmetryScore / completionScore | int | 5 子分 + 综合 |
| sessionDate | length 32 | `YYYY-MM-DD` 字符串(不是 LocalDate,JSON 友好) |
| notes | length 512 | 备注 |
| createdAt | datetime | |

### 8.3 训练计划表 `t_plan` / 用户计划关系表 `t_user_plan`

`Plan`:`id, title, description, level(NEWBIE|INTERMEDIATE|ADVANCED), cover, days, itemsJson(TEXT), official, published, authorId, adoptCount, createdAt`。

`UserPlan`:`id, userId, planId, progressDay, status(ACTIVE|ABANDONED|COMPLETED), adoptedAt, updatedAt`。

### 8.4 动态/评论/点赞表

`Post`:`id, userId, sessionId, content(1000), likes, commentsCount, visibility(PUBLIC|FRIENDS), createdAt`。索引 `idx_user_created(userId, createdAt DESC)`。

`PostComment`:`id, postId, userId, content, createdAt`。

`PostLike`:维护 `(postId, userId)` 关系,用 `existsByPostIdAndUserId` 实现幂等点赞。

### 8.5 徽章表

`Badge`:`code(PK string), name, description, icon(emoji 或 URL), criteriaJson(TEXT), sortOrder`。

`UserBadge`:`id, userId, badgeCode, unlockedAt`。

### 8.6 动作库表 `t_exercise`

`Exercise`:`code(PK string), name, description, kind(rep|timed), icon, videoUrl, landmarksJson(TEXT), defaultThresholdDown, defaultThresholdUp, enabled, sortOrder`。

### 8.7 关注关系表 `t_user_follow`

`UserFollow`:`id, followerId, followingId, createdAt`,用于 `/api/users/{id}/follow`。

## 9. 关键业务流程

### 9.1 训练流程

```
用户进入训练页
  → 选择动作(7 选 1)与目标次数
  → 点击「开始训练」
  → 加载 MediaPipe Pose 模型(首次)
  → 启动 Camera(320×240,30fps)
  → 3 秒倒计时(CountdownOverlay)
  → exercise.init(action, config, target);training store 重置
  → 每帧:onResults → exercise.update(landmarks)
       ├─ 提取角度(visibility 校验)
       ├─ 5 帧滑动均值平滑
       ├─ 状态机判断 idle/up/down 转移
       ├─ 计数事件 → voice.countVoice + 5 次一鼓励
       ├─ 纠错事件 → voice.correct(冷却 3s)
       └─ lost 事件 → 累计无人帧;超 150 帧自动暂停
  → 达到 targetReps 自动 stopTraining
  → exercise.getResult(bpm) 计算 5 子分 + 综合分
  → voice.finish 播报
  → storage.saveSession(...) 入库
  → if (auth.isLogin) sessionApi.create → 成功后 markSynced
  → 弹出 ReportModal,展示成绩
  → refreshStats 刷新顶部今日/本周/连续
```

### 9.2 登录流程

**理论(`api/auth.js` 已封装)**:

```
邮箱+密码 → POST /api/auth/login/email
  → 后端 BCrypt 验证 → 签 access(2h) + refresh(30d)
  → 前端 auth.setAuth(accessToken, user)
  → 落 localStorage:fitcoach_token / fitcoach_user
```

**当前(Login.vue 实际)**:

```
邮箱+密码 → 等待 500ms(模拟网络)
  → 构造 mock 用户对象(role 由邮箱是否为 admin@fitcoach.com 决定)
  → auth.setAuth('mock-token-' + Date.now(), mockUser)
  → 跳转 /train
```

**真实联调切换点**:把 `Login.vue` 的 `submit()` 替换为 `await authApi.loginByEmail(email, password)`,把返回的 `accessToken` / `user` 喂给 `auth.setAuth` 即可。后端登录链路本身已通。

### 9.3 离线记录同步流程

**目标流程**:

```
训练结束 → IndexedDB(synced=0)
  → 检测 isLogin && navigator.onLine
  → storage.getUnsynced() → POST /api/sessions/batch { sessions: [...] }
  → 返回 inserted:N → 全部 markSynced
```

**当前已具备**:

- 本地 `synced` 标记与 `getUnsynced()` 查询
- 后端 `POST /api/sessions/batch` 接口(内部循环调 `create`)

**仍缺**:

- 自动触发补传调度(应监听 `online` 事件 + 登录事件)
- 网络恢复监听 / 周期重试
- 字段契约统一 — 前端目前发 `date`,后端读 `sessionDate`(详见 §5.4.2)

### 9.4 Feed 发布流程

```
完成训练 → 用户点击「分享到动态」
  → 携带 sessionId、内容、visibility 调 POST /api/posts
  → 后端持久化 → 返回 Post
  → 其他用户在 /feed 通过 GET /api/posts/feed 拉取
  → 列表项展示 likes / commentsCount / liked(对当前用户)
  → 点赞/评论调对应接口
```

后端实现完整,前端 Feed 页尚未切换为真实接口。

## 10. 主要接口清单

### 10.1 认证(Tag 01)

| Method | Path |
|---|---|
| POST | `/api/auth/login/email` |
| POST | `/api/auth/login/phone` *(占位 501)* |
| POST | `/api/auth/login/wechat` *(占位 501)* |
| POST | `/api/auth/login/guest` |
| POST | `/api/auth/sms/send` *(占位 501)* |
| POST | `/api/auth/email/send` *(占位 501)* |
| POST | `/api/auth/register` |
| POST | `/api/auth/refresh` |
| POST | `/api/auth/logout` |
| GET  | `/api/auth/me` |

### 10.2 用户(Tag 02)

`GET /api/users/me`、`PUT /api/users/me`、`POST /api/users/me/avatar`、`GET /api/users/me/stats`、`GET /api/users/me/calendar`、`POST/DELETE /api/users/{userId}/follow`、`GET /api/users/{userId}/followers`、`GET /api/users/{userId}/followings`。

### 10.3 训练记录(Tag 03)

`POST /api/sessions`、`GET /api/sessions`、`GET /api/sessions/{id}`、`PUT /api/sessions/{id}`、`DELETE /api/sessions/{id}`、`POST /api/sessions/batch`、`GET /api/sessions/export/csv`。

### 10.4 AI 教练(Tag 04,**全部 mock**)

`POST /api/coach/feedback`、`GET /api/coach/suggestion`、`GET /api/coach/weekly-plan`、`GET /api/coach/history`。

### 10.5 计划(Tag 05)

`GET /api/plans`、`GET /api/plans/official`(public)、`GET /api/plans/market`、`GET /api/plans/{id}`、`POST /api/plans`、`PUT /api/plans/{id}`、`DELETE /api/plans/{id}`、`GET /api/plans/mine`、`POST/DELETE /api/plans/{id}/adopt`、`PUT /api/plans/{id}/progress`。

### 10.6 徽章(无独立 Tag,挂在 Badge 路由下)

`GET /api/badges`、`GET /api/badges/mine`、`POST /api/badges/check`。

### 10.7 排行榜(Tag 07,public)

`GET /api/leaderboard/weekly`、`/monthly`、`/friends`。

### 10.8 社交(Tag 08)

`GET /api/posts/feed`、`POST /api/posts`、`GET /api/posts/{id}`、`DELETE /api/posts/{id}`、`POST/DELETE /api/posts/{id}/like`、`POST/GET /api/posts/{id}/comments`、`GET /api/challenges` *(静态)*、`POST /api/challenges/{id}/join` *(不持久化)*、`GET /api/challenges/{id}/rank` *(返空)*。

### 10.9 动作库(Tag 09,GET 公开,写需 ADMIN)

`GET /api/exercises`、`GET /api/exercises/{code}`、`POST/PUT/DELETE /api/exercises/{code}`。

### 10.10 管理后台(Tag 10,需 ROLE_ADMIN)

`GET /api/admin/dashboard`、`/users`、`POST /api/admin/users/{id}/ban`、`/unban`、`GET /api/admin/sessions`、`/analytics`。

## 11. 部署与运行说明

### 11.1 本地开发环境要求

- JDK 17(后端 `<java.version>17</java.version>`)
- Maven 3.9+
- Node.js 18+(Vite 5 要求)
- npm 9+
- 现代浏览器,推荐 Chrome 90+(对 MediaPipe / IndexedDB v2 / PWA 支持完整)

### 11.2 前端启动

```bash
cd home-fitness-fullstack/frontend
npm install
npm run dev      # http://localhost:5173,代理 /api → :8080
npm run build    # 输出到 dist/
npm run preview  # 预览 dist 在 :4173
```

无 lint / test 配置,不要假设 `npm test` / `npm run lint` 存在。

### 11.3 后端启动

```bash
cd home-fitness-fullstack/backend
mvn spring-boot:run                                   # 默认 dev profile(H2 内存)
mvn spring-boot:run -Dspring-boot.run.profiles=prod   # 生产 MySQL,需 DB_HOST / DB_USER / DB_PASSWORD
mvn test                                              # 跑测试(目前无业务测试)
mvn -Dtest=ClassName#method test                      # 单测
mvn clean package                                     # 打 jar 到 target/
```

启动后:

- API 根:http://localhost:8080
- Swagger UI:http://localhost:8080/swagger-ui.html
- H2 console:http://localhost:8080/h2-console(JDBC `jdbc:h2:mem:fitcoach`,user `sa`,密码空)

种子账号(由 `DataInitializer` 在启动时创建,而非 data.sql):

- `admin@fitcoach.com` / `admin123`(role=ADMIN,weeklyGoal=100)
- `demo@fitcoach.com`  / `admin123`(role=USER,weeklyGoal=50)

### 11.4 Windows 一键启动

`home-fitness-fullstack/启动.bat`:

- 在新窗口中启动后端 `mvn spring-boot:run`
- 检测前端 `node_modules/`,缺失则 `npm install`
- 在新窗口中启动 `npm run dev`
- 自动打开 `http://localhost:5173`

### 11.5 Docker 部署

`home-fitness-fullstack/docker-compose.yml` 编排两个服务:

- **backend**:`./backend/Dockerfile` 多阶段构建
  - Stage 1(`maven:3.9-eclipse-temurin-17`):`mvn dependency:go-offline` → `mvn -DskipTests package`
  - Stage 2(`eclipse-temurin:17-jre`):仅复制 jar,容器入口 `java $JAVA_OPTS -jar app.jar`
  - 暴露 8080,挂载 `backend-uploads` 卷到 `/app/uploads`
  - healthcheck 用 `wget` 探测 `/v3/api-docs`
- **frontend**:`./frontend/Dockerfile` 多阶段构建
  - Stage 1(`node:20-alpine`):`npm ci` → `vite build` → 产物 `dist/`
  - Stage 2(`nginx:alpine`):`dist/` → `/usr/share/nginx/html`,自带 `nginx.conf` 反代 `/api → backend:8080`
  - 端口映射 `5173 → 80`,依赖 backend healthy

启动:

```bash
cd home-fitness-fullstack
docker compose up -d --build       # 构建并后台启动
docker compose ps                  # 查看状态
docker compose logs -f             # 查看实时日志
docker compose down                # 停止并删除容器
```

### 11.6 环境差异

| 项 | dev(默认) | prod |
|---|---|---|
| 数据库 | H2 内存 + `data.sql` 自动加载 | MySQL,从环境变量读取连接 |
| `ddl-auto` | `update` | `validate`(在 application-prod.yml 中收紧) |
| H2 console | 启用 | 关闭 |
| 数据持久化 | 重启即丢 | 持久化 |
| JWT 密钥 | 配置文件中的固定值 | **必须**通过环境变量覆盖 |

⚠️ Docker compose 默认仍跑 dev profile(H2),不适合生产持久化部署。生产部署需:

1. 切换 profile(`SPRING_PROFILES_ACTIVE=prod`)
2. 注入 DB / JWT 密钥环境变量
3. 单独运行 MySQL 容器或对接外部 RDS

## 12. 测试与验证建议

### 12.1 当前测试现状

- 后端 `pom.xml` 已引入 `spring-boot-starter-test` + `spring-security-test`
- 但 `src/test/java` 下未发现成体系的业务测试类
- 前端没有任何测试运行器或配置(无 vitest / jest / playwright 痕迹)

项目当前主要靠人工联调和页面验收。

### 12.2 建议的测试维度

#### 功能测试

- 邮箱登录与注册 + token 持久化
- 游客进入训练页
- 7 个动作各自的训练计数与评分
- 自动暂停(150 帧无关键点)
- 训练记录本地保存与 CSV 导出
- 计划采用 / 进度更新 / 放弃
- Post 发布 / 点赞 / 评论
- 管理后台用户列表 / 封禁 / 解封
- 徽章解锁(初次训练、百次达成等)

#### 接口测试

建议通过 Swagger UI / Postman / Apifox 重点覆盖:

- `/api/auth/login/email`(成功 / 邮箱不存在 / 密码错 / 账号禁用)
- `/api/sessions`(POST 字段缺失容错 / 跨用户 GET 403)
- `/api/sessions/batch`(空数组 / 大批量)
- `/api/plans/official`(匿名访问)
- `/api/posts/feed`(匿名访问预期 401,登录后 200)
- `/api/admin/dashboard`(非 ADMIN 403)
- `/api/badges/check`(各阈值场景)

#### 前端兼容性测试

- Chrome 桌面端(主要目标)
- Chrome 移动端 / Safari iOS
- Edge
- 手机 PWA 安装模式(`Add to Home Screen`)

#### 弱网与离线测试

- DevTools 切到 Offline,首次加载后可访问应用壳
- 离线训练后本地记录是否保存
- 网络恢复后是否能补传 — 当前仍需手动触发

### 12.3 可作为论文或答辩展示的验证点

- 训练识别主链路完整演示(深蹲/俯卧撑/平板)
- 7 动作切换 + 关节高亮变化
- 多维评分报告生成与海报导出
- 本地训练记录列表 / 趋势图 / 热力图
- CSV 导出
- PWA 安装到桌面 / 离线打开应用
- 后端 Swagger UI 演示(展示 60+ 接口)
- H2 console 演示数据写入

## 13. 当前实现边界与问题分析

### 13.1 已经比较完整的部分

- 前后端工程结构 + Docker 编排
- 浏览器端姿态识别(MediaPipe + 骨架绘制)
- 7 动作的状态机计数 + 5 维评分 + 综合分
- TTS 语音反馈 + Web Audio 高精度节拍器
- IndexedDB 本地记录 + CSV 导出
- PWA(Manifest + Service Worker 多策略缓存)
- 后端 Auth / User / Session / Plan / Badge / Leaderboard / Social / Exercise / Admin 9 个域的真实业务实现
- 全局异常处理 + 统一响应结构
- Docker 多阶段构建,前端走 nginx 反代后端
- 种子用户与种子动作/计划/徽章

### 13.2 当前主要缺口

#### 13.2.1 登录页未真实接后端

`Login.vue` 的 `submit()` 仍是 mock(等待 500ms → 写入 `mock-token-*`)。`api/auth.js` 已封装好,只需替换调用即可。

#### 13.2.2 多个页面仍使用 hardcoded mock

- `Feed.vue`:hardcoded 2 条 posts
- `Leaderboard.vue`:hardcoded mockData(monthly/friends 为空)
- `Plans.vue`:hardcoded officialPlans / myPlans
- `Admin.vue`:hardcoded dashboardData,带"UI 占位"提示框
- `Profile.vue`:统计仍来自本地 IndexedDB

#### 13.2.3 AI 教练全部 mock

`CoachController` 4 个接口全部返回 hardcoded 文案,`provider` 字段写死 `"mock"`。`application.yml` 已预留 Claude / OpenAI 配置项,需要补充真实实现 + 持久化 + 成本控制。

#### 13.2.4 自动补传未闭环

本地 `synced` 标记 + `getUnsynced()` 已具备,后端 `POST /api/sessions/batch` 也已具备,但缺乏:

- 触发时机(online 事件 / 登录事件 / 周期任务)
- 字段契约统一(`date` vs `sessionDate`)

#### 13.2.5 挑战赛未持久化

`/api/challenges` 静态返回,`/join` 不入库,`/rank` 返空。需要新建 `Challenge` + `ChallengeParticipant` 表才能完整。

#### 13.2.6 测试体系不足

后端无业务测试类,前端无测试运行器。

#### 13.2.7 Feed 接口安全配置不一致

`SocialController.feed` 用 `currentUserIdOrNull()` 兼容匿名,但 `SecurityConfig` 未把 `/api/posts/feed` 列入 permitAll,实际匿名访问会被 401 拦截。

### 13.3 工程性风险

- 默认开发环境为 H2 内存库,容器重启或服务重启即丢数据
- Docker compose 默认运行 dev profile,容器化部署需手动切 prod
- JWT 密钥默认值写死在 `application.yml`,生产必须覆盖
- 文件上传已接 `multipart`,但缺少静态映射(`/uploads/**`)、容量限制与 MIME 校验
- 用户头像写到本地磁盘卷,水平扩展时需切对象存储
- `ddl-auto: update` 不适合生产,缺少数据库迁移工具(Flyway / Liquibase)
- `cors.allowed-origins` 仅含 `localhost`,生产需注入域名

## 14. 后续优化建议

### 14.1 短期优化(联调闭环)

1. 把 `Login.vue` 的 mock 改为真实 `authApi.loginByEmail`
2. `Records.vue` 改为"本地优先 + 云端兜底"双源(已在本地有记录但无 remoteId 的,登录后自动补传)
3. `Feed.vue` / `Leaderboard.vue` / `Plans.vue` / `Admin.vue` 切换到真实接口
4. 修复 `date` ↔ `sessionDate` 字段契约
5. 把 `/api/posts/feed` 加入 SecurityConfig 白名单
6. 增加 online 监听,网络恢复时自动调 `/api/sessions/batch`

### 14.2 中期优化(AI 与社交)

1. 接入真实 Claude Haiku 4.5(prompt caching + 系统 prompt 复用 + 用户配额)
2. 新建 `t_coach_feedback` 表持久化 AI 反馈,前端可看历史
3. 给挑战赛建实体表(Challenge / ChallengeParticipant)和聚合排行
4. 后台增加动作库可视化维护
5. 引入 Redis 做排行榜缓存与 SMS 验证码存储
6. 头像上传改为 OSS / S3,生成签名 URL

### 14.3 工程化优化

1. 后端补 Service 层单元测试 + Web 层 MockMvc 集成测试
2. 前端引入 vitest 做 modules 单测(评分算法、状态机、IndexedDB)
3. 引入 Flyway 管理数据库迁移
4. 增加 GitHub Actions CI(`mvn test` + `npm run build`)
5. Docker Compose 拆出 prod profile,加 MySQL 服务与持久化卷
6. JWT 密钥、AI Key、DB 密码统一通过 `.env` 注入

## 15. 总结

FitCoach 是一个以"浏览器端姿态识别 + 离线优先 + 全栈扩展能力"为核心的智能居家健身陪练系统。就当前仓库实际状态而言,它不是一个静态原型,而是具备真实训练主链路与较完整后端业务骨架的全栈项目。

**最强的部分**:

- 训练功能链路真实可跑,7 动作 + 5 维评分,可解释、可演示
- 浏览器端识别与评分逻辑完整,无服务器推理依赖
- 本地记录与 PWA 能力清晰,具备真正的离线优先体验
- 后端 9 个业务域均已写到真实持久化层级,`AdminController` 与 `LeaderboardController` 都已经做 SQL 聚合
- 工程结构清晰、按域拆分,易于后续扩展

**最明显的不足**:

- 前后端联调尚未完全收口(登录页、Feed、排行榜、计划、个人中心、管理后台)
- AI 教练仍是 mock,未触达真正的智能化能力
- 自动补传调度缺失,字段契约 `date / sessionDate` 未统一
- 挑战赛、短信、邮件、微信登录均为占位
- 缺乏自动化测试体系与数据库迁移机制

如果作为课程设计、毕业设计或软件开发类作品,**当前形态已足以支撑一份较完整的技术说明书与论文初稿**;若要冲击更高质量的工程作品,优先补齐"前端真实联调 + AI 落地 + 自动化测试"三件事即可。
