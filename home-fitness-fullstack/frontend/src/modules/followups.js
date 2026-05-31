/**
 * 根据 AI 回复内容 + 近期情绪，给出 2-3 个情境化快捷追问。
 * @param {string} reply  AI 这条回复的文本
 * @param {string} [mood] 近期主导情绪 positive | neutral | negative
 */
export function followupsFor(reply, mood) {
  const r = reply || '';
  // 具体话题优先（记忆 / 名字）
  if (/记得|收着|提过|想起|之前|记忆/.test(r)) return ['还记得别的吗？', '我们还聊过什么？'];
  if (/名字|记住|喊你|叫你/.test(r)) return ['那你叫什么？', '今天该练了吗？'];
  // 情绪导向：低落多给"陪聊"向，积极多给"加练"向
  if (mood === 'negative') return ['其实我还好', '陪我聊点别的', '说点开心的'];
  if (mood === 'positive') return ['那今天先练哪个？', '再给我加个挑战', '冲一组试试'];
  // 回复内容导向
  if (/练|动作|计划|深蹲|俯卧|平板|目标|强度|有氧/.test(r)) return ['那今天先练哪个？', '给我排个轻松点的', '我今天没什么力气'];
  if (/休息|喝水|深呼吸|放松|不急|心情|低落|累|陪/.test(r)) return ['其实我还好', '陪我聊点别的', '说点开心的'];
  if (/[？?]\s*$/.test(r)) return ['嗯，是的', '不太想', '换个话题'];
  return ['然后呢？', '今天该练什么？', '随便聊聊'];
}
