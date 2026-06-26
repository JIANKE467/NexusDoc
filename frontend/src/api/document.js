import request from './request';

export function generateDocument(data) {
  return request.post('/document/generate', data);
}

export async function streamGenerateDocument(data, handlers = {}) {
  const response = await fetch('/api/document/generate/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
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

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\r?\n\r?\n+/);
    buffer = events.pop() || '';

    for (const eventBlock of events) {
      const payload = parseSseBlock(eventBlock);
      if (!payload) {
        continue;
      }
      finalPayload = payload.data || finalPayload;
      dispatchStreamPayload(payload, handlers);
    }
  }

  if (buffer.trim()) {
    const payload = parseSseBlock(buffer);
    if (payload) {
      finalPayload = payload.data || finalPayload;
      dispatchStreamPayload(payload, handlers);
    }
  }

  return finalPayload;
}

function parseSseBlock(block) {
  let eventName = 'message';
  const dataLines = [];
  block.split(/\n/).forEach((line) => {
    const trimmed = line.trimEnd();
    if (trimmed.startsWith('event:')) {
      eventName = trimmed.slice(6).trim() || 'message';
      return;
    }
    if (trimmed.startsWith('data:')) {
      dataLines.push(trimmed.slice(5).trimStart());
    }
  });

  if (!dataLines.length) {
    return null;
  }

  const dataText = dataLines.join('\n');
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
  return request.get('/document/list', { params: { userId } });
}

export function getDocumentDetail(documentId) {
  return request.get(`/document/detail/${documentId}`);
}

export function deleteDocument(documentId) {
  return request.delete(`/document/${documentId}`);
}
