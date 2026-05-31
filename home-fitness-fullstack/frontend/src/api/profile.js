import client from './client';

/** 用户画像 - 后端聚合 30 次 session + feedback */
export const profileApi = {
  me: () => client.get('/users/me/profile'),
  refresh: () => client.post('/users/me/profile/refresh')
};
