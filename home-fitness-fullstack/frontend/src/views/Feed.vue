<script setup>
import { ref } from 'vue';

const posts = ref([
  {
    id: 1,
    user: '健身达人小王',
    time: '10 分钟前',
    content: '今天完成了 50 个深蹲，综合评分 92，感觉状态不错！',
    action: '深蹲',
    reps: 50,
    score: 92,
    liked: false,
    likes: 12,
    comments: 3
  },
  {
    id: 2,
    user: '瑜伽妹妹',
    time: '1 小时前',
    content: '连续打卡第 7 天了🎉，今天解锁了"七日连续"徽章！',
    action: '前屈伸展',
    reps: 30,
    score: 88,
    liked: true,
    likes: 28,
    comments: 5
  }
]);

function toggleLike(p) {
  p.liked = !p.liked;
  p.likes += p.liked ? 1 : -1;
}

// 根据姓名生成暖色系 HSL 渐变，保持与排行榜一致的视觉语言
function avatarStyle(name) {
  let h = 0;
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) | 0;
  const hue = 15 + (Math.abs(h) % 40);
  const c1 = `hsl(${hue}, 52%, 62%)`;
  const c2 = `hsl(${(hue + 340) % 360}, 42%, 44%)`;
  return { background: `linear-gradient(135deg, ${c1}, ${c2})` };
}
</script>

<template>
  <div class="page-wrap feed-page">
    <div class="page-head">
      <h2>训练动态</h2>
      <p class="sub">看看大家都在练什么</p>
    </div>

    <div v-for="p in posts" :key="p.id" class="post-card">
      <div class="post-head">
        <div class="p-avatar" :style="avatarStyle(p.user)">{{ p.user[0] }}</div>
        <div>
          <div class="p-user">{{ p.user }}</div>
          <div class="p-time">{{ p.time }}</div>
        </div>
      </div>
      <div class="post-content">{{ p.content }}</div>
      <div class="workout-card">
        <div class="w-action">
          <div class="w-dot"></div>
          <span>{{ p.action }}</span>
        </div>
        <div class="w-stats">
          <div><span>{{ p.reps }}</span><small>次</small></div>
          <div><span>{{ p.score }}</span><small>分</small></div>
        </div>
      </div>
      <div class="post-actions">
        <button :class="['a-btn', p.liked ? 'liked' : '']" @click="toggleLike(p)">
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10 17s-5.5-3-7-7 1.5-7 4-7 3 2 3 2 .5-2 3-2 5.5 3 4 7-7 7-7 7z" :fill="p.liked ? 'currentColor' : 'none'"/>
          </svg>
          {{ p.likes }}
        </button>
        <button class="a-btn">
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><path d="M3 12a8 8 0 1 1 3.5 6.6L3 19l.5-3.5A8 8 0 0 1 3 12z"/></svg>
          {{ p.comments }}
        </button>
        <button class="a-btn">
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><circle cx="5" cy="10" r="2"/><circle cx="15" cy="5" r="2"/><circle cx="15" cy="15" r="2"/><path d="M7 9l6-3 M7 11l6 3"/></svg>
          分享
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-head { margin-bottom: 16px; }
.page-head h2 { font-family: var(--font-heading); font-size: 28px; font-weight: 700; color: var(--text); letter-spacing: -.04em; }
.page-head .sub { font-size: 12px; color: var(--text-2); margin-top: 4px; }

.post-card {
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  margin-bottom: 12px;
}
.post-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.p-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  color: #fff;
  font-weight: 800;
  display: flex; align-items: center; justify-content: center;
  position: relative;
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .28), 0 2px 8px rgba(60, 44, 30, .18);
}
.p-avatar::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 22%, rgba(255, 255, 255, .32), transparent 55%);
  pointer-events: none;
}
.p-user { font-size: 13px; font-weight: 700; }
.p-time { font-size: 11px; color: var(--text-3); }

.post-content {
  font-size: 13px;
  color: var(--text);
  line-height: 1.6;
  margin-bottom: 12px;
}

.workout-card {
  padding: 12px 14px;
  background: linear-gradient(135deg, var(--cyan-dim), var(--purple-dim));
  border: 1px solid rgba(217, 119, 87, .15);
  border-radius: 12px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.w-action {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
}
.w-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--cyan);
  box-shadow: 0 0 8px var(--cyan);
}
.w-stats { display: flex; gap: 14px; }
.w-stats div { text-align: center; }
.w-stats span { font-size: 16px; font-weight: 800; color: var(--cyan); }
.w-stats small { display: block; font-size: 9px; color: var(--text-3); }

.post-actions {
  display: flex;
  gap: 14px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.a-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-2);
  transition: color var(--transition);
}
.a-btn svg { width: 16px; height: 16px; }
.a-btn:hover { color: var(--text); }
.a-btn.liked { color: var(--red); }
</style>
