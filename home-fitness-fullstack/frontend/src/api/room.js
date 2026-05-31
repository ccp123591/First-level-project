import client from './client';

/** 环境建模 - 上传 1-3 帧让 vision-svc 推理 */
export const roomApi = {
  scan: (files) => {
    const fd = new FormData();
    files.forEach(f => fd.append('frames', f));
    return client.post('/room/scan', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 45000  // CV 推理可能稍慢
    });
  },
  me: () => client.get('/room/me'),
  history: (page = 0, size = 10) =>
    client.get('/room/history', { params: { page, size } }),
  overrideArea: (areaSqm) =>
    client.post('/room/me/area-override', { areaSqm })
};
