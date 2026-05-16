from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_healthz():
    r = client.get("/healthz")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_infer_one_frame_returns_valid_features():
    files = [("frames", ("a.jpg", b"\xff\xd8\xff\xe0fake", "image/jpeg"))]
    r = client.post("/infer", files=files)
    assert r.status_code == 200
    data = r.json()
    assert data["model"] == "placeholder-v0"
    assert 4.0 <= data["areaSqm"] <= 16.0
    assert data["safetyScore"] is not None and 0 <= data["safetyScore"] <= 100
    assert isinstance(data["recommendedActions"], list)
    assert len(data["recommendedActions"]) >= 1


def test_infer_deterministic_same_input_same_output():
    files = [("frames", ("a.jpg", b"deterministic-bytes", "image/jpeg"))]
    r1 = client.post("/infer", files=files)
    r2 = client.post("/infer", files=files)
    assert r1.json() == r2.json()


def test_infer_rejects_non_image_content_type():
    files = [("frames", ("a.txt", b"hi", "text/plain"))]
    r = client.post("/infer", files=files)
    assert r.status_code == 400


def test_infer_rejects_more_than_3():
    files = [("frames", (f"{i}.jpg", b"x", "image/jpeg")) for i in range(4)]
    r = client.post("/infer", files=files)
    assert r.status_code == 400


def test_jumpingjack_discouraged_in_small_room():
    # 用一组特定字节让面积偏小（hash 决定的）
    files = [("frames", ("a.jpg", b"\x00", "image/jpeg"))]
    r = client.post("/infer", files=files).json()
    if r["areaSqm"] < 10.0:
        actions = [d["action"] for d in r["discouragedActions"]]
        assert "jumpingJack" in actions
