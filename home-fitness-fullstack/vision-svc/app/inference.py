"""起步版推理：用图片字节的 hash 衍生稳定的伪随机特征，确保 e2e 链路通。

后续会话可替换为真实 MiDaS-small（深度）+ U2Net（前景）推理，schema 不变。
"""
from __future__ import annotations

import hashlib
from typing import List

from .schema import DiscouragedAction, Obstacle, RoomFeatures

_ROOM_TYPES = ["living-room", "bedroom", "office", "unknown"]
_LIGHTING = ["good", "good", "dim", "poor"]
_FLOOR = ["hardwood", "carpet", "tile", "unknown"]
_OBSTACLES = ["sofa", "chair", "table", "bed", "tv", "wall"]
_SIDES = ["left", "right", "front", "behind"]

# 推荐 / 不适合动作（根据面积估算分档）
_ALL_ACTIONS = ["squat", "stretch", "bridge", "plank", "pushup", "lunge", "jumpingJack"]


def _hash_seq(payloads: List[bytes]) -> bytes:
    h = hashlib.sha256()
    for p in payloads:
        h.update(p)
    return h.digest()


def analyze_frames(payloads: List[bytes]) -> RoomFeatures:
    """占位推理 — 后续替换为真实模型。"""
    if not payloads:
        return RoomFeatures()

    digest = _hash_seq(payloads)
    seed = int.from_bytes(digest[:4], "big")

    # 面积 4 ~ 16 ㎡（合理客厅范围），confidence 0.5 ~ 0.85
    area = 4.0 + (seed % 1200) / 100.0
    confidence = 0.5 + (digest[5] & 0x7F) / 256.0

    room_type = _ROOM_TYPES[digest[6] % len(_ROOM_TYPES)]
    lighting = _LIGHTING[digest[7] % len(_LIGHTING)]
    floor = _FLOOR[digest[8] % len(_FLOOR)]

    # 1 ~ 2 个障碍物
    obstacle_count = 1 + (digest[9] & 1)
    obstacles: List[Obstacle] = []
    for i in range(obstacle_count):
        b = digest[10 + i * 4 : 14 + i * 4]
        label = _OBSTACLES[b[0] % len(_OBSTACLES)]
        side = _SIDES[b[1] % len(_SIDES)]
        distance = round(0.6 + (b[2] & 0x3F) / 30.0, 1)  # 0.6 ~ 2.7 m
        x1 = 50 + (b[3] & 0x3F) * 8
        bbox = [x1, 200, x1 + 320, 520]
        obstacles.append(Obstacle(label=label, bbox=bbox, distanceM=distance, side=side))

    # 推荐 / 不适合 动作 — 按面积分档（与真实 MiDaS 推理上线后保持同样规则）
    recommended, discouraged, warnings = _action_rules(area, lighting, obstacles)

    safety_score = _safety(area, lighting, obstacles)

    return RoomFeatures(
        areaSqm=round(area, 2),
        areaConfidence=round(confidence, 2),
        roomType=room_type,
        lighting=lighting,
        floor=floor,
        obstacles=obstacles,
        recommendedActions=recommended,
        discouragedActions=discouraged,
        safetyScore=safety_score,
        warnings=warnings,
        model="placeholder-v0",
    )


def _action_rules(area: float, lighting: str, obstacles: List[Obstacle]):
    """根据面积 / 光线 / 障碍物推导可行动作清单（也用于真实推理 -- 把规则抽出来）。"""
    static_actions = ["stretch", "plank", "bridge"]  # 几乎所有房间都行
    recommended = list(static_actions)
    discouraged: List[DiscouragedAction] = []
    warnings: List[str] = []

    if area >= 5.0:
        recommended += ["squat", "pushup"]
    if area >= 7.0:
        recommended += ["lunge"]
    if area >= 10.0:
        recommended.append("jumpingJack")
    else:
        discouraged.append(DiscouragedAction(action="jumpingJack",
                                             reason=f"可用面积约 {area:.1f}㎡，跳跃动作不安全"))

    near_obstacle = any(o.distanceM is not None and o.distanceM < 1.0 for o in obstacles)
    if near_obstacle:
        if "lunge" in recommended:
            recommended.remove("lunge")
        discouraged.append(DiscouragedAction(action="lunge", reason="周围障碍物过近，弓步蹲不安全"))
        warnings.append("障碍物距离 < 1m，请清理周边再训练")

    if lighting == "poor":
        warnings.append("光线很差，姿态识别可能不稳定，建议增加照明")

    # 去重保序
    seen = set()
    dedup_recommended = []
    for a in recommended:
        if a not in seen:
            seen.add(a)
            dedup_recommended.append(a)

    return dedup_recommended, discouraged, warnings


def _safety(area: float, lighting: str, obstacles: List[Obstacle]) -> int:
    score = 100
    if area < 4: score -= 20
    elif area < 6: score -= 10
    if lighting == "dim": score -= 5
    elif lighting == "poor": score -= 15
    near = sum(1 for o in obstacles if o.distanceM is not None and o.distanceM < 1.0)
    score -= near * 10
    return max(0, min(100, score))
