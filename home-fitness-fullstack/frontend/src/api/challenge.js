import client from './client';

export const challengeApi = {
  list: () => client.get('/challenges'),
  detail: (id) => client.get(`/challenges/${id}`),
  join: (id) => client.post(`/challenges/${id}/join`),
  rank: (id, limit = 20) => client.get(`/challenges/${id}/rank`, { params: { limit } })
};
