const TECHNICAL_KEYS = new Set(['id', 'level', 'parentId', 'sourceType']);

export function normalizeAiText(input) {
  if (input == null) {
    return '';
  }
  let text = typeof input === 'string' ? input : JSON.stringify(input, null, 2);
  if (!text || text === 'undefined' || text === 'null' || text === '[object Object]') {
    return '';
  }
  text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').trim();
  text = stripCodeFence(text);
  text = text.replace(/\n{4,}/g, '\n\n\n');
  return text.trim();
}

export function stripCodeFence(text) {
  let value = String(text || '').trim();
  const fence = value.match(/^```(?:json|markdown|md|text|javascript|js)?\s*\n([\s\S]*?)\n```$/i);
  if (fence) {
    value = fence[1].trim();
  }
  const longFence = value.match(/^````(?:json|markdown|md|text)?\s*\n([\s\S]*?)\n````$/i);
  if (longFence) {
    value = longFence[1].trim();
  }
  return value;
}

export function detectContentType(text) {
  const normalized = normalizeAiText(text);
  const json = tryParseJson(normalized);
  if (json.ok) {
    return isMindMapJson(json.value) ? 'mindmap-json' : 'json';
  }
  if (/[#*_>`-]|\n\s*[-*]\s+|\n#{1,6}\s+/.test(normalized)) {
    return 'markdown';
  }
  return 'text';
}

export function tryParseJson(text) {
  const normalized = normalizeAiText(text);
  if (!looksLikeJson(normalized)) {
    return { ok: false, value: null };
  }
  try {
    return { ok: true, value: JSON.parse(normalized) };
  } catch {
    return { ok: false, value: null };
  }
}

export function isMindMapJson(value) {
  if (!value || typeof value !== 'object') {
    return false;
  }
  if (Array.isArray(value)) {
    return value.some((item) => isMindMapNode(item));
  }
  if (Array.isArray(value.nodes) || Array.isArray(value.children)) {
    return true;
  }
  return isMindMapNode(value);
}

export function jsonToReadableOutline(value) {
  if (value == null) {
    return '暂未解析到结构内容。';
  }
  const lines = [];
  if (!Array.isArray(value) && value.title) {
    lines.push(String(value.title));
    lines.push('');
  }
  const roots = Array.isArray(value)
    ? value
    : Array.isArray(value.nodes)
      ? value.nodes
      : Array.isArray(value.children)
        ? value.children
        : [value];

  roots.forEach((node, index) => appendNodeOutline(node, lines, 0, index + 1));
  const result = lines.join('\n').replace(/\n{3,}/g, '\n\n').trim();
  return result || jsonToReadableFields(value);
}

export function markdownToPlainPreview(markdown, maxLength = 220) {
  const text = normalizeAiText(markdown)
    .replace(/```[\s\S]*?```/g, ' 代码片段 ')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/^\s*[-*+]\s+/gm, '• ')
    .replace(/^\s*\d+[.)、]\s+/gm, '• ')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/__(.*?)__/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '$1')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
  return truncateText(text, maxLength);
}

export function markdownToSafeHtml(markdown) {
  const text = normalizeAiText(markdown);
  if (!text) {
    return '<p>暂未生成有效内容，请调整输入后重试。</p>';
  }
  const lines = escapeHtml(text).split('\n');
  const blocks = [];
  let paragraph = [];
  let listItems = [];
  let orderedItems = [];
  let inCode = false;
  let codeLines = [];

  const flushParagraph = () => {
    if (paragraph.length) {
      blocks.push(`<p>${renderInlineMarkdown(paragraph.join(' '))}</p>`);
      paragraph = [];
    }
  };
  const flushList = () => {
    if (listItems.length) {
      blocks.push(`<ul>${listItems.map((item) => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ul>`);
      listItems = [];
    }
    if (orderedItems.length) {
      blocks.push(`<ol>${orderedItems.map((item) => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ol>`);
      orderedItems = [];
    }
  };
  const flushCode = () => {
    if (codeLines.length) {
      blocks.push(`<pre><code>${codeLines.join('\n')}</code></pre>`);
      codeLines = [];
    }
  };

  lines.forEach((line) => {
    const trimmed = line.trim();
    if (/^```/.test(trimmed)) {
      if (inCode) {
        flushCode();
        inCode = false;
      } else {
        flushParagraph();
        flushList();
        inCode = true;
      }
      return;
    }
    if (inCode) {
      codeLines.push(line);
      return;
    }
    if (!trimmed) {
      flushParagraph();
      flushList();
      return;
    }
    const heading = trimmed.match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      flushParagraph();
      flushList();
      const level = Math.min(heading[1].length + 1, 5);
      blocks.push(`<h${level}>${renderInlineMarkdown(heading[2])}</h${level}>`);
      return;
    }
    const bracketHeading = trimmed.match(/^【(.+)】$/);
    if (bracketHeading) {
      flushParagraph();
      flushList();
      blocks.push(`<h3>${renderInlineMarkdown(bracketHeading[1])}</h3>`);
      return;
    }
    const unordered = trimmed.match(/^[-*+]\s+(.+)$/);
    if (unordered) {
      flushParagraph();
      orderedItems = [];
      listItems.push(unordered[1]);
      return;
    }
    const ordered = trimmed.match(/^\d+[.)、]\s+(.+)$/);
    if (ordered) {
      flushParagraph();
      listItems = [];
      orderedItems.push(ordered[1]);
      return;
    }
    const quote = trimmed.match(/^>\s?(.+)$/);
    if (quote) {
      flushParagraph();
      flushList();
      blocks.push(`<blockquote>${renderInlineMarkdown(quote[1])}</blockquote>`);
      return;
    }
    paragraph.push(trimmed);
  });

  flushCode();
  flushParagraph();
  flushList();
  return blocks.join('') || '<p>暂未生成有效内容，请调整输入后重试。</p>';
}

export function buildReadableCards(rawText, options = {}) {
  const text = normalizeAiText(rawText);
  if (!text) {
    return [];
  }
  const docType = options.docType || '智能回答';
  const sources = Array.isArray(options.sources) ? options.sources : [];
  const json = tryParseJson(text);
  if (json.ok) {
    return buildJsonCards(json.value, text, docType, sources);
  }
  return buildTextCards(text, docType, sources);
}

export function formatRawContentForDisplay(card) {
  const raw = normalizeAiText(card?.originalRaw || card?.raw || card?.body || '');
  const json = tryParseJson(raw);
  if (json.ok) {
    return JSON.stringify(json.value, null, 2);
  }
  return raw;
}

export function escapeHtml(text) {
  return String(text)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function buildJsonCards(value, rawText, docType, sources) {
  const outline = isMindMapJson(value) ? jsonToReadableOutline(value) : jsonToReadableFields(value);
  const title = !Array.isArray(value) && value?.title ? String(value.title) : '结构与节点预览';
  const cards = [
    createCard({
      id: 'structure-card',
      label: '结构卡',
      tone: 'structure',
      meta: isMindMapJson(value) ? 'Mind Map JSON' : 'Structured JSON',
      title,
      body: markdownToPlainPreview(outline, 260),
      detail: outline,
      raw: rawText,
      sources,
      originalRaw: rawText
    })
  ];

  const references = extractJsonReferences(value);
  if (references.length || sources.length) {
    cards.push(createCard({
      id: 'citation-card',
      label: '引用卡',
      tone: 'citation',
      meta: `${references.length || sources.length} Sources`,
      title: '参考来源引用',
      body: references.length ? references.map((item) => `[${item.id}] ${item.title}`).join('\n') : '来源已整理为引用信息。',
      detail: references.length ? references.map((item) => `[${item.id}] ${item.title}\n${item.url || ''}`).join('\n\n') : sources.map((item) => `[${item.id}] ${item.title}\n${item.url || ''}`).join('\n\n'),
      raw: rawText,
      sources: sources.length ? sources : references,
      originalRaw: rawText
    }));
  }
  return cards;
}

function buildTextCards(text, docType, sources) {
  const sections = splitIntoSections(text);
  const cards = [];
  const first = sections[0];
  const summaryBody = first?.body || text;

  cards.push(createCard({
    id: 'summary-card',
    label: '摘要卡',
    tone: 'summary',
    meta: detectContentType(text) === 'markdown' ? 'Markdown' : 'Summary',
    title: inferTitle(sections, '生成结果'),
    body: markdownToPlainPreview(summaryBody, 230),
    detail: summaryBody,
    raw: text,
    sources: sources.slice(0, 3),
    originalRaw: text
  }));

  const insightBody = findSectionBody(sections, ['观点', '分析', '风险', '趋势', '判断']);
  if (insightBody || /观点|分析|风险|趋势|判断|建议/.test(text)) {
    cards.push(createCard({
      id: 'insight-card',
      label: '观点卡',
      tone: 'insight',
      meta: 'Insight',
      title: inferSectionTitle(sections, ['观点', '分析', '风险', '趋势', '判断']) || '关键观点与判断',
      body: markdownToPlainPreview(insightBody || pickPreviewLines(text, ['观点', '分析', '风险', '趋势', '建议']), 230),
      detail: insightBody || text,
      raw: text,
      sources: sources.slice(0, 2),
      originalRaw: text
    }));
  }

  if (/任务|行动|待办|负责人|截止|清单/.test(text) || /会议纪要|任务/.test(docType)) {
    const actionBody = findSectionBody(sections, ['行动', '任务', '待办', '清单']);
    cards.push(createCard({
      id: 'action-card',
      label: '任务卡',
      tone: 'action',
      meta: 'Action',
      title: '可执行任务清单',
      body: markdownToPlainPreview(actionBody || pickPreviewLines(text, ['行动', '任务', '待办', '截止', '负责人']), 230),
      detail: actionBody || text,
      raw: text,
      sources: [],
      originalRaw: text
    }));
  }

  if (/思维导图|nodes|children|结构|大纲|提纲|章节/.test(text) || /思维导图/.test(docType)) {
    const structureBody = findSectionBody(sections, ['结构', '提纲', '思维导图', '章节']);
    cards.push(createCard({
      id: 'structure-card',
      label: '结构卡',
      tone: 'structure',
      meta: 'Structure',
      title: '结构与节点预览',
      body: markdownToPlainPreview(structureBody || pickPreviewLines(text, ['结构', '节点', '章节', '层级', '提纲']), 230),
      detail: structureBody || text,
      raw: text,
      sources: sources.slice(0, 2),
      originalRaw: text
    }));
  }

  if (/故事|小说|文案|创作|世界观|角色|剧情/.test(text) || /小说|创作/.test(docType)) {
    cards.push(createCard({
      id: 'generation-card',
      label: '生成卡',
      tone: 'generation',
      meta: 'Generate',
      title: '内容生成结果',
      body: markdownToPlainPreview(text, 260),
      detail: text,
      raw: text,
      sources: sources.slice(0, 2),
      originalRaw: text
    }));
  }

  if (sources.length > 0) {
    cards.push(createCard({
      id: 'citation-card',
      label: '引用卡',
      tone: 'citation',
      meta: `${sources.length} Sources`,
      title: '参考来源引用',
      body: sources.map((source) => `[${source.id}] ${source.title}`).slice(0, 5).join('\n'),
      detail: sources.map((source) => `[${source.id}] ${source.title}\n${source.url}`).join('\n\n'),
      raw: sources.map((source) => `${source.title} ${source.url}`).join('\n'),
      sources,
      originalRaw: text
    }));
  }

  return dedupeCards(cards);
}

function createCard(card) {
  const detailText = normalizeAiText(card.detail || card.body || card.raw);
  return {
    ...card,
    body: markdownToPlainPreview(card.body || detailText, 260),
    detailText,
    previewHtml: markdownToSafeHtml(markdownToPlainPreview(card.body || detailText, 260)),
    detailHtml: markdownToSafeHtml(detailText),
    copyText: detailText,
    points: [],
    raw: detailText,
    originalRaw: card.originalRaw || card.raw || detailText,
    sources: Array.isArray(card.sources) ? card.sources : []
  };
}

function splitIntoSections(text) {
  const normalized = normalizeAiText(text);
  const bracketMatches = [...normalized.matchAll(/【([^】]+)】([\s\S]*?)(?=【[^】]+】|$)/g)];
  if (bracketMatches.length > 0) {
    return bracketMatches.map((match) => ({
      title: match[1].trim(),
      body: match[2].trim(),
      raw: match[0].trim()
    })).filter((section) => section.body);
  }
  const headingMatches = [...normalized.matchAll(/^#{1,3}\s+(.+)\n([\s\S]*?)(?=^#{1,3}\s+|\s*$)/gm)];
  if (headingMatches.length > 0) {
    return headingMatches.map((match) => ({
      title: match[1].trim(),
      body: match[2].trim(),
      raw: match[0].trim()
    })).filter((section) => section.body);
  }
  return [{ title: '生成结果', body: normalized, raw: normalized }];
}

function inferTitle(sections, fallback) {
  return sections[0]?.title || fallback;
}

function inferSectionTitle(sections, keywords) {
  return sections.find((section) => keywords.some((keyword) => section.title.includes(keyword)))?.title;
}

function findSectionBody(sections, keywords) {
  return sections.find((section) => keywords.some((keyword) => section.title.includes(keyword)))?.body;
}

function pickPreviewLines(text, keywords) {
  const lines = normalizeAiText(text)
    .split(/\n+/)
    .map((line) => markdownToPlainPreview(line, 120))
    .filter(Boolean);
  const matched = lines.filter((line) => keywords.some((keyword) => line.includes(keyword)));
  return (matched.length ? matched : lines).slice(0, 5).join('\n');
}

function appendNodeOutline(node, lines, depth, index) {
  if (depth > 3 || node == null) {
    return;
  }
  if (typeof node !== 'object') {
    lines.push(`${'  '.repeat(depth)}- ${String(node)}`);
    return;
  }
  const label = node.label || node.title || node.name || node.text || `节点 ${index}`;
  const prefix = depth === 0 ? `${index}. ` : '- ';
  lines.push(`${'  '.repeat(depth)}${prefix}${String(label)}`);
  const detailLines = Object.entries(node)
    .filter(([key, value]) => !TECHNICAL_KEYS.has(key) && !['label', 'title', 'name', 'text', 'children', 'nodes', 'references'].includes(key) && primitive(value))
    .map(([, value]) => String(value).trim())
    .filter(Boolean);
  detailLines.slice(0, 3).forEach((detail) => {
    lines.push(`${'  '.repeat(depth + 1)}- ${detail}`);
  });
  const children = Array.isArray(node.children) ? node.children : Array.isArray(node.nodes) ? node.nodes : [];
  children.slice(0, 8).forEach((child, childIndex) => appendNodeOutline(child, lines, depth + 1, childIndex + 1));
}

function jsonToReadableFields(value) {
  if (Array.isArray(value)) {
    return value.slice(0, 12).map((item, index) => `${index + 1}. ${jsonToReadableFields(item)}`).join('\n');
  }
  if (!value || typeof value !== 'object') {
    return String(value ?? '');
  }
  return Object.entries(value)
    .filter(([key, val]) => !TECHNICAL_KEYS.has(key) && val != null && val !== '')
    .slice(0, 16)
    .map(([key, val]) => {
      if (Array.isArray(val)) {
        return `${key}：\n${val.slice(0, 6).map((item) => `- ${jsonToReadableFields(item)}`).join('\n')}`;
      }
      if (typeof val === 'object') {
        return `${key}：${jsonToReadableFields(val)}`;
      }
      return `${key}：${val}`;
    })
    .join('\n');
}

function extractJsonReferences(value) {
  if (!value || typeof value !== 'object' || !Array.isArray(value.references)) {
    return [];
  }
  return value.references.map((item, index) => {
    if (typeof item === 'string') {
      return { id: index + 1, title: item, url: '', site: 'reference', snippet: item };
    }
    const url = item.url || item.link || '';
    return {
      id: index + 1,
      title: item.title || item.name || url || '参考来源',
      url,
      site: url ? safeHost(url) : 'reference',
      snippet: item.snippet || item.description || item.title || ''
    };
  }).slice(0, 5);
}

function looksLikeJson(text) {
  const value = String(text || '').trim();
  return (value.startsWith('{') && value.endsWith('}')) || (value.startsWith('[') && value.endsWith(']'));
}

function isMindMapNode(value) {
  return Boolean(value && typeof value === 'object' && (value.label || value.title) && ('children' in value || 'level' in value || 'parentId' in value));
}

function primitive(value) {
  return ['string', 'number', 'boolean'].includes(typeof value);
}

function truncateText(text, maxLength) {
  const value = String(text || '').trim();
  if (value.length <= maxLength) {
    return value;
  }
  return `${value.slice(0, maxLength).trim()}...`;
}

function renderInlineMarkdown(value) {
  const links = [];
  const withMarkdownLinks = String(value || '').replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, (_match, label, url) => {
    const safeUrl = escapeHtml(url);
    const token = `@@NEXUSDOC_LINK_${links.length}@@`;
    links.push(`<a href="${safeUrl}" target="_blank" rel="noreferrer">${label}</a>`);
    return token;
  });
  const withAutoLinks = withMarkdownLinks.replace(/(^|[\s>])((https?:\/\/[^\s<]+))/g, (_match, prefix, url) => {
    const safeUrl = escapeHtml(url);
    const token = `@@NEXUSDOC_LINK_${links.length}@@`;
    links.push(`<a href="${safeUrl}" target="_blank" rel="noreferrer">${safeUrl}</a>`);
    return `${prefix}${token}`;
  });
  return withAutoLinks
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/@@NEXUSDOC_LINK_(\d+)@@/g, (_match, index) => links[Number(index)] || '');
}

function safeHost(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '');
  } catch {
    return 'source';
  }
}

function dedupeCards(cards) {
  const seen = new Set();
  return cards.filter((card) => {
    if (seen.has(card.id)) {
      return false;
    }
    seen.add(card.id);
    return true;
  });
}
