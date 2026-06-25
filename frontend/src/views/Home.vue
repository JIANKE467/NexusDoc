<template>
  <section class="nexus-chat-shell">
    <div class="aurora aurora-one"></div>
    <div class="aurora aurora-two"></div>
    <div class="grid-overlay"></div>

    <aside :class="['chat-sidebar', { 'is-open': sidebarOpen }]">
      <div class="sidebar-brand">
        <div class="brand-mark">文</div>
        <div>
          <strong>NexusDoc</strong>
          <span>文档知识中枢</span>
        </div>
      </div>

      <button class="new-chat" type="button" @click="createSession">
        <span>+</span>
        新建对话
      </button>

      <div class="sidebar-section">
        <p>最近会话</p>
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
            <span v-if="session.pinned" class="pin-badge">置顶</span>
            <el-dropdown trigger="click" @command="(command) => handleSessionCommand(command, session)">
              <button class="session-more" type="button" @click.stop>⋯</button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="session.pinned ? 'unpin' : 'pin'">
                    {{ session.pinned ? '取消置顶' : '置顶' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="move">移动到档案夹</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="session-foot">
            <span class="session-meta">{{ session.updatedAt }}</span>
            <span class="folder-chip">{{ session.folderName || DEFAULT_FOLDER }}</span>
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
        <div class="topbar-actions">
          <span v-if="aiConfig && !aiConfig.apiKeyConfigured" class="config-warning">
            后端未配置 AI Key
          </span>
          <button
            :class="['nav-action', { active: activeNav === 'home' }]"
            type="button"
            @click="goWorkspaceHome"
          >
            主页
          </button>
          <button
            :class="['nav-action', { active: activeNav === 'chat' }]"
            type="button"
            @click="goAiChat"
          >
            AI 对话
          </button>
          <button
            :class="['nav-action', { active: activeNav === 'folders' }]"
            type="button"
            @click="openFolderView"
          >
            档案夹
          </button>
          <button class="nav-action" type="button" @click="$router.push('/history')">历史记录</button>
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
          <div class="orbital-card">
            <span class="status-pill">AI Document Intelligence</span>
            <h2>今天想处理什么文档？</h2>
            <p>上传、理解、总结、重写，让知识流动起来。把会议纪要、课程资料、政策通知或项目需求粘贴进来，NexusDoc 会把它整理成可继续追问的知识工作包。</p>
          </div>

          <div class="prompt-grid">
            <button
              v-for="prompt in promptCards"
              :key="prompt.title"
              class="prompt-card"
              type="button"
              @click="applyPrompt(prompt)"
            >
              <span>{{ prompt.kicker }}</span>
              <strong>{{ prompt.title }}</strong>
              <small>{{ prompt.description }}</small>
            </button>
          </div>
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
            <select v-model="selectedDocType" aria-label="文档类型">
              <option v-for="item in docTypes" :key="item" :value="item">{{ item }}</option>
            </select>
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
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAiConfig } from '../api/ai';
import { generateDocument } from '../api/document';
import { ANONYMOUS_USER_ID } from '../config/user';

const SESSION_STORAGE_KEY = 'nexusdoc-chat-sessions';
const DEFAULT_FOLDER = '默认档案夹';
const folderOptions = ['默认档案夹', '学习资料', '项目文档', '小说设定', '政策合同'];

const docTypes = [
  '通用总结',
  '会议纪要',
  '工作任务',
  '学习资料',
  '政策公告',
  '合同初读',
  '内容创作',
  '思维导图',
  '小说设定',
  '趋势与隐藏问题分析'
];
const promptCards = [
  {
    kicker: 'Summarize',
    title: '总结这份文档',
    description: '提炼摘要、背景、结论和可追踪事项',
    text: '请帮我总结这份文档，输出摘要、核心结论、关键事实和后续行动建议：\n\n'
  },
  {
    kicker: 'Extract',
    title: '提取重点',
    description: '按主题整理关键段落与重要信息',
    text: '请从下面内容中提取重点，并按主题分组整理：\n\n'
  },
  {
    kicker: 'Meeting',
    title: '生成会议纪要',
    description: '自动整理议题、决策、负责人和截止时间',
    text: '请把下面内容整理成正式会议纪要，包含会议主题、核心讨论、决议事项、负责人和截止时间：\n\n'
  },
  {
    kicker: 'Rewrite',
    title: '改写为正式公文',
    description: '提升表达规范度与结构完整度',
    text: '请把下面内容改写为正式公文风格，语言规范、结构清晰、表达克制：\n\n'
  }
];

const sessions = ref([]);
const activeSessionId = ref('');
const inputText = ref('');
const selectedDocType = ref('通用总结');
const sending = ref(false);
const sidebarOpen = ref(false);
const messageViewport = ref(null);
const aiConfig = ref(null);
const activeNav = ref('home');
const selectedFolder = ref(DEFAULT_FOLDER);
const moveDialogVisible = ref(false);
const movingSession = ref(null);
const targetFolder = ref(DEFAULT_FOLDER);

const activeSession = computed(() => sessions.value.find((session) => session.id === activeSessionId.value));
const activeMessages = computed(() => activeSession.value?.messages || []);
const sortedSessions = computed(() => {
  return [...sessions.value].sort((first, second) => {
    if (Boolean(first.pinned) !== Boolean(second.pinned)) {
      return first.pinned ? -1 : 1;
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
  await loadAiConfig();
  await nextTick();
  scrollToBottom();
});

async function loadAiConfig() {
  aiConfig.value = await getAiConfig();
}

function restoreSessions() {
  const cached = localStorage.getItem(SESSION_STORAGE_KEY);
  sessions.value = cached ? JSON.parse(cached).map(normalizeSession) : [createSessionData('新文档对话')];
  activeSessionId.value = sessions.value[0]?.id || '';
  persistSessions();
}

function persistSessions() {
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sessions.value));
}

function createSessionData(title) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    title,
    updatedAt: formatSessionTime(),
    updatedAtValue: Date.now(),
    pinned: false,
    pinnedTime: null,
    folderName: DEFAULT_FOLDER,
    messages: []
  };
}

function createSession() {
  const session = createSessionData('新文档对话');
  sessions.value.unshift(session);
  activeSessionId.value = session.id;
  inputText.value = '';
  sidebarOpen.value = false;
  activeNav.value = 'home';
  persistSessions();
}

function switchSession(sessionId) {
  activeSessionId.value = sessionId;
  sidebarOpen.value = false;
  activeNav.value = 'chat';
  nextTick(scrollToBottom);
}

function applyPrompt(prompt) {
  inputText.value = prompt.text;
  selectedDocType.value = prompt.title === '生成会议纪要' ? '会议纪要' : '通用总结';
  nextTick(() => {
    const textarea = document.querySelector('.composer textarea');
    textarea?.focus();
    resizeComposer({ target: textarea });
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

  const session = activeSession.value;
  activeNav.value = 'chat';
  const userMessage = createMessage('user', content);
  const assistantMessage = createMessage('assistant', '', true);
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
      content
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

function buildSessionTitle(content) {
  return content.replace(/\s+/g, ' ').slice(0, 18) || '新文档对话';
}

function formatSessionTime(date = new Date()) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date);
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function normalizeSession(session) {
  return {
    ...session,
    updatedAt: session.updatedAt || formatSessionTime(),
    updatedAtValue: session.updatedAtValue || Date.now(),
    pinned: Boolean(session.pinned),
    pinnedTime: session.pinnedTime || null,
    folderName: session.folderName || DEFAULT_FOLDER,
    messages: Array.isArray(session.messages) ? session.messages : []
  };
}

function getSessionTime(session) {
  return Number(session.updatedAtValue || session.pinnedTime || 0);
}

function goWorkspaceHome() {
  const emptySession = sortedSessions.value.find((session) => session.messages.length === 0);
  if (emptySession) {
    activeSessionId.value = emptySession.id;
  } else {
    const session = createSessionData('新文档对话');
    sessions.value.unshift(session);
    activeSessionId.value = session.id;
    persistSessions();
  }
  inputText.value = '';
  activeNav.value = 'home';
  nextTick(scrollToBottom);
}

function goAiChat() {
  activeNav.value = 'chat';
  nextTick(scrollToBottom);
}

function openFolderView() {
  activeNav.value = 'folders';
  sidebarOpen.value = false;
}

function openFolderSession(sessionId) {
  activeSessionId.value = sessionId;
  activeNav.value = 'chat';
  nextTick(scrollToBottom);
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
  grid-template-columns: 292px minmax(0, 1fr);
  min-height: calc(100vh - 65px);
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
}

.chat-sidebar,
.chat-main {
  position: relative;
  z-index: 1;
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 24px;
  border-right: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(6, 12, 27, 0.72);
  backdrop-filter: blur(24px);
  animation: enterFromLeft 560ms ease both;
}

.sidebar-brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid rgba(125, 211, 252, 0.4);
  border-radius: 14px;
  color: #f8fbff;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.85), rgba(45, 212, 191, 0.42));
  box-shadow: 0 16px 40px rgba(37, 99, 235, 0.34);
}

.sidebar-brand strong,
.sidebar-brand span {
  display: block;
}

.sidebar-brand strong {
  font-size: 17px;
}

.sidebar-brand span,
.sidebar-section p,
.session-meta {
  color: #91a3bd;
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
  min-height: 46px;
  border: 1px solid rgba(125, 211, 252, 0.28);
  border-radius: 14px;
  color: #e8f3ff;
  background: rgba(15, 23, 42, 0.74);
}

.new-chat:hover,
.ghost-action:hover,
.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 18px 42px rgba(59, 130, 246, 0.28);
}

.sidebar-section {
  display: grid;
  gap: 10px;
}

.session-item {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.13);
  border-radius: 16px;
  color: #dce7f7;
  text-align: left;
  background: rgba(15, 23, 42, 0.38);
}

.session-item:hover,
.session-item.active {
  transform: translateX(4px);
  border-color: rgba(96, 165, 250, 0.46);
  background: rgba(37, 99, 235, 0.18);
}

.session-empty {
  padding: 14px;
  border: 1px dashed rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  color: #91a3bd;
  font-size: 13px;
  line-height: 1.6;
  background: rgba(15, 23, 42, 0.24);
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
  justify-content: space-between;
}

.session-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pin-badge,
.folder-chip {
  flex: 0 0 auto;
  border-radius: 999px;
  font-size: 11px;
  line-height: 1;
}

.pin-badge {
  padding: 5px 7px;
  color: #a5f3fc;
  background: rgba(14, 165, 233, 0.14);
  box-shadow: inset 0 0 0 1px rgba(103, 232, 249, 0.2);
}

.folder-chip {
  max-width: 92px;
  padding: 4px 7px;
  overflow: hidden;
  color: #b7c5dc;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(148, 163, 184, 0.12);
}

.session-more {
  display: grid;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 10px;
  color: #b7c5dc;
  background: rgba(15, 23, 42, 0.5);
}

.session-more:hover {
  color: #e5eefc;
  background: rgba(59, 130, 246, 0.22);
}

.chat-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  animation: enterSoft 640ms ease both;
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 34px 18px;
}

.sidebar-toggle {
  display: none;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  color: #e2e8f0;
  background: rgba(15, 23, 42, 0.72);
}

.eyebrow {
  margin: 0 0 8px;
  color: #67e8f9;
  font-weight: 700;
}

.chat-topbar h1 {
  margin: 0;
  font-size: clamp(22px, 3vw, 34px);
  letter-spacing: 0;
}

.topbar-actions,
.dialog-actions,
.composer-tools {
  display: flex;
  gap: 12px;
  align-items: center;
}

.config-warning {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(248, 113, 113, 0.32);
  border-radius: 999px;
  color: #fecaca;
  font-size: 13px;
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
  min-height: 38px;
  padding: 0 13px;
  border-radius: 12px;
  color: #aebed2;
  background: transparent;
}

.nav-action:hover,
.nav-action.active {
  color: #e8f8ff;
  background: rgba(14, 165, 233, 0.13);
  box-shadow: 0 0 28px rgba(14, 165, 233, 0.12);
}

.ghost-action.warning {
  color: #fde68a;
  border-color: rgba(251, 191, 36, 0.34);
}

.message-viewport {
  min-height: 0;
  padding: 8px 34px 22px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-viewport::-webkit-scrollbar {
  width: 10px;
}

.message-viewport::-webkit-scrollbar-thumb {
  border: 3px solid transparent;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.44);
  background-clip: content-box;
}

.welcome-panel {
  display: grid;
  gap: 28px;
  max-width: 980px;
  margin: 8vh auto 0;
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
  gap: 14px;
}

.prompt-card {
  display: grid;
  gap: 8px;
  min-height: 158px;
  padding: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  color: #e5eefc;
  text-align: left;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.72), rgba(30, 41, 59, 0.42));
  backdrop-filter: blur(18px);
}

.prompt-card:hover {
  transform: translateY(-6px);
  border-color: rgba(103, 232, 249, 0.52);
  box-shadow: 0 24px 54px rgba(14, 165, 233, 0.18);
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
  padding: 18px 0 40px;
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
  padding: 14px 34px 28px;
}

.composer {
  display: grid;
  gap: 12px;
  max-width: 930px;
  margin: 0 auto;
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.72);
  box-shadow: 0 24px 70px rgba(2, 6, 23, 0.38);
  backdrop-filter: blur(24px);
  transition: border-color 180ms ease, box-shadow 180ms ease;
}

.composer:focus-within {
  border-color: rgba(103, 232, 249, 0.58);
  box-shadow: 0 28px 80px rgba(14, 165, 233, 0.24);
}

.composer textarea {
  width: 100%;
  min-height: 48px;
  max-height: 180px;
  resize: none;
  border: 0;
  outline: 0;
  color: #f8fbff;
  background: transparent;
  font: inherit;
  line-height: 1.7;
}

.composer textarea::placeholder {
  color: #7f91aa;
}

.composer-tools {
  justify-content: space-between;
}

.composer select {
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 12px;
  color: #dce7f7;
  background: rgba(15, 23, 42, 0.84);
}

.send-button {
  min-height: 40px;
  min-width: 92px;
  border-radius: 14px;
  color: #06101f;
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

@media (max-width: 980px) {
  .nexus-chat-shell {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: 292px;
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
}

@media (max-width: 640px) {
  .chat-topbar,
  .message-viewport,
  .composer-wrap {
    padding-left: 16px;
    padding-right: 16px;
  }

  .chat-topbar {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .topbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .prompt-grid {
    grid-template-columns: 1fr;
  }

  .message-bubble {
    max-width: 82%;
  }

  .composer-tools {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
