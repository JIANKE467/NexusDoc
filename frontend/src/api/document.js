import request from './request';

export function generateDocument(data) {
  return request.post('/document/generate', data);
}

export function listDocuments(userId) {
  return request.get('/document/list', { params: { userId } });
}

export function getDocumentDetail(documentId) {
  return request.get(`/document/detail/${documentId}`);
}

export function deleteDocument(documentId) {
  return request.delete(`/document/${documentId}`);
}
