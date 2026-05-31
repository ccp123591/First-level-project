import client from './client';

/**
 * TTS - 后端双方案：
 *   - 返回 audioBase64 + mimeType   → 用 Audio data: URL 播放（mimo）
 *   - 返回 fallbackText 仅           → 用 window.speechSynthesis 朗读（browser）
 */
export const ttsApi = {
  speak: (text, voice) => client.post('/tts/speak', { text, voice }),
  speakFeedback: (feedbackId) => client.post(`/tts/coach/feedback/${feedbackId}`)
};

let currentAudio = null;

/** 停止当前正在播放的 TTS（音频 + 浏览器朗读）。 */
export function stopTts() {
  if (currentAudio) {
    try { currentAudio.pause(); currentAudio.src = ''; } catch (_) {}
    currentAudio = null;
  }
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    try { window.speechSynthesis.cancel(); } catch (_) {}
  }
}

/** 工具：把后端 TtsResult 播出来。同一时刻只播一条，返回的 Promise 在播放结束时 resolve。 */
export function playTtsResult(result) {
  stopTts();                       // 先停掉上一条，避免叠音
  if (!result) return Promise.resolve();
  if (result.audioBase64 && result.mimeType) {
    return new Promise((resolve) => {
      const audio = new Audio(`data:${result.mimeType};base64,${result.audioBase64}`);
      currentAudio = audio;
      const done = () => { if (currentAudio === audio) currentAudio = null; resolve(); };
      audio.onended = done;
      audio.onerror = done;
      audio.play().catch(done);
    });
  }
  // browser fallback —— 用浏览器原生 Web Speech API
  if (result.fallbackText && 'speechSynthesis' in window) {
    const u = new SpeechSynthesisUtterance(result.fallbackText);
    u.lang = 'zh-CN';
    u.rate = 0.95;
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(u);
  }
  return Promise.resolve();
}
