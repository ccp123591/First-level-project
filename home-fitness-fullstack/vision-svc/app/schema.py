"""与 Java RoomFeatures 镜像的 Pydantic 模型。"""
from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field


class Obstacle(BaseModel):
    label: str
    bbox: List[int] = Field(..., min_length=4, max_length=4)
    distanceM: Optional[float] = None
    side: Optional[str] = None


class DiscouragedAction(BaseModel):
    action: str
    reason: str


class RoomFeatures(BaseModel):
    areaSqm: Optional[float] = None
    areaConfidence: Optional[float] = None
    roomType: str = "unknown"
    lighting: str = "good"
    floor: str = "unknown"
    obstacles: List[Obstacle] = []
    recommendedActions: List[str] = []
    discouragedActions: List[DiscouragedAction] = []
    safetyScore: Optional[int] = None
    warnings: List[str] = []
    model: str = "placeholder-v0"
