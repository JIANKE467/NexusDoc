import request from './request';

export function getAiConfig() {
  return request.get('/ai/config');
}

export function updateAiConfig(data) {
  return request.post('/ai/config', data);
}

export function testAiConfig(prompt = '请用一句话回复：文枢 NexusDoc AI 已接入。') {
  return request.post('/ai/test', { prompt });
}
