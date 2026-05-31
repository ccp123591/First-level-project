import client from './client';

/** 情感计算 - lexicon / 未来 mimo-vl */
export const emotionApi = {
  analyze: (text, source = 'note', refId = null) =>
    client.post('/emotion/analyze', { text, source, refId }),
  history: (page = 0, size = 10) =>
    client.get('/emotion/history', { params: { page, size } }),
  summary: (days = 7) =>
    client.get('/emotion/summary', { params: { days } })
};
