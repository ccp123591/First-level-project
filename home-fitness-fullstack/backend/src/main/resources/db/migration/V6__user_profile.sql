-- V6 — 用户画像聚合存储。aggregate 模式由 ProfileExtractionService 周期性刷新。
CREATE TABLE IF NOT EXISTS t_user_profile (
  user_id BIGINT NOT NULL PRIMARY KEY,
  profile_json TEXT,
  summary_text VARCHAR(800),
  summarizer VARCHAR(16) DEFAULT 'aggregate',
  version INT NOT NULL DEFAULT 1,
  updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
