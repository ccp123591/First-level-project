-- V7 — 环境建模快照（Phase 11 / S1）
CREATE TABLE IF NOT EXISTS t_room_layout_snapshot (
  id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT NOT NULL,
  scan_id       VARCHAR(40) NOT NULL,
  features_json TEXT NOT NULL,
  summary_text  VARCHAR(400) NOT NULL,
  area_sqm      DECIMAL(5,2),
  safety_score  INT,
  source        VARCHAR(16) DEFAULT 'camera',
  vision_model  VARCHAR(32),
  captured_at   DATETIME NOT NULL,
  processed_at  DATETIME NOT NULL,
  INDEX idx_room_user_captured (user_id, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
