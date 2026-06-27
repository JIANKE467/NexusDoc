import request from './request';
import { createDeviceHeaders } from '../utils/deviceId';

const DEBUG_STREAM = import.meta.env.DEV;

export function generateDocument(data) {
  return request.post('/document/generate', data);
}

export function supportsReadableStream() {
  return typeof window !== 'undefined'
    && typeof window.fetch === 'function'
    && typeof window.ReadableStream !== 'undefined'
    && typeof TextDecoder !== 'undefined';
}

export async function streamGenerateDocument(data, handlers = {}) {
  if (!supportsReadableStream()) {
    throw new Error('当前浏览器不支持流式响应');
  }

  const response = await fetch('/api/document/generate/stream', {
    method: 'POST',
    headers: createDeviceHeaders({
      'Content-Type': 'application/json',
      Accept: 'text/event-stream'
    }),
    body: JSON.stringify(data)
  });

  if (!response.ok || !response.body) {
    throw new Error('流式生成请求失败');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let finalPayload = null;

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }

    logStream('chunk', value?.byteLength || 0);
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() || '';

    for (const eventText of events) {
      const payload = parseSseEvent(eventText);
      if (!payload) continue;
      logStream('event', payload.data?.type || payload.eventName);
      finalPayload = payload.data || finalPayload;
      dispatchStreamPayload(payload, handlers);
    }
  }

  if (buffer.trim()) {
    const payload = parseSseEvent(buffer);
    if (payload) {
      finalPayload = payload.data || finalPayload;
      dispatchStreamPayload(payload, handlers);
    }
  }

  return finalPayload;
}

function parseSseEvent(eventText) {
  if (!eventText?.trim()) {
    return null;
  }
  const lines = eventText.split(/\r?\n/);
  let eventName = 'message';
  const dataLines = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith(':')) continue;
    if (trimmed.startsWith('event:')) {
      eventName = trimmed.slice(6).trim() || 'message';
      continue;
    }
    if (trimmed.startsWith('data:')) {
      dataLines.push(trimmed.slice(5).trimStart());
    }
  }

  const dataText = dataLines.join('\n').trim();
  if (!dataText) {
    return null;
  }
  if (dataText === '[DONE]') {
    return { eventName: 'done', data: { type: 'done' } };
  }

  try {
    return { eventName, data: JSON.parse(dataText) };
  } catch (error) {
    return { eventName, data: { type: eventName, content: dataText } };
  }
}

function dispatchStreamPayload(payload, handlers) {
  const eventType = payload.data?.type || payload.eventName;
  const openAiDelta = payload.data?.choices?.[0]?.delta?.content;
  if (openAiDelta) {
    logStream('delta', openAiDelta.length);
    handlers.onDelta?.(openAiDelta);
    return;
  }
  if (eventType === 'start') {
    handlers.onStart?.(payload.data);
    return;
  }
  if (eventType === 'delta' || eventType === 'card_delta') {
    logStream('delta', (payload.data?.content || '').length);
    handlers.onDelta?.(payload.data?.content || '');
    return;
  }
  if (typeof payload.data?.content === 'string') {
    logStream('delta', payload.data.content.length);
    handlers.onDelta?.(payload.data.content);
    return;
  }
  const openAiMessage = payload.data?.choices?.[0]?.message?.content;
  if (openAiMessage) {
    logStream('delta', openAiMessage.length);
    handlers.onDelta?.(openAiMessage);
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

function logStream(stage, value) {
  if (!DEBUG_STREAM) {
    return;
  }
  console.log(`[NexusDoc Stream] ${stage}`, value);
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
