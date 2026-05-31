<template>
  <div class="page room-page">
    <div class="hero">
      <h2>环境扫描</h2>
      <p class="hint">拍 1-3 张房间照片，AI 帮你算出可用面积 + 推荐适合此空间的动作。原图不存储。</p>
    </div>

    <section class="panel">
      <h3>上传照片</h3>
      <input ref="picker" type="file" accept="image/png,image/jpeg,image/webp" multiple
             @change="onPick" hidden>
      <button class="primary" @click="$refs.picker.click()" :disabled="busy">
        {{ busy ? '识别中…' : '选 1-3 张照片' }}
      </button>
      <div v-if="picked.length" class="thumbs">
        <img v-for="(u, i) in picked" :key="i" :src="u" alt="">
      </div>
      <button v-if="picked.length" class="ghost" @click="scan" :disabled="busy">
        开始识别
      </button>
    </section>

    <section v-if="result" class="panel result">
      <h3>识别结果 <small>model={{ result.visionModel }}</small></h3>
      <div class="summary">{{ result.summaryText }}</div>
      <div class="grid">
        <div class="stat"><span>可用面积</span><b>{{ result.areaSqm || '—' }} ㎡</b>
          <em v-if="result.features?.areaConfidence">置信 {{ result.features.areaConfidence }}</em></div>
        <div class="stat"><span>安全分</span><b>{{ result.safetyScore }} / 100</b></div>
        <div class="stat"><span>光线</span><b>{{ zhLight(result.features?.lighting) }}</b></div>
        <div class="stat"><span>类型</span><b>{{ zhRoom(result.features?.roomType) }}</b></div>
      </div>
      <div v-if="result.features?.obstacles?.length" class="block">
        <h4>障碍物</h4>
        <ul class="obstacles">
          <li v-for="(o, i) in result.features.obstacles" :key="i">
            <b>{{ zhObs(o.label) }}</b>
            <span>{{ zhSide(o.side) }}{{ o.distanceM ? ` ${o.distanceM}m` : '' }}</span>
          </li>
        </ul>
      </div>
      <div v-if="result.features?.recommendedActions?.length" class="block">
        <h4>推荐动作</h4>
        <div class="chips green">
          <span v-for="a in result.features.recommendedActions" :key="a">{{ zhAction(a) }}</span>
        </div>
      </div>
      <div v-if="result.features?.discouragedActions?.length" class="block">
        <h4>不适合</h4>
        <div class="chips red">
          <span v-for="d in result.features.discouragedActions" :key="d.action">
            {{ zhAction(d.action) }} <small>· {{ d.reason }}</small>
          </span>
        </div>
      </div>
      <div v-if="result.features?.warnings?.length" class="warnings">
        注意：{{ result.features.warnings.join(' / ') }}
      </div>
      <div class="override">
        <label>实际面积更准？手动填写覆盖：</label>
        <input v-model.number="overrideValue" type="number" step="0.1" min="0.1" max="500">
        <button class="ghost" @click="doOverride" :disabled="!overrideValue">覆盖</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { roomApi } from '@/api/room';
import { useAppStore } from '@/stores/app';

const app = useAppStore();
const picker = ref(null);
const picked = ref([]);  // ObjectURL preview
const files = ref([]);
const result = ref(null);
const busy = ref(false);
const overrideValue = ref('');

function onPick(e) {
  const list = Array.from(e.target.files || []).slice(0, 3);
  files.value = list;
  picked.value = list.map(f => URL.createObjectURL(f));
}

async function scan() {
  if (!files.value.length) return;
  busy.value = true;
  try {
    result.value = await roomApi.scan(files.value);
    app.showToast('环境识别完成', 'success');
  } finally { busy.value = false; }
}

async function doOverride() {
  if (!overrideValue.value) return;
  result.value = await roomApi.overrideArea(Number(overrideValue.value));
  overrideValue.value = '';
  app.showToast('已覆盖面积', 'success');
}

async function loadLatest() {
  try { result.value = await roomApi.me(); } catch (_) {}
}
onMounted(loadLatest);

const zhRoom  = t => ({ 'living-room':'客厅','bedroom':'卧室','office':'书房','unknown':'未知' }[t] || t || '—');
const zhLight = t => ({ good:'良好', dim:'偏暗', poor:'很差', unknown:'未知' }[t] || t || '—');
const zhSide  = t => ({ left:'左侧', right:'右侧', front:'正前方', behind:'后方' }[t] || t || '附近');
const zhObs   = t => ({ sofa:'沙发', chair:'椅子', table:'桌子','coffee-table':'茶几', bed:'床', tv:'电视', wall:'墙', door:'门', carpet:'地毯' }[t] || t || '物品');
const zhAction = a => ({ squat:'深蹲', pushup:'俯卧撑', plank:'平板支撑', stretch:'前屈伸展', lunge:'弓步蹲', bridge:'臀桥', jumpingJack:'开合跳' }[a] || a);
</script>

<style scoped>
.room-page { padding: 16px; max-width: 760px; margin: 0 auto; }
.hero h2 { margin: 0; color: var(--text-strong); }
.hint { color: var(--text-soft); font-size: 13px; }
.panel { background: var(--bg-card); border-radius: 14px; padding: 16px; margin-top: 14px; box-shadow: var(--shadow-sm); }
.panel h3 { margin: 0 0 12px; font-size: 15px; }
.panel h3 small { color: var(--text-soft); font-weight: 400; font-size: 11px; margin-left: 8px; }
button.primary { background: var(--grad-primary); color: #fff; padding: 10px 22px; border: 0; border-radius: 10px; font-weight: 600; cursor: pointer; }
button.primary:disabled { opacity: .55; cursor: not-allowed; }
button.ghost { background: transparent; border: 1px solid var(--cyan); color: var(--cyan); padding: 8px 16px; border-radius: 10px; cursor: pointer; margin-top: 10px; }
.thumbs { display: flex; gap: 10px; margin: 12px 0 8px; flex-wrap: wrap; }
.thumbs img { width: 110px; height: 110px; object-fit: cover; border-radius: 10px; }
.result .summary { padding: 12px; background: var(--cyan-dim); border-radius: 10px; color: var(--text); font-weight: 500; margin-bottom: 14px; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 10px; }
.stat { background: var(--bg-elev); padding: 12px; border-radius: 10px; display: flex; flex-direction: column; align-items: flex-start; }
.stat span { font-size: 12px; color: var(--text-soft); }
.stat b { font-size: 18px; color: var(--text-strong); margin-top: 2px; }
.stat em { font-size: 11px; color: var(--text-soft); margin-top: 2px; font-style: normal; }
.block { margin-top: 14px; }
.block h4 { margin: 0 0 6px; font-size: 13px; color: var(--text-soft); }
.obstacles { list-style: none; padding: 0; margin: 0; }
.obstacles li { padding: 6px 0; display: flex; justify-content: space-between; border-bottom: 1px solid var(--border); }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chips span { padding: 4px 12px; border-radius: 14px; font-size: 13px; }
.chips.green span { background: rgba(46,160,67,.16); color: #2ea043; }
.chips.red span { background: rgba(217,87,87,.16); color: #d95757; }
.chips.red small { color: var(--text-soft); }
.warnings { margin-top: 12px; padding: 10px; background: rgba(255,180,0,.12); color: #c08400; border-radius: 8px; font-size: 13px; }
.override { margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--border); display: flex; gap: 10px; align-items: center; font-size: 13px; flex-wrap: wrap; }
.override label { color: var(--text-soft); }
.override input { padding: 6px 10px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg-input); color: var(--text); width: 100px; }
.override button { margin-top: 0; }
</style>
