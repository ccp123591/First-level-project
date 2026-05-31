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

/** 工具：把后端 TtsResult 直接播出来。 */
export function playTtsResult(result) {
  if (!result) return Promise.resolve();
  if (result.audioBase64 && result.mimeType) {
    const audio = new Audio(`data:${result.mimeType};base64,${result.audioBase64}`);
    return audio.play();
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
