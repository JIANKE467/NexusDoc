import request from './request';

export function getFileMcpCapabilities() {
  return request.get('/mcp/file/capabilities', { silentError: true });
}

export function parseFile(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/mcp/file/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function generateFromFile({ file, userId, mode, enableWebSearch, cardTypes, requirement }) {
  const formData = new FormData();
  formData.append('file', file);
  if (userId !== undefined && userId !== null) {
    formData.append('userId', userId);
  }
  formData.append('mode', mode || '智能回答');
  formData.append('enableWebSearch', String(Boolean(enableWebSearch)));
  formData.append('cardTypes', cardTypes || '');
  formData.append('requirement', requirement || '');
  return request.post('/mcp/file/generate', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}
