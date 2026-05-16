# 环境建模 S1 — 训练空间评估 + 动作适配

**日期**: 2026-05-16  
**作用域**: 整个 FitCoach 项目的"环境建模"功能第一期。后续 S2 风险点 / S3 活动量画像 / S4 跌倒检测各自独立 spec。

---

## 1. 目标

通过摄像头拍 1-3 帧老人家训练空间 → 后端语义化输出 → 给 AI 教练增加"在 6㎡ 客厅、左侧有沙发"的上下文 → 教练给出该空间下安全可行的动作建议。

## 2. 非目标（明确不做）

- 实时跌倒检测（S4 做）
- 长期活动量监测（S3 做）
- 风险点清单（S2 做）
- 前端引导页 UI（本轮只做后端 + Swagger 调通，前端后续会话做）
- 米制面积精确测量（MVP 用人体高度参考估计，带 confidence；老人/家属可手动 override）

## 3. 架构

```
前端 (后续) ─multipart 3 jpg─▶ Spring Boot
                                  │
                                  ├─ RoomLayoutService
                                  │  ├─ UploadValidator (复用)
                                  │  ├─ VisionClient ─HTTP─▶ vision-svc (Python FastAPI + ONNX)
                                  │  ├─ 落库 t_room_layout_snapshot
                                  │  └─ 删原图
                                  │
                                  └─ CoachContext.roomLayoutSummary 注入（弱依赖钩子）
```

## 4. 模块边界

| 模块 | 路径 | 职责 |
|---|---|---|
| 业务 | `com.fitcoach.room` | RoomLayoutSnapshot 实体/Repo/Service/Controller/DTO |
| 基础设施 | `com.fitcoach.infra.vision` | VisionClient (HTTP 调用 sidecar) + RoomFeatures DTO |
| sidecar | `vision-svc/` | Python FastAPI + onnxruntime；MVP 占位逻辑，后续接 MiDaS-small + U2Net |
| 部署 | `docker-compose.yml` | 新 service `vision-svc` 端口 8081（容器内） |
| 集成 | `coach/CoachService` | ObjectProvider<RoomLayoutRepository> 弱依赖，buildContext 注入 summary |

## 5. 数据模型

### 表 `t_room_layout_snapshot`（Flyway V7）

```sql
id, user_id, scan_id, features_json, summary_text, area_sqm, safety_score,
source, vision_model, captured_at, processed_at
+ INDEX (user_id, captured_at)
```

保留扫描历史（不 upsert）。Coach 永远读最新一行。

### features_json 结构

```json
{
  "areaSqm": 6.2, "areaConfidence": 0.7,
  "roomType": "living-room|bedroom|office|unknown",
  "lighting": "good|dim|poor",
  "floor": "hardwood|carpet|tile|unknown",
  "obstacles": [{"label":"sofa","bbox":[..],"distanceM":1.2,"side":"left"}],
  "recommendedActions": ["squat","stretch","bridge","plank"],
  "discouragedActions": [{"action":"jumpingJack","reason":"空间过小"}],
  "safetyScore": 78,
  "warnings": ["左侧 1.2m 处沙发，跳跃动作不安全"]
}
```

## 6. REST API

| Method | Path | 说明 |
|---|---|---|
| POST | `/api/room/scan` | multipart 上传 1-3 张 jpg/png → 返回 RoomLayoutResponse |
| GET | `/api/room/me` | 返回当前用户最新一份 RoomLayout（404 若无） |
| GET | `/api/room/history?page=&size=` | 分页历史 |
| POST | `/api/room/me/area-override` | 手动覆盖 areaSqm（老人/家属知道实际面积） |

## 7. CV 路径（Python sidecar）

**MVP（本轮）**：FastAPI POST `/infer` 接收 multipart 图片 → 返回**占位** RoomFeatures（合理默认值 + 根据图片字节 hash 给伪随机变化，保证 Java 端 e2e 链路通）。

**后续升级**（独立 commit / 后续会话）：
- 接入 `Intel/midas-small` ONNX 做相对深度估计
- 接入 `xuebinqin/U-2-Net` portrait 版做前景/障碍物分割
- 人体高度参考（MediaPipe Pose 在前端给出 keypoint 像素高度 / 后端深度做尺度倒推）算米制面积
- 简单房间分类（光照统计 + 主色调）

## 8. 隐私

- 上传到后端是 multipart，复用 `UploadValidator`（Tika MIME + 5MB 上限 + 扩展名白名单）
- 后端处理时存到 `${upload.dir}/room-scan/` 临时目录
- vision-svc 返回结果**后立即删原图**（finally 块）
- DB 只存抽象 JSON，**永不存图片字节**

## 9. AI Coach 集成

`CoachContext` 新增字段：
```java
private String roomLayoutSummary;     // "客厅约 6.2㎡，光线良好，左侧 1.2m 有沙发"
private List<String> roomRecommendedActions;
```

`CoachService.buildContext` 增加（与 emotion / profile / memory 同样的 ObjectProvider 弱依赖模式）：
```java
try {
  RoomLayoutRepository repo = roomRepoProvider.getIfAvailable();
  if (repo != null) repo.findTopByUserIdOrderByCapturedAtDesc(userId)
      .ifPresent(s -> b.roomLayoutSummary(s.getSummaryText()) ...)
} catch (Exception ignored) {}
```

MimoCoachProvider 序列化整个 context 时自动带进 prompt。

## 10. 错误处理

- vision-svc 不可用 → `BusinessException(503, "环境识别服务暂不可用")`
- 超时 (默认 30s) → 同上
- 图片解析失败 → `BusinessException(400, "图片格式错误")`
- 上传超大 → 复用 `MaxUploadSizeExceededException` → 已有 413 handler

## 11. 测试

- `UploadValidator` 已覆盖
- `VisionClient` 单测：`@RestClientTest` + MockRestServiceServer（参考 MimoCoachProviderTest 套路）
- `RoomLayoutService` 单测：Mockito mock VisionClient + Repo
- `RoomControllerIntegrationTest`：SpringBootTest + MockMvc，hit /api/room/scan，验证返回结构 + 持久化
- Python sidecar：FastAPI TestClient + pytest 一个 smoke 测试

## 12. Deployment

`docker-compose.yml` 加：
```yaml
vision-svc:
  build: ./vision-svc
  ports: ["8081:8081"]
  healthcheck: GET /healthz
backend:
  environment:
    FITCOACH_VISION_BASE_URL: http://vision-svc:8081
  depends_on:
    vision-svc: { condition: service_healthy }
```

## 13. 未涵盖 / 风险

- **前端 UX**：本轮只给后端，前端引导页（拍 3 帧 / 实时预览）后续会话做。
- **米制面积精度**：MVP 用人体参考 + confidence 字段；老人手动 override 兜底。
- **ONNX 模型下载**：sidecar Dockerfile 在国内构建可能慢；MVP 不下载，后续升级时再处理。
- **多语言扩展**：summary_text 目前只生成中文。
