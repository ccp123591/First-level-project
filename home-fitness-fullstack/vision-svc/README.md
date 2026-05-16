# vision-svc — FitCoach 环境建模推理服务

Python FastAPI + onnxruntime 起步版本，给 Java 后端的 `VisionClient` 提供 `/infer` 端点。

## 起步版本（v0.1）

- `/healthz` 健康检查
- `POST /infer` 接收 1-3 帧图片 multipart → 返回 `RoomFeatures` JSON
- 当前推理基于图片字节 SHA-256 hash 衍生稳定的伪随机特征，**确保 Java 端 e2e 链路通**；规则与未来真实 MiDaS 推理共用 `_action_rules` / `_safety` 函数。

## 后续升级路径（不在本会话）

1. 下载 `Intel/midas-small` ONNX 权重到 `models/midas-small.onnx`
2. 接入 `onnxruntime.InferenceSession`，把 inference.analyze_frames 里"伪随机"替换为真实深度图
3. 用 MediaPipe Pose 在前端给的人体关键点高度做尺度估计
4. 接入 `xuebinqin/U-2-Net` portrait 版做障碍物分割

## 本地跑

```bash
cd vision-svc
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8081

# 测试
pytest -q
```

## Docker

被根目录 `docker-compose.yml` 拉起：

```bash
docker compose up -d --build vision-svc
curl http://localhost:8081/healthz
```
