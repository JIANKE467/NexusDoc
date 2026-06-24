import request from './request';

export function askDocument(data) {
  return request.post('/chat/ask', data);
}

export function listChatRecords(documentId) {
  return request.get('/chat/list', { params: { documentId } });
}
