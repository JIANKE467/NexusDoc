<template>
  <section class="nexus-chat-shell" :style="{ '--scroll-progress': scrollProgress.toFixed(3) }">
    <div class="aurora aurora-one"></div>
    <div class="aurora aurora-two"></div>
    <div class="aurora aurora-three"></div>
    <div class="depth-light"></div>
    <div class="ocean-current"></div>
    <div class="grid-overlay"></div>
    <div class="floating-artifacts" aria-hidden="true">
      <span class="artifact artifact-doc">DOC</span>
      <span class="artifact artifact-node"></span>
      <span class="artifact artifact-map">JSON</span>
      <span class="artifact artifact-ai">AI</span>
      <span class="artifact artifact-line"></span>
    </div>

    <aside :class="['chat-sidebar', { 'is-open': sidebarOpen }]">
      <div class="sidebar-brand">
        <div class="brand-mark">文</div>
        <div>
          <strong>NexusDoc</strong>
          <span>Knowledge OS</span>
        </div>
      </div>

      <button class="new-chat" type="button" @click="createSession">
        <span>+</span>
        新建对话
      </button>

      <div class="sidebar-section">
        <p>最近会话</p>
        <div class="session-scroll-list">
          <div v-if="sortedSessions.length === 0" class="session-empty">
            暂无会话，点击“新建对话”开始处理文档。
          </div>
          <div
            v-for="session in sortedSessions"
            :key="session.id"
            :class="['session-item', { active: session.id === activeSessionId }]"
            @click="switchSession(session.id)"
          >
            <div class="session-head">
              <span class="session-title">{{ session.title }}</span>
              <el-dropdown
                trigger="click"
                popper-class="nexus-dropdown"
                @command="(command) => handleSessionCommand(command, session)"
              >
                <button class="session-more" type="button" @click.stop>⋯</button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="session.pinned ? 'unpin' : 'pin'">
                      {{ session.pinned ? '取消置顶' : '置顶' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="move">移动到档案夹</el-dropdown-item>
                    <el-dropdown-item class="danger" command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="session-foot">
              <span v-if="session.isDraft" class="draft-badge">Draft</span>
              <span v-else-if="session.pinned" class="pin-badge">Pinned</span>
              <span class="session-meta">{{ session.updatedAt }}</span>
              <span class="meta-dot">·</span>
              <span class="folder-chip">{{ session.isDraft ? '未发送' : (session.folderName || DEFAULT_FOLDER) }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <main class="chat-main">
      <header class="chat-topbar">
        <button class="sidebar-toggle" type="button" @click="sidebarOpen = !sidebarOpen">☰</button>
        <div>
          <p class="eyebrow">NexusDoc / 文枢</p>
          <h1>让每一份文档，都成为可对话的知识。</h1>
        </div>
        <div v-if="aiConfig && !aiConfig.apiKeyConfigured" class="topbar-actions">
          <span v-if="aiConfig && !aiConfig.apiKeyConfigured" class="config-warning">
            后端未配置 AI Key
          </span>
        </div>
      </header>

      <section ref="messageViewport" class="message-viewport">
        <div v-if="activeNav === 'folders'" class="folder-view">
          <div class="folder-hero">
            <span class="status-pill">Knowledge Archive</span>
            <h2>档案夹</h2>
            <p>把重要会话和文档按主题收纳，让处理过的知识继续可查、可追问、可复用。</p>
          </div>

          <div class="folder-grid">
            <button
              v-for="folder in folderStats"
              :key="folder.name"
              :class="['folder-card', { active: folder.name === selectedFolder }]"
              type="button"
              @click="selectedFolder = folder.name"
            >
              <span>{{ folder.name }}</span>
              <strong>{{ folder.count }} 个文档</strong>
              <small>最近更新 {{ folder.lastUpdateTime || '暂无' }}</small>
            </button>
          </div>

          <div class="folder-docs">
            <div class="folder-docs-head">
              <h3>{{ selectedFolder }}</h3>
              <span>{{ folderSessions.length }} 个会话</span>
            </div>
            <div v-if="folderSessions.length === 0" class="folder-empty">该档案夹暂无文档。</div>
            <button
              v-for="session in folderSessions"
              :key="session.id"
              class="folder-doc-item"
              type="button"
              @click="openFolderSession(session.id)"
            >
              <span>{{ session.title }}</span>
              <small>{{ session.updatedAt }}</small>
            </button>
          </div>
        </div>

        <div v-else-if="activeMessages.length === 0" class="welcome-panel">
          <section class="hero-stage" aria-labelledby="home-hero-title">
            <div class="hero-copy">
              <span class="status-pill">AI Document Intelligence</span>
              <p class="hero-kicker">AI Document Ocean / 文档知识深海</p>
              <h2 id="home-hero-title">
                让每一份文档，<br />
                都成为可对话的知识。
              </h2>
              <p class="hero-subtitle">
                上传、总结、提问、搜索、生成思维导图，让复杂文档变成可以持续追问的知识工作台。
              </p>
              <div class="hero-actions">
                <button class="primary-cta" type="button" @click="startProcessing">
                  开始处理文档
                </button>
                <button class="secondary-cta" type="button" @click="$router.push('/history')">
                  查看历史记录
                </button>
              </div>
            </div>

            <div class="ocean-stage" aria-hidden="true">
              <div class="knowledge-core">
                <span>文枢</span>
                <i></i>
              </div>
              <div class="stage-card stage-card-main">
                <small>Document Stream</small>
                <strong>会议纪要 / 课程资料 / 项目需求</strong>
                <span>Structured into searchable knowledge</span>
              </div>
              <div class="stage-card stage-card-left">
                <small>Mind Map</small>
                <strong>节点结构</strong>
              </div>
              <div class="stage-card stage-card-right">
                <small>Web Search</small>
                <strong>参考来源</strong>
              </div>
              <div class="stage-bubble bubble-one"></div>
              <div class="stage-bubble bubble-two"></div>
            </div>
          </section>

          <div class="section-heading">
            <span>Document Tools</span>
            <h3>今天想处理什么文档？</h3>
            <p>从总结、提取、改写，到联网补充与结构生成，把文档沉入知识深海，再浮出清晰答案。</p>
          </div>

          <div class="prompt-grid">
            <button
              v-for="(prompt, index) in promptCards"
              :key="prompt.title"
              :ref="(el) => setFeatureCardRef(el, index)"
              :class="['prompt-card', 'feature-card', { 'is-visible': visibleCards[index] }]"
              type="button"
              @click="applyPrompt(prompt)"
            >
              <span>{{ prompt.kicker }}</span>
              <strong>{{ prompt.title }}</strong>
              <small>{{ prompt.description }}</small>
            </button>
          </div>

          <section class="mindmap-preview" aria-label="思维导图预览">
            <div class="preview-copy">
              <span class="status-pill">Knowledge Graph Preview</span>
              <h3>把长文档拆成可探索的知识节点。</h3>
              <p>
                NexusDoc 可以把摘要、风险、趋势和参考来源整理成结构化节点，为后续思维导图渲染预留清晰数据。
              </p>
            </div>
            <div class="node-stage" aria-hidden="true">
              <span class="map-node map-center">中心主题</span>
              <span class="map-node map-a">原文信息</span>
              <span class="map-node map-b">网络补充</span>
              <span class="map-node map-c">合理推断</span>
              <span class="map-node map-d">行动清单</span>
            </div>
          </section>
        </div>

        <div v-else class="message-list">
          <article
            v-for="message in activeMessages"
            :key="message.id"
            :class="['message-row', message.role]"
          >
            <div class="avatar">{{ message.role === 'assistant' ? 'AI' : '我' }}</div>
            <div class="message-bubble">
              <div v-if="message.loading" class="thinking">
                <span>AI 正在思考</span>
                <i></i>
                <i></i>
                <i></i>
              </div>
              <div v-else class="message-content">{{ message.content }}</div>
            </div>
          </article>
        </div>
      </section>

      <footer v-if="activeNav !== 'folders'" class="composer-wrap">
        <div class="composer">
          <textarea
            v-model="inputText"
            rows="1"
            placeholder="粘贴文档、输入需求，或直接说：帮我总结这份材料..."
            @input="resizeComposer"
            @keydown="handleComposerKeydown"
          ></textarea>
          <div class="composer-tools">
            <div class="composer-left-tools">
              <select v-model="selectedDocType" aria-label="文档类型">
                <option v-for="item in docTypes" :key="item" :value="item">{{ item }}</option>
              </select>
              <label class="web-search-toggle" title="开启后，系统会先搜索网络资料，再结合搜索结果回答。">
                <input v-model="enableWebSearch" type="checkbox" />
                <span>联网搜索</span>
              </label>
            </div>
            <button class="send-button" type="button" :disabled="sending || !inputText.trim()" @click="sendMessage">
              <span v-if="sending">生成中</span>
              <span v-else>发送</span>
            </button>
          </div>
        </div>
      </footer>
    </main>

    <el-dialog v-model="moveDialogVisible" title="移动到档案夹" width="460px" class="nexus-dialog">
      <div class="move-dialog-body">
        <p>当前文档：{{ movingSession?.title || '未选择' }}</p>
        <label>
          <span>目标档案夹</span>
          <select v-model="targetFolder">
            <option v-for="folder in folderOptions" :key="folder" :value="folder">{{ folder }}</option>
          </select>
        </label>
      </div>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="moveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmMoveSession">确认移动</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAiConfig } from '../api/ai';
import { generateDocument } from '../api/document';
import { ANONYMOUS_USER_ID } from '../config/user';

const SESSION_STORAGE_KEY = 'nexusdoc-chat-sessions';
const DEFAULT_FOLDER = '默认档案夹';
const folderOptions = ['默认档案夹', '学习资料', '项目文档', '小说设定', '政策合同'];

const docTypes = [
  '智能回答',
  '通用总结',
  '会议纪要',
  '提取重点',
  '正式改写',
  '思维导图',
  '小说设定',
  '趋势分析'
];
const promptCards = [
  {
    kicker: 'Summarize',
    title: '总结这份文档',
    description: '提炼摘要、背景、结论和可追踪事项',
    docType: '通用总结',
    text: '请帮我总结这份文档，输出摘要、核心结论、关键事实和后续行动建议：\n\n'
  },
  {
    kicker: 'Extract',
    title: '提取重点',
    description: '按主题整理关键段落与重要信息',
    docType: '提取重点',
    text: '请从下面内容中提取重点，并按主题分组整理：\n\n'
  },
  {
    kicker: 'Meeting',
    title: '生成会议纪要',
    description: '自动整理议题、决策、负责人和截止时间',
    docType: '会议纪要',
    text: '请把下面内容整理成正式会议纪要，包含会议主题、核心讨论、决议事项、负责人和截止时间：\n\n'
  },
  {
    kicker: 'Rewrite',
    title: '改写为正式公文',
    description: '提升表达规范度与结构完整度',
    docType: '正式改写',
    text: '请把下面内容改写为正式公文风格，语言规范、结构清晰、表达克制：\n\n'
  },
  {
    kicker: 'Mind Map',
    title: '生成思维导图',
    description: '输出可渲染的节点 JSON，构成文本框式思维导图。',
    docType: '思维导图',
    text: '请根据下面内容生成思维导图 JSON 数据：\n\n'
  },
  {
    kicker: 'Web Search',
    title: '联网搜索增强',
    description: '补充原文之外的背景资料，生成带参考来源的回答。',
    docType: '趋势与隐藏问题分析',
    enableWebSearch: true,
    text: '请开启联网搜索，结合网络资料分析下面内容并列出参考来源：\n\n'
  },
  {
    kicker: 'Novel Forge',
    title: '小说设定生成',
    description: '构建世界观、人物关系、核心冲突与章节规划。',
    docType: '小说设定',
    text: '请基于下面设想生成小说设定集，包含世界观、角色、冲突和章节规划：\n\n'
  },
  {
    kicker: 'Insight',
    title: '趋势与隐藏问题分析',
    description: '发现文档背后的风险、趋势和需要追问的问题。',
    docType: '趋势分析',
    text: '请分析下面内容中的隐藏问题、潜在风险和后续趋势：\n\n'
  }
];

const sessions = ref([]);
const activeSessionId = ref('');
const inputText = ref('');
const selectedDocType = ref('智能回答');
const enableWebSearch = ref(false);
const sending = ref(false);
const sidebarOpen = ref(false);
const messageViewport = ref(null);
const aiConfig = ref(null);
const activeNav = ref('home');
const selectedFolder = ref(DEFAULT_FOLDER);
const moveDialogVisible = ref(false);
const movingSession = ref(null);
const targetFolder = ref(DEFAULT_FOLDER);
const scrollProgress = ref(0);
const featureCards = ref([]);
const visibleCards = ref([]);
const route = useRoute();
const router = useRouter();

let scrollFrame = 0;
let featureObserver = null;

const activeSession = computed(() => sessions.value.find((session) => session.id === activeSessionId.value));
const activeMessages = computed(() => activeSession.value?.messages || []);
const sortedSessions = computed(() => {
  return [...sessions.value].sort((first, second) => {
    if (Boolean(first.pinned) !== Boolean(second.pinned)) {
      return first.pinned ? -1 : 1;
    }
    if (Boolean(first.isDraft) !== Boolean(second.isDraft)) {
      return first.isDraft ? -1 : 1;
    }
    return getSessionTime(second) - getSessionTime(first);
  });
});
const folderStats = computed(() => {
  return folderOptions.map((name) => {
    const items = sessions.value.filter((session) => (session.folderName || DEFAULT_FOLDER) === name);
    const latest = items.reduce((max, session) => Math.max(max, getSessionTime(session)), 0);
    return {
      name,
      count: items.length,
      lastUpdateTime: latest ? formatSessionTime(new Date(latest)) : ''
    };
  });
});
const folderSessions = computed(() => {
  return sortedSessions.value.filter((session) => (session.folderName || DEFAULT_FOLDER) === selectedFolder.value);
});

onMounted(async () => {
  restoreSessions();
  syncActiveNavFromRoute(route.query.view);
  await loadAiConfig();
  await nextTick();
  initMotionEffects();
  if (activeMessages.value.length > 0) {
    scrollToBottom();
  } else {
    scrollToTop();
  }
});

onUnmounted(() => {
  teardownMotionEffects();
});

watch(
  () => route.query.view,
  (view) => {
    syncActiveNavFromRoute(view);
  }
);

async function loadAiConfig() {
  aiConfig.value = await getAiConfig();
}

function restoreSessions() {
  const cached = localStorage.getItem(SESSION_STORAGE_KEY);
  sessions.value = sanitizeSessions(cached ? JSON.parse(cached).map(normalizeSession) : []);
  let homeSession = sessions.value.find(isBlankDraftSession);
  if (!homeSession) {
    homeSession = createSessionData('新文档对话');
    sessions.value.unshift(homeSession);
  }
  activeSessionId.value = homeSession.id;
  persistSessions();
}

function persistSessions() {
  sessions.value = sanitizeSessions(sessions.value);
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sessions.value));
}

function createSessionData(title) {
  const now = Date.now();
  return {
    id: `draft-${now}-${Math.random().toString(16).slice(2)}`,
    title,
    updatedAt: formatSessionTime(),
    updatedAtValue: now,
    createdAtValue: now,
    isDraft: true,
    pinned: false,
    pinnedTime: null,
    folderName: DEFAULT_FOLDER,
    messages: []
  };
}

function createSession() {
  const existingDraft = sessions.value.find(isBlankDraftSession);
  const session = existingDraft || createSessionData('新文档对话');
  moveSessionToTop(session.id);
  if (!existingDraft) {
    sessions.value.unshift(session);
  }
  activeSessionId.value = session.id;
  inputText.value = '';
  sidebarOpen.value = false;
  activeNav.value = 'home';
  replaceWorkspaceView('home');
  persistSessions();
  nextTick(focusComposerInput);
}

function switchSession(sessionId) {
  activeSessionId.value = sessionId;
  sidebarOpen.value = false;
  activeNav.value = 'chat';
  replaceWorkspaceView('chat');
  nextTick(scrollToBottom);
}

function applyPrompt(prompt) {
  inputText.value = prompt.text;
  selectedDocType.value = prompt.docType || '通用总结';
  if (prompt.enableWebSearch) {
    enableWebSearch.value = true;
  }
  nextTick(() => {
    const textarea = document.querySelector('.composer textarea');
    textarea?.focus();
    resizeComposer({ target: textarea });
  });
}

function startProcessing() {
  activeNav.value = 'home';
  nextTick(() => {
    const textarea = document.querySelector('.composer textarea');
    textarea?.focus();
  });
}

function setFeatureCardRef(el, index) {
  if (el) {
    featureCards.value[index] = el;
  }
}

function initMotionEffects() {
  visibleCards.value = promptCards.map(() => false);
  const viewport = messageViewport.value;
  if (!viewport) {
    return;
  }
  viewport.addEventListener('scroll', requestScrollProgress, { passive: true });
  requestScrollProgress();

  if ('IntersectionObserver' in window) {
    featureObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const index = Number(entry.target.dataset.featureIndex);
          if (entry.isIntersecting && Number.isInteger(index)) {
            visibleCards.value[index] = true;
            featureObserver?.unobserve(entry.target);
          }
        });
      },
      {
        root: viewport,
        threshold: 0.2
      }
    );
    featureCards.value.forEach((card, index) => {
      if (card) {
        card.dataset.featureIndex = String(index);
        featureObserver.observe(card);
      }
    });
  } else {
    visibleCards.value = promptCards.map(() => true);
  }
}

function teardownMotionEffects() {
  const viewport = messageViewport.value;
  if (viewport) {
    viewport.removeEventListener('scroll', requestScrollProgress);
  }
  if (scrollFrame) {
    cancelAnimationFrame(scrollFrame);
    scrollFrame = 0;
  }
  featureObserver?.disconnect();
  featureObserver = null;
}

function requestScrollProgress() {
  if (scrollFrame) {
    return;
  }
  scrollFrame = requestAnimationFrame(() => {
    const viewport = messageViewport.value;
    if (!viewport) {
      scrollProgress.value = 0;
    } else {
      const maxScroll = Math.max(viewport.scrollHeight - viewport.clientHeight, 1);
      scrollProgress.value = Math.min(viewport.scrollTop / maxScroll, 1);
    }
    scrollFrame = 0;
  });
}

async function sendMessage() {
  const content = inputText.value.trim();
  if (!content || sending.value) {
    return;
  }
  if (aiConfig.value && !aiConfig.value.apiKeyConfigured) {
    ElMessage.warning('请在后端运行环境设置 SILICONFLOW_API_KEY 后重启服务');
    return;
  }

  let session = activeSession.value;
  if (!session) {
    session = createSessionData('新文档对话');
    sessions.value.unshift(session);
    activeSessionId.value = session.id;
  }
  activeNav.value = 'chat';
  replaceWorkspaceView('chat');
  const userMessage = createMessage('user', content);
  const assistantMessage = createMessage('assistant', '', true);
  if (session.isDraft) {
    session.isDraft = false;
  }
  session.messages.push(userMessage, assistantMessage);
  session.title = buildSessionTitle(content);
  session.updatedAt = formatSessionTime();
  session.updatedAtValue = Date.now();
  inputText.value = '';
  sending.value = true;
  persistSessions();
  await nextTick();
  resetComposerHeight();
  scrollToBottom();

  try {
    // 真实 AI 调用入口：当前复用后端已有文档生成接口，后续如需改成流式聊天接口，可在这里替换为新的 API 调用。
    const result = await generateDocument({
      userId: ANONYMOUS_USER_ID,
      title: session.title,
      docType: selectedDocType.value,
      tag: 'AI 对话',
      content,
      enableWebSearch: enableWebSearch.value
    });
    await revealAssistantMessage(assistantMessage, result.resultText || 'AI 暂未返回内容。');
  } catch (error) {
    assistantMessage.loading = false;
    assistantMessage.content = error.message || 'AI 服务暂时不可用，请稍后重试。';
  } finally {
    sending.value = false;
    persistSessions();
    await nextTick();
    scrollToBottom();
  }
}

function createMessage(role, content, loading = false) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    loading
  };
}

async function revealAssistantMessage(message, fullText) {
  message.loading = false;
  message.content = '';
  const chunkSize = fullText.length > 800 ? 8 : 3;
  for (let index = 0; index < fullText.length; index += chunkSize) {
    message.content += fullText.slice(index, index + chunkSize);
    await wait(14);
    scrollToBottom();
  }
}

function handleComposerKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    sendMessage();
  }
}

function resizeComposer(event) {
  const textarea = event?.target;
  if (!textarea) {
    return;
  }
  textarea.style.height = 'auto';
  textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
}

function resetComposerHeight() {
  const textarea = document.querySelector('.composer textarea');
  if (textarea) {
    textarea.style.height = 'auto';
  }
}

function scrollToBottom() {
  const viewport = messageViewport.value;
  if (viewport) {
    viewport.scrollTop = viewport.scrollHeight;
  }
}

function scrollToTop() {
  const viewport = messageViewport.value;
  if (viewport) {
    viewport.scrollTop = 0;
  }
}

function buildSessionTitle(content) {
  const text = content.trim().replace(/\s+/g, ' ');
  if (!text) {
    return '新文档对话';
  }
  return text.length > 18 ? `${text.slice(0, 18)}...` : text;
}

function formatSessionTime(date = new Date()) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date);
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function normalizeSession(session) {
  const messages = Array.isArray(session.messages) ? session.messages : [];
  const isDraft = session.isDraft === true || (messages.length === 0 && session.title === '新文档对话');
  return {
    ...session,
    updatedAt: session.updatedAt || formatSessionTime(),
    updatedAtValue: session.updatedAtValue || Date.now(),
    createdAtValue: session.createdAtValue || session.updatedAtValue || Date.now(),
    isDraft,
    pinned: Boolean(session.pinned),
    pinnedTime: session.pinnedTime || null,
    folderName: session.folderName || DEFAULT_FOLDER,
    messages
  };
}

function sanitizeSessions(items) {
  const cleaned = [];
  let draftKept = false;
  for (const session of items) {
    if (isBlankDraftSession(session)) {
      if (draftKept) {
        continue;
      }
      draftKept = true;
    }
    cleaned.push(session);
  }
  return cleaned;
}

function isBlankDraftSession(session) {
  return Boolean(session?.isDraft)
    && session.title === '新文档对话'
    && (!session.messages || session.messages.length === 0);
}

function moveSessionToTop(sessionId) {
  const index = sessions.value.findIndex((session) => session.id === sessionId);
  if (index <= 0) {
    return;
  }
  const [session] = sessions.value.splice(index, 1);
  sessions.value.unshift(session);
}

function focusComposerInput() {
  const textarea = document.querySelector('.composer textarea');
  textarea?.focus();
}

function getSessionTime(session) {
  return Number(session.updatedAtValue || session.pinnedTime || 0);
}

function goWorkspaceHome() {
  activateWorkspaceHome();
  replaceWorkspaceView('home');
}

function activateWorkspaceHome() {
  const emptySession = sortedSessions.value.find(isBlankDraftSession);
  if (emptySession) {
    moveSessionToTop(emptySession.id);
    activeSessionId.value = emptySession.id;
  } else {
    const session = createSessionData('新文档对话');
    sessions.value.unshift(session);
    activeSessionId.value = session.id;
    persistSessions();
  }
  inputText.value = '';
  activeNav.value = 'home';
  nextTick(scrollToTop);
}

function goAiChat() {
  activeNav.value = 'chat';
  replaceWorkspaceView('chat');
  nextTick(scrollToBottom);
}

function openFolderView() {
  activeNav.value = 'folders';
  sidebarOpen.value = false;
  replaceWorkspaceView('folders');
}

function openFolderSession(sessionId) {
  activeSessionId.value = sessionId;
  activeNav.value = 'chat';
  replaceWorkspaceView('chat');
  nextTick(scrollToBottom);
}

function syncActiveNavFromRoute(view) {
  if (view === 'folders') {
    activeNav.value = 'folders';
    sidebarOpen.value = false;
    nextTick(scrollToTop);
    return;
  }
  if (view === 'chat') {
    activeNav.value = 'chat';
    nextTick(scrollToBottom);
    return;
  }
  activateWorkspaceHome();
}

function replaceWorkspaceView(view) {
  if (route.path !== '/') {
    return;
  }
  const query = view === 'home' ? {} : { view };
  router.replace({ path: '/', query });
}

function handleSessionCommand(command, session) {
  if (command === 'pin' || command === 'unpin') {
    togglePinSession(session);
    return;
  }
  if (command === 'move') {
    openMoveDialog(session);
    return;
  }
  if (command === 'delete') {
    confirmDeleteSession(session);
  }
}

function togglePinSession(session) {
  session.pinned = !session.pinned;
  session.pinnedTime = session.pinned ? Date.now() : null;
  session.updatedAt = formatSessionTime();
  session.updatedAtValue = Date.now();
  persistSessions();
  ElMessage.success(session.pinned ? '已置顶' : '已取消置顶');
}

function openMoveDialog(session) {
  movingSession.value = session;
  targetFolder.value = session.folderName || DEFAULT_FOLDER;
  moveDialogVisible.value = true;
}

function confirmMoveSession() {
  if (!movingSession.value) {
    return;
  }
  movingSession.value.folderName = targetFolder.value;
  movingSession.value.updatedAt = formatSessionTime();
  movingSession.value.updatedAtValue = Date.now();
  selectedFolder.value = targetFolder.value;
  moveDialogVisible.value = false;
  persistSessions();
  ElMessage.success(`已移动到「${targetFolder.value}」`);
}

async function confirmDeleteSession(session) {
  try {
    await ElMessageBox.confirm('删除后将不再出现在最近会话中。', '确认删除该会话吗？', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    sessions.value = sessions.value.filter((item) => item.id !== session.id);
    if (activeSessionId.value === session.id) {
      activeSessionId.value = sortedSessions.value[0]?.id || '';
      activeNav.value = activeSessionId.value ? 'chat' : 'home';
    }
    if (sessions.value.length === 0) {
      const newSession = createSessionData('新文档对话');
      sessions.value.push(newSession);
      activeSessionId.value = newSession.id;
      activeNav.value = 'home';
    }
    persistSessions();
    ElMessage.success('已删除会话');
  } catch (error) {
    if (error !== 'cancel') {
      throw error;
    }
  }
}
</script>

<style scoped>
.nexus-chat-shell {
  position: relative;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: #e5eefc;
  background:
    radial-gradient(circle at 20% 12%, rgba(70, 115, 255, 0.28), transparent 30%),
    radial-gradient(circle at 80% 8%, rgba(45, 212, 191, 0.2), transparent 28%),
    linear-gradient(135deg, #070b18 0%, #0d1326 48%, #111827 100%);
}

.aurora {
  position: absolute;
  border-radius: 999px;
  filter: blur(38px);
  opacity: 0.64;
  pointer-events: none;
  animation: floatGlow 9s ease-in-out infinite;
}

.aurora-one {
  width: 360px;
  height: 360px;
  left: 18%;
  top: -160px;
  background: rgba(78, 118, 255, 0.38);
}

.aurora-two {
  width: 320px;
  height: 320px;
  right: 10%;
  bottom: -150px;
  background: rgba(34, 211, 238, 0.22);
  animation-delay: -3s;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.72), transparent 78%);
  transform: translate3d(0, calc(var(--scroll-progress) * -34px), 0);
  transition: transform 120ms linear;
}

.aurora-three {
  width: 420px;
  height: 420px;
  left: 48%;
  bottom: 12%;
  background: rgba(124, 58, 237, 0.18);
  animation-delay: -5s;
}

.depth-light,
.ocean-current {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.depth-light {
  background:
    radial-gradient(circle at 52% calc(18% + var(--scroll-progress) * 34%), rgba(240, 171, 252, 0.16), transparent 22%),
    radial-gradient(circle at 68% 46%, rgba(34, 211, 238, 0.14), transparent 28%);
  mix-blend-mode: screen;
}

.ocean-current {
  opacity: 0.54;
  background:
    linear-gradient(105deg, transparent 0 18%, rgba(34, 211, 238, 0.08) 28%, transparent 42% 100%),
    linear-gradient(165deg, transparent 0 34%, rgba(124, 58, 237, 0.1) 48%, transparent 64% 100%);
  filter: blur(1px);
  transform: translate3d(calc(var(--scroll-progress) * -32px), calc(var(--scroll-progress) * 42px), 0);
}

.floating-artifacts {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.artifact {
  position: absolute;
  display: grid;
  place-items: center;
  border: 1px solid rgba(125, 211, 252, 0.24);
  color: rgba(226, 246, 255, 0.78);
  background: rgba(15, 23, 42, 0.28);
  box-shadow: 0 0 34px rgba(34, 211, 238, 0.12);
  backdrop-filter: blur(16px);
  animation: driftArtifact 13s ease-in-out infinite;
}

.artifact-doc {
  left: 39%;
  top: 14%;
  width: 74px;
  height: 96px;
  border-radius: 18px;
  font-size: 12px;
  transform: rotate(-9deg);
}

.artifact-map {
  right: 9%;
  top: 36%;
  width: 92px;
  height: 46px;
  border-radius: 999px;
  font-size: 12px;
  animation-delay: -4s;
}

.artifact-ai {
  right: 25%;
  bottom: 18%;
  width: 58px;
  height: 58px;
  border-radius: 20px;
  color: #a5f3fc;
  animation-delay: -7s;
}

.artifact-node {
  left: 16%;
  bottom: 26%;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #22d3ee;
  filter: blur(0.5px);
  animation-delay: -2s;
}

.artifact-line {
  left: 58%;
  top: 7%;
  width: 160px;
  height: 1px;
  border: 0;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.46), transparent);
  box-shadow: 0 0 22px rgba(34, 211, 238, 0.28);
  animation-delay: -9s;
}

.chat-sidebar,
.chat-main {
  position: relative;
  z-index: 1;
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 22px 18px;
  border-right: 1px solid rgba(148, 163, 184, 0.12);
  background:
    linear-gradient(180deg, rgba(8, 13, 28, 0.86), rgba(7, 11, 24, 0.76)),
    rgba(6, 12, 27, 0.72);
  backdrop-filter: blur(24px);
  animation: enterFromLeft 560ms ease both;
}

.sidebar-brand {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 38px;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 10px;
  color: rgba(248, 250, 252, 0.94);
  font-size: 15px;
  font-weight: 700;
  background:
    linear-gradient(135deg, rgba(96, 165, 250, 0.2), rgba(34, 211, 238, 0.08)),
    rgba(255, 255, 255, 0.045);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.sidebar-brand strong,
.sidebar-brand span {
  display: block;
}

.sidebar-brand strong {
  color: rgba(248, 250, 252, 0.94);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.25;
}

.sidebar-brand span,
.sidebar-section p,
.session-meta {
  color: rgba(148, 163, 184, 0.66);
  font-size: 12px;
}

.new-chat,
.session-item,
.ghost-action,
.nav-action,
.prompt-card,
.send-button,
.sidebar-toggle,
.session-more,
.folder-card,
.folder-doc-item {
  border: 0;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease, box-shadow 180ms ease;
}

.new-chat {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  color: rgba(248, 250, 252, 0.88);
  font-size: 14px;
  font-weight: 560;
  background: rgba(255, 255, 255, 0.045);
}

.new-chat:hover,
.ghost-action:hover,
.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: rgba(125, 211, 252, 0.22);
  background: rgba(255, 255, 255, 0.075);
  box-shadow: none;
}

.sidebar-section {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.sidebar-section p {
  flex-shrink: 0;
  margin: 6px 0 0;
  color: rgba(148, 163, 184, 0.48);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.session-scroll-list {
  display: grid;
  flex: 1;
  align-content: start;
  gap: 6px;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding-right: 2px;
}

.session-scroll-list::-webkit-scrollbar,
.message-viewport::-webkit-scrollbar {
  width: 6px;
}

.session-scroll-list::-webkit-scrollbar-track,
.message-viewport::-webkit-scrollbar-track {
  background: transparent;
}

.session-scroll-list::-webkit-scrollbar-thumb,
.message-viewport::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(120, 180, 255, 0.24);
}

.session-scroll-list:hover::-webkit-scrollbar-thumb,
.message-viewport:hover::-webkit-scrollbar-thumb {
  background: rgba(120, 180, 255, 0.38);
}

.session-item {
  position: relative;
  display: grid;
  gap: 5px;
  width: 100%;
  min-height: 62px;
  padding: 10px 10px 10px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  color: rgba(226, 232, 240, 0.9);
  text-align: left;
  background: transparent;
}

.session-item:hover,
.session-item.active {
  transform: none;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.055);
}

.session-item.active {
  border-color: rgba(96, 165, 250, 0.14);
  background: rgba(96, 165, 250, 0.1);
  box-shadow: inset 2px 0 0 rgba(34, 211, 238, 0.72);
}

.session-empty {
  padding: 12px;
  border: 1px dashed rgba(148, 163, 184, 0.14);
  border-radius: 12px;
  color: rgba(148, 163, 184, 0.66);
  font-size: 13px;
  line-height: 1.6;
  background: rgba(255, 255, 255, 0.025);
}

.session-head,
.session-foot {
  display: flex;
  gap: 8px;
  align-items: center;
}

.session-head {
  min-width: 0;
}

.session-foot {
  flex-wrap: nowrap;
  color: rgba(148, 163, 184, 0.58);
  font-size: 11px;
}

.session-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 560;
}

.draft-badge,
.pin-badge,
.folder-chip {
  flex: 0 0 auto;
  font-size: 11px;
  line-height: 1;
}

.draft-badge {
  color: rgba(203, 213, 225, 0.6);
  font-weight: 650;
}

.pin-badge {
  color: rgba(103, 232, 249, 0.84);
  font-weight: 650;
}

.folder-chip {
  max-width: 112px;
  overflow: hidden;
  color: rgba(148, 163, 184, 0.62);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-dot {
  color: rgba(148, 163, 184, 0.42);
}

.session-more {
  display: grid;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 8px;
  color: rgba(203, 213, 225, 0.42);
  background: transparent;
}

.session-more:hover {
  color: rgba(255, 255, 255, 0.88);
  background: rgba(255, 255, 255, 0.07);
}

.chat-main {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  animation: enterSoft 640ms ease both;
}

.chat-topbar {
  position: relative;
  z-index: 20;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 34px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
  background: linear-gradient(180deg, rgba(7, 11, 24, 0.64), rgba(7, 11, 24, 0));
}

.sidebar-toggle {
  display: none;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 10px;
  color: rgba(226, 232, 240, 0.78);
  background: rgba(255, 255, 255, 0.045);
}

.eyebrow {
  margin: 0 0 5px;
  color: rgba(103, 232, 249, 0.74);
  font-size: 12px;
  font-weight: 650;
}

.chat-topbar h1 {
  margin: 0;
  color: rgba(248, 250, 252, 0.92);
  font-size: 24px;
  font-weight: 680;
  letter-spacing: 0;
}

.topbar-actions,
.dialog-actions,
.composer-tools {
  display: flex;
  gap: 8px;
  align-items: center;
}

.topbar-actions {
  padding: 4px;
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.035);
  backdrop-filter: blur(18px);
}

.config-warning {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid rgba(248, 113, 113, 0.32);
  border-radius: 999px;
  color: #fecaca;
  font-size: 12px;
  background: rgba(127, 29, 29, 0.26);
  box-shadow: 0 0 26px rgba(248, 113, 113, 0.12);
}

.ghost-action {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 13px;
  color: #e2e8f0;
  background: rgba(15, 23, 42, 0.46);
}

.nav-action {
  min-height: 30px;
  padding: 0 11px;
  border-radius: 10px;
  color: rgba(203, 213, 225, 0.66);
  font-size: 13px;
  font-weight: 560;
  background: transparent;
}

.nav-action:hover,
.nav-action.active {
  color: rgba(248, 250, 252, 0.94);
  background: rgba(255, 255, 255, 0.075);
  box-shadow: none;
}

.nav-action.active {
  box-shadow: inset 0 0 0 1px rgba(125, 211, 252, 0.12);
}

.ghost-action.warning {
  color: #fde68a;
  border-color: rgba(251, 191, 36, 0.34);
}

.message-viewport {
  flex: 1;
  min-height: 0;
  padding: 8px 34px 188px;
  overflow-x: hidden;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.welcome-panel {
  display: grid;
  gap: 34px;
  max-width: 1180px;
  min-height: calc(100vh - 180px);
  margin: 0 auto;
  padding: 8vh 0 48px;
}

.hero-stage {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.95fr);
  gap: 42px;
  align-items: center;
  min-height: 560px;
  isolation: isolate;
}

.hero-stage::before {
  position: absolute;
  inset: 9% 8% 12%;
  z-index: -1;
  border-radius: 50%;
  content: "";
  background:
    radial-gradient(circle at 50% 52%, rgba(34, 211, 238, 0.18), transparent 35%),
    radial-gradient(circle at 45% 46%, rgba(124, 58, 237, 0.16), transparent 44%);
  filter: blur(8px);
}

.hero-copy {
  animation: heroRise 760ms cubic-bezier(0.2, 0.72, 0.22, 1) both;
  transform: translate3d(0, calc(var(--scroll-progress) * -26px), 0);
}

.hero-kicker {
  margin: 0 0 14px;
  color: #8fb7ff;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.hero-copy h2 {
  max-width: 760px;
  margin: 0;
  color: #f8fbff;
  font-size: 64px;
  line-height: 1.08;
  letter-spacing: 0;
  text-shadow: 0 22px 80px rgba(34, 211, 238, 0.2);
}

.hero-subtitle {
  max-width: 660px;
  margin: 24px 0 0;
  color: #b8c8dd;
  font-size: 17px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 34px;
}

.primary-cta,
.secondary-cta {
  min-height: 48px;
  padding: 0 22px;
  border: 1px solid rgba(125, 211, 252, 0.32);
  border-radius: 999px;
  color: #f8fbff;
  font-weight: 700;
  background: rgba(15, 23, 42, 0.46);
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.primary-cta {
  color: #05101e;
  background: linear-gradient(135deg, #67e8f9, #818cf8 58%, #f0abfc);
  box-shadow: 0 20px 70px rgba(34, 211, 238, 0.25);
}

.secondary-cta {
  backdrop-filter: blur(18px);
}

.primary-cta:hover,
.secondary-cta:hover {
  transform: translateY(-3px);
  border-color: rgba(240, 171, 252, 0.46);
  box-shadow: 0 24px 80px rgba(124, 58, 237, 0.28);
}

.ocean-stage {
  position: relative;
  min-height: 480px;
  transform: translate3d(0, calc(var(--scroll-progress) * 34px), 0);
}

.knowledge-core {
  position: absolute;
  left: 50%;
  top: 48%;
  display: grid;
  width: 188px;
  height: 188px;
  place-items: center;
  border: 1px solid rgba(165, 243, 252, 0.36);
  border-radius: 42% 58% 51% 49%;
  color: #ecfeff;
  font-size: 28px;
  font-weight: 800;
  background:
    radial-gradient(circle at 35% 28%, rgba(255, 255, 255, 0.78), transparent 18%),
    linear-gradient(135deg, rgba(34, 211, 238, 0.92), rgba(124, 58, 237, 0.72));
  box-shadow:
    0 0 70px rgba(34, 211, 238, 0.36),
    inset 0 -18px 42px rgba(3, 7, 18, 0.34);
  transform: translate(-50%, -50%);
  animation: coreFloat 7s ease-in-out infinite;
}

.knowledge-core i {
  position: absolute;
  inset: -18px;
  border: 1px solid rgba(165, 243, 252, 0.18);
  border-radius: 48%;
  content: "";
  animation: slowSpin 18s linear infinite;
}

.stage-card {
  position: absolute;
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 22px;
  color: #e5eefc;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.58), rgba(15, 23, 42, 0.24));
  box-shadow: 0 24px 70px rgba(2, 6, 23, 0.32);
  backdrop-filter: blur(20px);
  animation: stageFloat 8s ease-in-out infinite;
}

.stage-card small {
  color: #67e8f9;
  font-size: 12px;
}

.stage-card span {
  color: #91a3bd;
  font-size: 12px;
}

.stage-card-main {
  left: 13%;
  right: 4%;
  bottom: 40px;
  min-height: 112px;
}

.stage-card-left {
  left: 3%;
  top: 92px;
  width: 154px;
  animation-delay: -2.4s;
}

.stage-card-right {
  right: 0;
  top: 138px;
  width: 160px;
  animation-delay: -5s;
}

.stage-bubble {
  position: absolute;
  border-radius: 999px;
  border: 1px solid rgba(125, 211, 252, 0.22);
  background: rgba(34, 211, 238, 0.14);
  box-shadow: 0 0 46px rgba(34, 211, 238, 0.16);
  animation: bubbleDrift 11s ease-in-out infinite;
}

.bubble-one {
  left: 22%;
  bottom: 150px;
  width: 22px;
  height: 22px;
}

.bubble-two {
  right: 28%;
  top: 54px;
  width: 14px;
  height: 14px;
  animation-delay: -5s;
}

.section-heading {
  display: grid;
  gap: 10px;
  max-width: 780px;
}

.section-heading span,
.preview-copy .status-pill {
  width: fit-content;
}

.section-heading span {
  color: #67e8f9;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
}

.section-heading h3,
.preview-copy h3 {
  margin: 0;
  color: #f8fbff;
  font-size: 34px;
  letter-spacing: 0;
}

.section-heading p,
.preview-copy p {
  margin: 0;
  color: #9fb0c8;
  line-height: 1.8;
}

.orbital-card {
  position: relative;
  padding: clamp(28px, 5vw, 54px);
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 28px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.78), rgba(15, 23, 42, 0.42));
  box-shadow: 0 34px 90px rgba(2, 6, 23, 0.48);
  backdrop-filter: blur(22px);
}

.orbital-card::after {
  position: absolute;
  right: -80px;
  top: -80px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(103, 232, 249, 0.22);
  border-radius: 999px;
  content: "";
  box-shadow: inset 0 0 50px rgba(59, 130, 246, 0.18);
}

.status-pill {
  display: inline-flex;
  margin-bottom: 18px;
  padding: 8px 12px;
  border: 1px solid rgba(103, 232, 249, 0.28);
  border-radius: 999px;
  color: #a5f3fc;
  background: rgba(14, 165, 233, 0.12);
}

.orbital-card h2 {
  margin: 0;
  font-size: clamp(36px, 6vw, 64px);
  letter-spacing: 0;
}

.orbital-card p {
  max-width: 720px;
  margin: 18px 0 0;
  color: #a9b8ce;
  font-size: 17px;
  line-height: 1.8;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.prompt-card {
  position: relative;
  display: grid;
  gap: 8px;
  min-height: 176px;
  padding: 20px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  color: #e5eefc;
  text-align: left;
  background:
    linear-gradient(145deg, rgba(15, 23, 42, 0.72), rgba(30, 41, 59, 0.34)),
    radial-gradient(circle at 12% 0%, rgba(34, 211, 238, 0.14), transparent 34%);
  backdrop-filter: blur(18px);
}

.prompt-card::after {
  position: absolute;
  right: -34px;
  top: -34px;
  width: 96px;
  height: 96px;
  border: 1px solid rgba(240, 171, 252, 0.18);
  border-radius: 999px;
  content: "";
  background: rgba(124, 58, 237, 0.12);
}

.feature-card {
  opacity: 0;
  transform: translateY(24px);
}

.feature-card.is-visible {
  opacity: 1;
  transform: translateY(0);
  transition: opacity 520ms ease, transform 520ms cubic-bezier(0.2, 0.72, 0.22, 1);
}

.prompt-card:hover {
  transform: translateY(-6px);
  border-color: rgba(103, 232, 249, 0.52);
  box-shadow: 0 24px 54px rgba(14, 165, 233, 0.18);
}

.feature-card.is-visible:hover {
  transform: translateY(-7px);
}

.prompt-card span {
  color: #67e8f9;
  font-size: 12px;
}

.prompt-card strong {
  font-size: 18px;
}

.prompt-card small {
  color: #9fb0c8;
  line-height: 1.6;
}

.mindmap-preview {
  display: grid;
  grid-template-columns: minmax(0, 0.78fr) minmax(360px, 1fr);
  gap: 30px;
  align-items: center;
  padding: 28px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 30px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.56), rgba(15, 23, 42, 0.24));
  box-shadow: 0 32px 100px rgba(2, 6, 23, 0.28);
  backdrop-filter: blur(22px);
}

.node-stage {
  position: relative;
  min-height: 280px;
  border: 1px solid rgba(125, 211, 252, 0.12);
  border-radius: 26px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    rgba(2, 6, 23, 0.24);
  background-size: 32px 32px;
  overflow: hidden;
}

.node-stage::before,
.node-stage::after {
  position: absolute;
  inset: 50% auto auto 50%;
  width: 72%;
  height: 1px;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(103, 232, 249, 0.36), transparent);
  transform: translate(-50%, -50%) rotate(18deg);
}

.node-stage::after {
  transform: translate(-50%, -50%) rotate(-22deg);
}

.map-node {
  position: absolute;
  z-index: 1;
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border: 1px solid rgba(125, 211, 252, 0.24);
  border-radius: 999px;
  color: #e8f8ff;
  background: rgba(15, 23, 42, 0.72);
  box-shadow: 0 12px 42px rgba(14, 165, 233, 0.14);
  backdrop-filter: blur(16px);
}

.map-center {
  left: 50%;
  top: 50%;
  min-height: 56px;
  padding: 0 22px;
  color: #05101e;
  font-weight: 800;
  background: linear-gradient(135deg, #67e8f9, #c4b5fd);
  transform: translate(-50%, -50%);
}

.map-a {
  left: 12%;
  top: 18%;
}

.map-b {
  right: 13%;
  top: 20%;
}

.map-c {
  left: 14%;
  bottom: 18%;
}

.map-d {
  right: 12%;
  bottom: 16%;
}

.folder-view {
  display: grid;
  gap: 22px;
  max-width: 980px;
  margin: 4vh auto 0;
  animation: enterSoft 360ms ease both;
}

.folder-hero {
  padding: 28px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 26px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.72), rgba(30, 41, 59, 0.34));
  box-shadow: 0 26px 70px rgba(2, 6, 23, 0.36);
  backdrop-filter: blur(22px);
}

.folder-hero h2 {
  margin: 0;
  font-size: clamp(34px, 5vw, 54px);
}

.folder-hero p {
  max-width: 680px;
  margin: 12px 0 0;
  color: #a9b8ce;
  line-height: 1.8;
}

.folder-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.folder-card {
  display: grid;
  gap: 9px;
  min-height: 128px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  color: #dce7f7;
  text-align: left;
  background: rgba(15, 23, 42, 0.5);
}

.folder-card:hover,
.folder-card.active {
  transform: translateY(-4px);
  border-color: rgba(103, 232, 249, 0.46);
  background: rgba(14, 165, 233, 0.14);
  box-shadow: 0 20px 48px rgba(14, 165, 233, 0.14);
}

.folder-card span {
  color: #67e8f9;
  font-size: 13px;
}

.folder-card strong {
  font-size: 18px;
}

.folder-card small {
  color: #91a3bd;
}

.folder-docs {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  background: rgba(15, 23, 42, 0.46);
}

.folder-docs-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #91a3bd;
}

.folder-docs-head h3 {
  margin: 0;
  color: #e5eefc;
}

.folder-empty {
  padding: 18px;
  border: 1px dashed rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  color: #91a3bd;
  text-align: center;
}

.folder-doc-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 15px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 16px;
  color: #dce7f7;
  text-align: left;
  background: rgba(30, 41, 59, 0.34);
}

.folder-doc-item:hover {
  transform: translateX(4px);
  border-color: rgba(103, 232, 249, 0.36);
  background: rgba(37, 99, 235, 0.16);
}

.folder-doc-item small {
  color: #91a3bd;
}

.message-list {
  display: grid;
  gap: 18px;
  max-width: 930px;
  margin: 0 auto;
  padding: 18px 0 24px;
}

.message-row {
  display: flex;
  gap: 12px;
  animation: bubbleIn 260ms ease both;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  display: grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 14px;
  color: #ecfeff;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.95), rgba(34, 211, 238, 0.55));
}

.message-row.user .avatar {
  background: linear-gradient(135deg, rgba(148, 163, 184, 0.75), rgba(71, 85, 105, 0.85));
}

.message-bubble {
  max-width: min(720px, 78%);
  padding: 15px 17px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  color: #dce7f7;
  background: rgba(15, 23, 42, 0.64);
  box-shadow: 0 14px 42px rgba(2, 6, 23, 0.2);
  backdrop-filter: blur(18px);
}

.message-row.user .message-bubble {
  color: #f8fbff;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.86), rgba(14, 165, 233, 0.62));
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.75;
}

.thinking {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  color: #b9c8dc;
}

.thinking i {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #67e8f9;
  animation: thinkingDot 1s ease-in-out infinite;
}

.thinking i:nth-child(3) {
  animation-delay: 0.14s;
}

.thinking i:nth-child(4) {
  animation-delay: 0.28s;
}

.composer-wrap {
  position: absolute;
  right: 34px;
  bottom: 28px;
  left: 34px;
  z-index: 30;
  padding: 0;
  pointer-events: none;
}

.composer {
  display: grid;
  gap: 10px;
  max-width: 930px;
  margin: 0 auto;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.78), rgba(10, 17, 34, 0.76)),
    rgba(15, 23, 42, 0.72);
  box-shadow:
    0 18px 54px rgba(2, 6, 23, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(24px);
  pointer-events: auto;
  transition: border-color 180ms ease, background 180ms ease, box-shadow 180ms ease;
}

.composer:focus-within {
  border-color: rgba(125, 211, 252, 0.32);
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.84), rgba(10, 17, 34, 0.82)),
    rgba(15, 23, 42, 0.8);
  box-shadow:
    0 22px 64px rgba(2, 6, 23, 0.46),
    0 0 0 1px rgba(34, 211, 238, 0.08);
}

.composer textarea {
  width: 100%;
  min-height: 44px;
  max-height: 180px;
  padding: 2px 2px 0;
  resize: none;
  border: 0;
  outline: 0;
  color: #f8fbff;
  background: transparent;
  font: inherit;
  line-height: 1.7;
}

.composer textarea::placeholder {
  color: rgba(148, 163, 184, 0.56);
}

.composer-tools {
  justify-content: space-between;
  padding-top: 2px;
}

.composer-left-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.composer select {
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid rgba(148, 163, 184, 0.13);
  border-radius: 10px;
  color: rgba(226, 232, 240, 0.86);
  font-size: 13px;
  background: rgba(255, 255, 255, 0.045);
}

.web-search-toggle {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid rgba(148, 163, 184, 0.13);
  border-radius: 10px;
  color: rgba(203, 213, 225, 0.7);
  font-size: 13px;
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
  transition: border-color 180ms ease, color 180ms ease, box-shadow 180ms ease;
}

.web-search-toggle:hover,
.web-search-toggle:has(input:checked) {
  color: rgba(165, 243, 252, 0.9);
  border-color: rgba(103, 232, 249, 0.24);
  box-shadow: none;
}

.web-search-toggle input {
  width: 14px;
  height: 14px;
  accent-color: #22d3ee;
}

.send-button {
  min-height: 36px;
  min-width: 84px;
  border-radius: 11px;
  color: #06101f;
  font-size: 13px;
  font-weight: 700;
  background: linear-gradient(135deg, #67e8f9, #60a5fa);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.move-dialog-body {
  display: grid;
  gap: 18px;
}

.move-dialog-body p {
  margin: 0;
  color: #475569;
}

.move-dialog-body label {
  display: grid;
  gap: 8px;
  color: #475569;
}

.move-dialog-body select {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #d6dce8;
  border-radius: 12px;
  color: #0f172a;
  background: #ffffff;
}

:deep(.el-dropdown__popper) {
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(15, 23, 42, 0.96);
  box-shadow: 0 18px 54px rgba(2, 6, 23, 0.35);
}

:deep(.el-dropdown-menu) {
  background: transparent;
}

:deep(.el-dropdown-menu__item) {
  color: #dce7f7;
}

:deep(.el-dropdown-menu__item:not(.is-disabled):focus),
:deep(.el-dropdown-menu__item:not(.is-disabled):hover) {
  color: #e8f8ff;
  background: rgba(14, 165, 233, 0.16);
}

@keyframes enterSoft {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes enterFromLeft {
  from {
    opacity: 0;
    transform: translateX(-16px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes bubbleIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes thinkingDot {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.35;
  }
  40% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

@keyframes floatGlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(24px, 18px, 0) scale(1.08);
  }
}

@keyframes heroRise {
  from {
    opacity: 0;
    transform: translate3d(0, 24px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@keyframes coreFloat {
  0%,
  100% {
    transform: translate(-50%, -50%) translate3d(0, 0, 0);
    border-radius: 42% 58% 51% 49%;
  }
  50% {
    transform: translate(-50%, -50%) translate3d(0, -16px, 0);
    border-radius: 54% 46% 44% 56%;
  }
}

@keyframes slowSpin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes stageFloat {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, -12px, 0);
  }
}

@keyframes bubbleDrift {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
    opacity: 0.44;
  }
  50% {
    transform: translate3d(12px, -26px, 0);
    opacity: 0.9;
  }
}

@keyframes driftArtifact {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(-4deg);
  }
  50% {
    transform: translate3d(18px, -24px, 0) rotate(5deg);
  }
}

@media (max-width: 980px) {
  .nexus-chat-shell {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: 300px;
    z-index: 40;
    transform: translateX(-105%);
    transition: transform 220ms ease;
  }

  .chat-sidebar.is-open {
    transform: translateX(0);
  }

  .sidebar-toggle {
    display: grid;
    place-items: center;
  }

  .prompt-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-stage,
  .mindmap-preview {
    grid-template-columns: 1fr;
  }

  .hero-stage {
    min-height: auto;
    padding-top: 34px;
  }

  .hero-copy h2 {
    font-size: 48px;
  }

  .ocean-stage {
    min-height: 360px;
  }

  .folder-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .chat-topbar,
  .message-viewport {
    padding-left: 16px;
    padding-right: 16px;
  }

  .message-viewport {
    padding-bottom: 178px;
  }

  .composer-wrap {
    right: 16px;
    bottom: 16px;
    left: 16px;
  }

  .chat-topbar {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .topbar-actions {
    width: 100%;
    justify-content: space-between;
    overflow-x: auto;
  }

  .prompt-grid {
    grid-template-columns: 1fr;
  }

  .welcome-panel {
    min-height: auto;
    padding-top: 28px;
  }

  .hero-copy h2 {
    font-size: 38px;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .ocean-stage {
    min-height: 300px;
  }

  .knowledge-core {
    width: 142px;
    height: 142px;
    font-size: 23px;
  }

  .stage-card-main {
    left: 0;
    right: 0;
    bottom: 14px;
  }

  .stage-card-left,
  .stage-card-right {
    width: 132px;
  }

  .stage-card-right {
    top: 84px;
  }

  .mindmap-preview {
    padding: 18px;
  }

  .node-stage {
    min-height: 260px;
  }

  .message-bubble {
    max-width: 82%;
  }

  .composer-tools {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .aurora,
  .artifact,
  .knowledge-core,
  .knowledge-core i,
  .stage-card,
  .stage-bubble,
  .message-row,
  .chat-main,
  .chat-sidebar {
    animation: none;
  }

  .hero-copy,
  .ocean-stage,
  .grid-overlay,
  .ocean-current {
    transform: none;
  }

  .feature-card {
    opacity: 1;
    transform: none;
  }
}
</style>
