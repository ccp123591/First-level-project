<script setup>
import { ref, onMounted } from 'vue';
import { adminApi } from '@/api/exercise';
import { useAuthStore } from '@/stores/auth';
import { useAppStore } from '@/stores/app';

const auth = useAuthStore();
const app = useAppStore();

const dashboardData = ref({ users: 0, sessions: 0, todaySessions: 0, dau: 0 });
const users = ref([]);
const keyword = ref('');
const loading = ref(false);

async function loadDashboard() {
  dashboardData.value = (await adminApi.dashboard()) || dashboardData.value;
}
async function loadUsers() {
  const res = await adminApi.users({ page: 1, size: 20, keyword: keyword.value || undefined });
  users.value = res?.items || [];
}
async function toggleBan(u) {
  try {
    if (u.status === 'DISABLED') { await adminApi.unbanUser(u.id); u.status = 'ACTIVE'; app.showToast('已解封', 'success'); }
    else { await adminApi.banUser(u.id); u.status = 'DISABLED'; app.showToast('已封禁', 'success'); }
  } catch (_) { /* 拦截器已提示 */ }
}

onMounted(async () => {
  if (!auth.isAdmin) return;
  loading.value = true;
  try { await loadDashboard(); await loadUsers(); } finally { loading.value = false; }
});
</script>

<template>
  <div class="page-wrap admin-page">
    <div class="page-head">
      <h2>管理后台</h2>
      <p class="sub">实时运营数据与用户管理</p>
    </div>

    <template v-if="auth.isAdmin">
      <div class="dashboard">
        <div class="dash-card">
          <div class="dc-v">{{ dashboardData.users }}</div>
          <div class="dc-l">注册用户</div>
        </div>
        <div class="dash-card">
          <div class="dc-v">{{ dashboardData.sessions }}</div>
          <div class="dc-l">训练总次</div>
        </div>
        <div class="dash-card">
          <div class="dc-v">{{ dashboardData.todaySessions }}</div>
          <div class="dc-l">今日训练</div>
        </div>
        <div class="dash-card">
          <div class="dc-v">{{ dashboardData.dau }}</div>
          <div class="dc-l">今日 DAU</div>
        </div>
      </div>

      <div class="section-head">
        <h3>用户管理</h3>
        <div class="search">
          <input v-model="keyword" type="text" placeholder="搜索邮箱 / 昵称" @keyup.enter="loadUsers" />
          <button @click="loadUsers">搜索</button>
        </div>
      </div>

      <div class="user-list">
        <div v-for="u in users" :key="u.id" class="user-row">
          <div class="u-avatar">{{ (u.nickname || u.email || '?')[0].toUpperCase() }}</div>
          <div class="u-info">
            <div class="u-name">
              {{ u.nickname || '未命名' }}
              <span v-if="u.role === 'ADMIN'" class="u-tag admin">管理员</span>
              <span v-if="u.status === 'DISABLED'" class="u-tag banned">已封禁</span>
            </div>
            <div class="u-email">{{ u.email }}</div>
          </div>
          <button
            :class="['u-action', u.status === 'DISABLED' ? 'unban' : 'ban']"
            :disabled="u.role === 'ADMIN'"
            @click="toggleBan(u)"
          >{{ u.status === 'DISABLED' ? '解封' : '封禁' }}</button>
        </div>
        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="!users.length" class="empty">暂无用户</div>
      </div>
    </template>

    <div v-else class="info-notice">
      <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="10" cy="10" r="7"/><path d="M10 7v4 M10 13h.01"/></svg>
      需要管理员权限才能访问此页面
    </div>
  </div>
</template>

<style scoped>
.page-head { margin-bottom: 16px; }
.page-head h2 { font-family: var(--font-heading); font-size: 28px; font-weight: 700; color: var(--text); letter-spacing: -.04em; }
.page-head .sub { font-size: 12px; color: var(--text-2); margin-top: 4px; }

.dashboard {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
@media (min-width: 640px) { .dashboard { grid-template-columns: repeat(4, 1fr); } }

.dash-card {
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  text-align: center;
}
.dc-v {
  font-size: 28px;
  font-weight: 900;
  background: var(--grad-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.dc-l { font-size: 11px; color: var(--text-3); margin-top: 2px; }

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
@media (min-width: 768px) { .quick-grid { grid-template-columns: repeat(4, 1fr); } }

.qk-card {
  padding: 18px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  cursor: pointer;
  transition: all var(--transition);
}
.qk-card:hover { transform: translateY(-2px); border-color: var(--border-hover); }
.qk-ico {
  width: 40px; height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}
.qk-ico svg { width: 22px; height: 22px; }
.qk-card h4 { font-size: 14px; font-weight: 700; margin-bottom: 2px; }
.qk-card p { font-size: 11px; color: var(--text-3); }

.info-notice {
  margin-top: 20px;
  padding: 12px 14px;
  background: var(--orange-dim);
  border: 1px solid rgba(255, 159, 67, .3);
  border-radius: 12px;
  color: var(--orange);
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.info-notice svg { width: 16px; height: 16px; flex-shrink: 0; }

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.section-head h3 { font-size: 15px; font-weight: 700; }
.search { display: flex; gap: 6px; }
.search input {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--text);
  font-size: 13px;
}
.search button {
  padding: 8px 14px;
  border-radius: 10px;
  background: var(--cyan-dim);
  color: var(--cyan);
  font-size: 12px;
  font-weight: 600;
}

.user-list {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
}
.user-row:last-child { border-bottom: none; }
.u-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--grad-primary);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}
.u-info { flex: 1; min-width: 0; }
.u-name { font-size: 13px; font-weight: 700; display: flex; align-items: center; gap: 6px; }
.u-tag { font-size: 9px; padding: 1px 6px; border-radius: 6px; font-weight: 700; }
.u-tag.admin { background: var(--purple-dim); color: var(--purple); }
.u-tag.banned { background: var(--red-dim, rgba(255,90,90,.15)); color: var(--red); }
.u-email { font-size: 11px; color: var(--text-3); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.u-action {
  padding: 6px 14px;
  border-radius: 100px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.u-action.ban { background: var(--red-dim, rgba(255,90,90,.15)); color: var(--red); }
.u-action.unban { background: var(--green-dim); color: var(--green); }
.u-action:disabled { opacity: .4; }
.empty { text-align: center; padding: 30px 20px; color: var(--text-3); font-size: 13px; }
</style>
