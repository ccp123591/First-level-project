import client from './client';

/**
 * AI 教练：基于训练数据生成个性化反馈，以及陪伴聊天 + 记忆唤起。
 */
export const coachApi = {
  feedback:   (sessionId) => client.post(`/coach/feedback`, { sessionId }),
  suggestion: ()          => client.get('/coach/suggestion'),
  weeklyPlan: ()          => client.get('/coach/weekly-plan'),
  history:    (params)    => client.get('/coach/history', { params }),

  /**
   * 陪伴聊天（多轮 + RAG 记忆）。
   * @param {string} message  本次用户输入
   * @param {Array<{role:'user'|'assistant', content:string}>} history 最近若干轮，可空
   * @returns {{reply, provider, tokensUsed, recalled[]}}
   */
  chat: (message, history = []) => client.post('/coach/chat', { message, history }),

  /** 叙旧 — 按时间近的最近聊天记忆做老朋友式回顾 */
  reminisce: () => client.post('/coach/reminisce')
};
