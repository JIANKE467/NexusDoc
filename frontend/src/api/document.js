import request from './request';
import { getOrCreateDeviceId } from '../utils/deviceId';

export function generateDocument(data) {
  return request.post('/document/generate', data);
}

export async function streamGenerateDocument(data, handlers = {}) {
  const response = await fetch('/api/document/generate/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Device-Id': getOrCreateDeviceId(),
      Accept: 'text/event-stream'
    },
    body: JSON.stringify(data)
  });

  if (!response.ok || !response.body) {
    throw new Error('流式生成请求失败');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let finalPayload = null;
  let currentEventName = 'message';

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split(/\r?\n/);
    buffer = lines.pop() || '';

    for (const line of lines) {
      const parsed = parseSseLine(line, currentEventName);
      if (parsed?.eventName) {
        currentEventName = parsed.eventName;
        continue;
      }
      const payload = parsed?.payload;
      if (!payload) {
        continue;
      }
      finalPayload = payload.data || finalPayload;
      dispatchStreamPayload(payload, handlers);
    }
  }

  if (buffer.trim()) {
    const parsed = parseSseLine(buffer, currentEventName);
    if (parsed?.payload) {
      const payload = parsed.payload;
      finalPayload = payload.data || finalPayload;
      dispatchStreamPayload(payload, handlers);
    }
  }

  return finalPayload;
}

function parseSseLine(line, eventName = 'message') {
  const trimmed = line.trim();
  if (!trimmed) {
    return null;
  }
  if (trimmed.startsWith('event:')) {
    return { eventName: trimmed.slice(6).trim() || 'message' };
  }
  if (!trimmed.startsWith('data:')) {
    return null;
  }

  const dataText = trimmed.slice(5).trimStart();
  if (dataText === '[DONE]') {
    return { payload: { eventName: 'done', data: { type: 'done' } } };
  }

  try {
    return { payload: { eventName, data: JSON.parse(dataText) } };
  } catch (error) {
    return { payload: { eventName, data: { type: eventName, content: dataText } } };
  }
}

function dispatchStreamPayload(payload, handlers) {
  const eventType = payload.data?.type || payload.eventName;
  const openAiDelta = payload.data?.choices?.[0]?.delta?.content;
  if (openAiDelta) {
    handlers.onDelta?.(openAiDelta);
    return;
  }
  if (eventType === 'start') {
    handlers.onStart?.(payload.data);
    return;
  }
  if (eventType === 'delta' || eventType === 'card_delta') {
    handlers.onDelta?.(payload.data?.content || '');
    return;
  }
  if (eventType === 'source') {
    handlers.onSource?.(payload.data);
    return;
  }
  if (eventType === 'warning') {
    handlers.onWarning?.(payload.data?.message || '生成过程中出现警告');
    return;
  }
  if (eventType === 'saved') {
    handlers.onSaved?.(payload.data);
    return;
  }
  if (eventType === 'done') {
    handlers.onDone?.(payload.data);
    return;
  }
  if (eventType === 'error') {
    handlers.onError?.(payload.data);
    return;
  }
  handlers.onMessage?.(payload.data);
}

export function listDocuments(userId) {
  return request.get('/document/list', { params: userId == null ? {} : { userId } });
}

export function getDocumentDetail(documentId) {
  return request.get(`/document/detail/${documentId}`);
}

export function deleteDocument(documentId) {
  return request.delete(`/document/${documentId}`);
}

export function exportDeviceData() {
  return request.get('/document/device/export');
}

export function importDeviceData(data) {
  return request.post('/document/device/import', data);
}
