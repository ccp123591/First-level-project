-- V3 — 官方训练计划种子（items_json 含每日明细）
-- 计划 1：新手入门 7 天
INSERT INTO t_plan (title, description, level, cover, days, official, published, author_id, adopt_count, created_at, items_json)
VALUES ('新手入门 7 天', '零基础友好，每天 15 分钟', 'NEWBIE', '#00F0FF', 7, true, true, 1, 128, NOW(),
'[{"day":1,"items":[{"action":"squat","reps":10},{"action":"stretch","reps":10}]},
 {"day":2,"items":[{"action":"pushup","reps":8},{"action":"bridge","reps":12}]},
 {"day":3,"items":[{"action":"jumpingJack","reps":20},{"action":"stretch","reps":10}]},
 {"day":4,"items":[{"action":"squat","reps":12},{"action":"plank","duration":30}]},
 {"day":5,"items":[{"action":"pushup","reps":10},{"action":"lunge","reps":10}]},
 {"day":6,"items":[{"action":"bridge","reps":15},{"action":"stretch","reps":12}]},
 {"day":7,"items":[{"action":"squat","reps":15},{"action":"plank","duration":45}]}]');

-- 计划 2：核心强化 14 天
INSERT INTO t_plan (title, description, level, cover, days, official, published, author_id, adopt_count, created_at, items_json)
VALUES ('核心强化 14 天', '针对核心力量的系统训练', 'INTERMEDIATE', '#7C6AFF', 14, true, true, 1, 56, NOW(),
'[{"day":1,"items":[{"action":"plank","duration":30}]},
 {"day":2,"items":[{"action":"plank","duration":35}]},
 {"day":3,"items":[{"action":"plank","duration":40}]},
 {"day":4,"items":[{"action":"plank","duration":45}]},
 {"day":5,"items":[{"action":"plank","duration":50}]},
 {"day":6,"items":[{"action":"plank","duration":55}]},
 {"day":7,"items":[{"action":"plank","duration":60}]},
 {"day":8,"items":[{"action":"plank","duration":65}]},
 {"day":9,"items":[{"action":"plank","duration":70}]},
 {"day":10,"items":[{"action":"plank","duration":75}]},
 {"day":11,"items":[{"action":"plank","duration":80}]},
 {"day":12,"items":[{"action":"plank","duration":90}]},
 {"day":13,"items":[{"action":"plank","duration":100}]},
 {"day":14,"items":[{"action":"plank","duration":120}]}]');

-- 计划 3：30 天俯卧撑挑战（10 → 100，每天 +3）
INSERT INTO t_plan (title, description, level, cover, days, official, published, author_id, adopt_count, created_at, items_json)
VALUES ('30 天俯卧撑挑战', '从 10 个到 100 个的进阶', 'INTERMEDIATE', '#FF9F43', 30, true, true, 1, 82, NOW(),
'[{"day":1,"items":[{"action":"pushup","reps":10}]},
 {"day":2,"items":[{"action":"pushup","reps":13}]},
 {"day":3,"items":[{"action":"pushup","reps":16}]},
 {"day":4,"items":[{"action":"pushup","reps":19}]},
 {"day":5,"items":[{"action":"pushup","reps":22}]},
 {"day":6,"items":[{"action":"pushup","reps":25}]},
 {"day":7,"items":[{"action":"pushup","reps":28}]},
 {"day":8,"items":[{"action":"pushup","reps":31}]},
 {"day":9,"items":[{"action":"pushup","reps":34}]},
 {"day":10,"items":[{"action":"pushup","reps":37}]},
 {"day":11,"items":[{"action":"pushup","reps":40}]},
 {"day":12,"items":[{"action":"pushup","reps":43}]},
 {"day":13,"items":[{"action":"pushup","reps":46}]},
 {"day":14,"items":[{"action":"pushup","reps":49}]},
 {"day":15,"items":[{"action":"pushup","reps":52}]},
 {"day":16,"items":[{"action":"pushup","reps":55}]},
 {"day":17,"items":[{"action":"pushup","reps":58}]},
 {"day":18,"items":[{"action":"pushup","reps":61}]},
 {"day":19,"items":[{"action":"pushup","reps":64}]},
 {"day":20,"items":[{"action":"pushup","reps":67}]},
 {"day":21,"items":[{"action":"pushup","reps":70}]},
 {"day":22,"items":[{"action":"pushup","reps":73}]},
 {"day":23,"items":[{"action":"pushup","reps":76}]},
 {"day":24,"items":[{"action":"pushup","reps":79}]},
 {"day":25,"items":[{"action":"pushup","reps":82}]},
 {"day":26,"items":[{"action":"pushup","reps":85}]},
 {"day":27,"items":[{"action":"pushup","reps":88}]},
 {"day":28,"items":[{"action":"pushup","reps":91}]},
 {"day":29,"items":[{"action":"pushup","reps":95}]},
 {"day":30,"items":[{"action":"pushup","reps":100}]}]');

-- 计划 4：柔韧性提升 10 天
INSERT INTO t_plan (title, description, level, cover, days, official, published, author_id, adopt_count, created_at, items_json)
VALUES ('柔韧性提升', '每天 20 分钟拉伸', 'NEWBIE', '#00E58A', 10, true, true, 1, 45, NOW(),
'[{"day":1,"items":[{"action":"stretch","reps":10},{"action":"bridge","reps":10}]},
 {"day":2,"items":[{"action":"stretch","reps":12},{"action":"bridge","reps":12}]},
 {"day":3,"items":[{"action":"stretch","reps":14},{"action":"bridge","reps":14}]},
 {"day":4,"items":[{"action":"stretch","reps":16},{"action":"bridge","reps":16}]},
 {"day":5,"items":[{"action":"stretch","reps":18},{"action":"bridge","reps":18}]},
 {"day":6,"items":[{"action":"stretch","reps":20},{"action":"bridge","reps":20}]},
 {"day":7,"items":[{"action":"stretch","reps":22},{"action":"bridge","reps":22}]},
 {"day":8,"items":[{"action":"stretch","reps":24},{"action":"bridge","reps":24}]},
 {"day":9,"items":[{"action":"stretch","reps":26},{"action":"bridge","reps":26}]},
 {"day":10,"items":[{"action":"stretch","reps":30},{"action":"bridge","reps":30}]}]');
