-- V1 — FitCoach 完整 schema（覆盖现有实体 + 6 张新表）
-- 仅 prod profile 使用；dev 继续 H2 + data.sql。

CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(64) UNIQUE,
  phone VARCHAR(20),
  password_hash VARCHAR(128),
  nickname VARCHAR(32) NOT NULL,
  avatar VARCHAR(256),
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  login_type VARCHAR(16),
  open_id VARCHAR(128),
  device_id VARCHAR(128),
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  weekly_goal INT DEFAULT 50,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_email (email),
  INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user_follow (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  follower_id BIGINT NOT NULL,
  following_id BIGINT NOT NULL,
  created_at DATETIME,
  UNIQUE KEY uk_follow (follower_id, following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_session (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  action_label VARCHAR(32),
  reps INT NOT NULL,
  target_reps INT,
  duration INT NOT NULL,
  score INT,
  rhythm_score INT,
  stability_score INT,
  depth_score INT,
  symmetry_score INT,
  completion_score INT,
  session_date VARCHAR(32),
  notes VARCHAR(512),
  created_at DATETIME,
  INDEX idx_user_date (user_id, session_date),
  INDEX idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_exercise (
  code VARCHAR(32) NOT NULL PRIMARY KEY,
  name VARCHAR(32) NOT NULL,
  description VARCHAR(256),
  kind VARCHAR(32),
  icon VARCHAR(256),
  video_url VARCHAR(512),
  landmarks_json TEXT,
  default_threshold_down INT,
  default_threshold_up INT,
  enabled BOOLEAN DEFAULT TRUE,
  sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_plan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(64) NOT NULL,
  description VARCHAR(256),
  level VARCHAR(16),
  cover VARCHAR(128),
  days INT,
  items_json TEXT,
  official BOOLEAN DEFAULT FALSE,
  published BOOLEAN DEFAULT TRUE,
  author_id BIGINT,
  adopt_count INT DEFAULT 0,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user_plan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  progress_day INT NOT NULL DEFAULT 0,
  status VARCHAR(16) DEFAULT 'ACTIVE',
  adopted_at DATETIME,
  updated_at DATETIME,
  UNIQUE KEY uk_user_plan (user_id, plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_coach_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  session_id BIGINT,
  review VARCHAR(500),
  suggestion VARCHAR(500),
  encouragement VARCHAR(300),
  next_goal VARCHAR(200),
  provider VARCHAR(32),
  tokens_used INT DEFAULT 0,
  created_at DATETIME,
  INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_badge (
  code VARCHAR(32) NOT NULL PRIMARY KEY,
  name VARCHAR(32) NOT NULL,
  description VARCHAR(128),
  icon VARCHAR(16),
  criteria_json TEXT,
  sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user_badge (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  badge_code VARCHAR(32) NOT NULL,
  unlocked_at DATETIME,
  UNIQUE KEY uk_user_badge (user_id, badge_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_post (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  session_id BIGINT,
  content VARCHAR(1000),
  likes INT DEFAULT 0,
  comments_count INT DEFAULT 0,
  visibility VARCHAR(16) DEFAULT 'PUBLIC',
  created_at DATETIME,
  INDEX idx_post_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_post_comment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at DATETIME,
  INDEX idx_pc_post_created (post_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_post_like (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME,
  UNIQUE KEY uk_post_like (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_refresh_token_blacklist (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  jti VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  expires_at DATETIME NOT NULL,
  reason VARCHAR(64),
  created_at DATETIME,
  UNIQUE KEY uk_jti (jti),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_verify_code (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  target VARCHAR(128) NOT NULL,
  channel VARCHAR(16) NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  code_hash VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME,
  created_at DATETIME,
  INDEX idx_target (target)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_password_reset (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME,
  created_at DATETIME,
  UNIQUE KEY uk_token (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_login_attempt (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  identifier VARCHAR(128) NOT NULL,
  ip VARCHAR(64),
  outcome VARCHAR(16) NOT NULL,
  created_at DATETIME,
  INDEX idx_identifier (identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_challenge (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(64) NOT NULL,
  description VARCHAR(256),
  action VARCHAR(32) NOT NULL,
  target_reps INT NOT NULL,
  start_date VARCHAR(32),
  end_date VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  cover VARCHAR(128),
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_challenge_participant (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  challenge_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  joined_at DATETIME,
  progress_reps INT DEFAULT 0,
  completed BOOLEAN DEFAULT FALSE,
  UNIQUE KEY uk_chal_user (challenge_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
