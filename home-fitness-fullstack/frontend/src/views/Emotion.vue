<template>
  <div class="page emotion-page">
    <div class="hero">
      <h2>情感记录</h2>
      <p class="hint">写下此刻心情，AI 会自动识别情绪并汇总 7 天趋势。教练会综合考虑你的情感状态调整建议。</p>
    </div>

    <section class="panel">
      <h3>记录一段心情</h3>
      <textarea v-model="text" rows="3" maxlength="500"
                placeholder="例如：今天完成了 15 个深蹲，太棒了，状态非常好！"></textarea>
      <div class="actions">
        <select v-model="source">
          <option value="note">训练笔记</option>
          <option value="post">动态</option>
          <option value="chat">闲聊</option>
        </select>
        <button class="primary" :disabled="busy || !text.trim()" @click="analyze">
          {{ busy ? '分析中…' : '分析并保存' }}
        </button>
      </div>
      <div v-if="latest" class="result">
        <div :class="['badge', `b-${latest.emotion}`]">{{ zhEmotion(latest.emotion) }}</div>
        <div class="score">情感分: <b>{{ latest.score }}</b></div>
        <div v-if="latest.tags?.length" class="tags">
          关键词:
          <span v-for="t in latest.tags" :key="t" class="tag">{{ t }}</span>
        </div>
      </div>
    </section>

    <section class="panel">
      <h3>近 7 天情感分布</h3>
      <div v-if="summary" class="summary">
        <div class="stat"><span>样本</span><b>{{ summary.total }}</b></div>
        <div class="stat"><span>积极</span><b>{{ summary.positive }}</b></div>
        <div class="stat"><span>中性</span><b>{{ summary.neutral }}</b></div>
        <div class="stat"><span>消极</span><b>{{ summary.negative }}</b></div>
        <div class="stat"><span>平均分</span><b>{{ summary.avgScore }}</b></div>
        <div class="stat highlight"><span>主导</span><b>{{ zhEmotion(summary.dominantEmotion) }}</b></div>
      </div>
    </section>

    <section class="panel">
      <h3>最近记录</h3>
      <ul class="history">
        <li v-for="it in history" :key="it.id">
          <div class="row">
            <span :class="['badge-sm', `b-${it.emotion}`]">{{ zhEmotion(it.emotion) }}</span>
            <span class="t">{{ it.text }}</span>
            <span class="meta">{{ fmt(it.createdAt) }}</span>
          </div>
        </li>
        <li v-if="!history.length" class="empty">还没有记录</li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { emotionApi } from '@/api/emotion';
import { useAppStore } from '@/stores/app';

const app = useAppStore();
const text = ref('');
const source = ref('note');
const busy = ref(false);
const latest = ref(null);
const summary = ref(null);
const history = ref([]);

const zhEmotion = e => ({ positive: '积极', neutral: '中性', negative: '消极' }[e] || e);
const fmt = t => t ? new Date(t).toLocaleString('zh-CN', { hour12: false }).slice(5, 16) : '';

async function analyze() {
  busy.value = true;
  try {
    latest.value = await emotionApi.analyze(text.value, source.value);
    text.value = '';
    app.showToast('已记录情感', 'success');
    await loadAll();
  } finally { busy.value = false; }
}

async function loadAll() {
  try {
    const [s, h] = await Promise.all([emotionApi.summary(7), emotionApi.history(0, 10)]);
    summary.value = s;
    history.value = h.items || [];
  } catch (e) { /* toast 已在拦截器里 */ }
}

onMounted(loadAll);
</script>

<style scoped>
.emotion-page { padding: 16px; max-width: 720px; margin: 0 auto; }
.hero { margin-bottom: 16px; }
.hero h2 { margin: 0; color: var(--text-strong); }
.hint { color: var(--text-soft); font-size: 13px; margin-top: 4px; }
.panel { background: var(--bg-card); border-radius: 14px; padding: 16px; margin-bottom: 14px; box-shadow: var(--shadow-sm); }
.panel h3 { margin: 0 0 12px; font-size: 15px; color: var(--text-strong); }
textarea { width: 100%; padding: 10px; border-radius: 10px; border: 1px solid var(--border); background: var(--bg-input); color: var(--text); font: inherit; resize: vertical; }
.actions { display: flex; gap: 10px; margin-top: 10px; align-items: center; }
.actions select { padding: 8px 10px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg-input); color: var(--text); }
button.primary { background: var(--grad-primary); color: #fff; padding: 10px 18px; border: 0; border-radius: 10px; font-weight: 600; cursor: pointer; }
button.primary:disabled { opacity: .55; cursor: not-allowed; }
.result { margin-top: 14px; padding: 12px; background: var(--bg-elev); border-radius: 10px; display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.badge { padding: 6px 12px; border-radius: 18px; font-weight: 600; }
.badge-sm { padding: 2px 8px; border-radius: 10px; font-size: 14px; }
.b-positive { background: rgba(46,160,67,.16); color: #2ea043; }
.b-neutral  { background: rgba(120,140,180,.16); color: #6c7a8c; }
.b-negative { background: rgba(217,87,87,.16); color: #d95757; }
.score b { color: var(--cyan); margin-left: 4px; }
.tags { font-size: 13px; color: var(--text-soft); }
.tag { background: var(--bg-elev); padding: 2px 8px; border-radius: 8px; margin-left: 4px; }
.summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(82px, 1fr)); gap: 10px; }
.stat { display: flex; flex-direction: column; align-items: center; background: var(--bg-elev); padding: 10px; border-radius: 10px; }
.stat span { font-size: 12px; color: var(--text-soft); }
.stat b { font-size: 17px; margin-top: 2px; }
.stat.highlight { background: var(--cyan-dim); }
.history { list-style: none; padding: 0; margin: 0; }
.history li { padding: 10px 0; border-bottom: 1px solid var(--border); }
.row { display: flex; align-items: center; gap: 10px; }
.row .t { flex: 1; color: var(--text); font-size: 14px; }
.row .meta { color: var(--text-soft); font-size: 12px; }
.empty { color: var(--text-soft); text-align: center; padding: 16px 0; }
</style>
