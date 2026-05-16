"""FastAPI 启动模块：把推理委托给 inference.analyze_frames。"""
from __future__ import annotations

import logging
from typing import List

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse

from .inference import analyze_frames
from .schema import RoomFeatures

logger = logging.getLogger("vision-svc")
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")

app = FastAPI(title="FitCoach vision-svc", version="0.1.0")


@app.get("/healthz")
def healthz() -> dict:
    return {"status": "ok"}


@app.post("/infer", response_model=RoomFeatures)
async def infer(frames: List[UploadFile] = File(...)) -> RoomFeatures:
    if not frames:
        raise HTTPException(status_code=400, detail="No frames uploaded")
    if len(frames) > 3:
        raise HTTPException(status_code=400, detail="At most 3 frames")
    payloads = []
    for f in frames:
        if not (f.content_type and f.content_type.startswith("image/")):
            raise HTTPException(status_code=400, detail=f"Unsupported content-type: {f.content_type}")
        payloads.append(await f.read())
    try:
        result = analyze_frames(payloads)
        logger.info("inference ok: model=%s area=%.2f safety=%s",
                    result.model, result.areaSqm or 0.0, result.safetyScore)
        return result
    except Exception as e:  # pragma: no cover - defensive
        logger.exception("inference failed: %s", e)
        return JSONResponse(status_code=500, content={"detail": "inference failure"})
