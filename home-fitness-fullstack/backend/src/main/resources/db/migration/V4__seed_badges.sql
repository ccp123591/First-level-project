-- V4 — 徽章种子
INSERT INTO t_badge (code, name, description, icon, sort_order, criteria_json) VALUES
('first_training', '初次训练', '完成第一次训练',     '🎯', 1, '{"sessions":1}'),
('hundred_reps',   '百次达成', '累计完成 100 次',    '💯', 2, '{"totalReps":100}'),
('seven_streak',   '七日连续', '连续打卡 7 天',      '🔥', 3, '{"streakDays":7}'),
('perfect_score',  '完美评分', '单次评分 95+',       '⭐', 4, '{"bestScore":95}'),
('rhythm_master',  '节奏大师', '节奏评分 100',       '🎵', 5, '{"bestRhythm":100}'),
('thirty_days',    '坚持不懈', '累计 30 天',         '💪', 6, '{"totalDays":30}');
