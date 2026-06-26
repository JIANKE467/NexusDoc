<template>
  <section class="history-page">
    <div class="history-shell">
      <div class="history-header">
        <div>
          <p class="history-kicker">Workspace Library</p>
          <h1 class="history-title">历史工作台库</h1>
          <p class="history-desc">已生成的知识卡片工作台会沉淀在这里，方便继续查看、追问、归档和复用。</p>
        </div>
        <button class="history-action-button" type="button" @click="$router.push('/')">
          处理新文档
        </button>
      </div>

      <div class="history-panel">
        <div v-if="loading" class="history-state">
          <div class="state-icon is-loading">DOC</div>
          <div class="state-title">正在加载历史记录...</div>
          <div class="state-desc">正在从文档知识库读取最近处理记录。</div>
        </div>

        <div v-else-if="errorMessage" class="history-state">
          <div class="state-icon is-error">!</div>
          <div class="state-title">历史记录加载失败</div>
          <div class="state-desc">{{ errorMessage }}</div>
          <button class="state-button" type="button" @click="loadDocuments">重新加载</button>
        </div>

        <div v-else-if="documents.length === 0" class="history-state">
          <div class="state-icon">DOC</div>
          <div class="state-title">暂无历史工作台</div>
          <div class="state-desc">生成第一组知识卡片后，它会出现在这里。</div>
          <button class="state-button" type="button" @click="$router.push('/')">生成知识卡片</button>
        </div>

        <div v-else class="history-workspace-grid">
          <article v-for="row in documents" :key="row.documentId" class="history-workspace-card">
            <div class="workspace-card-top">
              <span>{{ row.docType || '智能回答' }}</span>
              <small>{{ formatTime(row.createTime) }}</small>
            </div>
            <h2>{{ row.title || '未命名工作台' }}</h2>
            <p>{{ row.summaryPreview || '暂无摘要预览。打开工作台后可继续查看生成内容。' }}</p>
            <div class="workspace-meta">
              <span>{{ row.tag || '未设置标签' }}</span>
              <span>{{ estimateCardCount(row) }} 张卡片</span>
            </div>
            <div class="workspace-actions">
              <button type="button" @click="$router.push(`/document/${row.documentId}`)">打开</button>
              <button type="button" @click="$router.push(`/chat/${row.documentId}`)">继续追问</button>
              <button class="danger" type="button" @click="remove(row.documentId)">删除</button>
            </div>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deleteDocument, listDocuments } from '../api/document';
import { ANONYMOUS_USER_ID } from '../config/user';

const loading = ref(false);
const errorMessage = ref('');
const documents = ref([]);

onMounted(loadDocuments);

async function loadDocuments() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await listDocuments(ANONYMOUS_USER_ID);
    documents.value = normalizeHistoryList(result);
  } catch (error) {
    console.error('加载历史记录失败', error);
    documents.value = [];
    errorMessage.value = error?.message || '历史记录加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function remove(documentId) {
  try {
    await ElMessageBox.confirm('删除后将无法在历史记录中查看。', '确认删除这份文档吗？', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteDocument(documentId);
    ElMessage.success('删除成功');
    await loadDocuments();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error;
    }
  }
}

function normalizeHistoryList(result) {
  const payload = result?.data ?? result;
  const list = payload?.data ?? payload ?? [];
  return Array.isArray(list) ? list : [];
}

function formatTime(value) {
  return value ? value.replace('T', ' ') : '';
}

function estimateCardCount(row) {
  const text = `${row.summaryPreview || ''} ${row.resultText || ''}`;
  const headings = text.match(/【[^】]+】/g);
  return Math.max(3, Math.min(8, headings?.length || 4));
}
</script>

<style scoped>
.history-page {
  min-height: 100%;
  overflow: auto;
  color: var(--nx-text);
  background:
    radial-gradient(circle at 20% 0%, rgba(184, 216, 204, 0.08), transparent 32%),
    radial-gradient(circle at 80% 12%, rgba(216, 178, 110, 0.06), transparent 28%),
    var(--nx-bg);
}

.history-shell {
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
  padding: 46px 0;
}

.history-header {
  display: flex;
  gap: 18px;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 22px;
}

.history-kicker {
  margin: 0 0 8px;
  color: var(--nx-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.history-title {
  margin: 0;
  color: var(--nx-text);
  font-size: 32px;
  font-weight: 720;
  letter-spacing: 0;
}

.history-desc {
  max-width: 560px;
  margin: 10px 0 0;
  color: var(--nx-text-muted);
  line-height: 1.7;
}

.history-action-button,
.state-button,
.table-action {
  border: 0;
  cursor: pointer;
  transition: transform 180ms ease, background 180ms ease, box-shadow 180ms ease;
}

.history-action-button {
  min-height: 42px;
  padding: 0 18px;
  border-radius: 13px;
  color: #08110f;
  font-weight: 700;
  background: var(--nx-accent);
  box-shadow: none;
}

.history-action-button:hover,
.state-button:hover {
  transform: translateY(-1px);
  box-shadow: var(--nx-shadow-card);
}

.history-panel {
  min-height: 420px;
  overflow: hidden;
  border: 1px solid var(--nx-border);
  border-radius: 24px;
  background: var(--nx-panel);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(20px);
}

.history-workspace-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 18px;
}

.history-workspace-card {
  position: relative;
  display: grid;
  gap: 14px;
  min-height: 240px;
  padding: 22px;
  overflow: hidden;
  border: 1px solid rgba(246, 200, 111, 0.28);
  border-radius: 22px;
  background:
    radial-gradient(circle at 18% 0%, rgba(246, 200, 111, 0.12), transparent 32%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.01)),
    rgba(10, 12, 18, 0.94);
  box-shadow:
    0 18px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.035);
  backdrop-filter: blur(10px) saturate(1.02);
  -webkit-backdrop-filter: blur(10px) saturate(1.02);
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease, background 180ms ease;
}

.history-workspace-card::before {
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  content: "";
  opacity: 0.68;
  background: linear-gradient(90deg, transparent, rgba(246, 200, 111, 0.86), transparent);
}

.history-workspace-card:hover {
  transform: translateY(-2px);
  border-color: rgba(246, 200, 111, 0.36);
  background:
    radial-gradient(circle at 18% 0%, rgba(246, 200, 111, 0.14), transparent 34%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.042), rgba(255, 255, 255, 0.012)),
    rgba(12, 14, 20, 0.96);
  box-shadow:
    0 22px 64px rgba(0, 0, 0, 0.48),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.workspace-card-top,
.workspace-meta,
.workspace-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.workspace-card-top span,
.workspace-meta span {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid rgba(246, 200, 111, 0.16);
  border-radius: 999px;
  color: rgba(246, 200, 111, 0.86);
  background: rgba(246, 200, 111, 0.1);
  font-size: 12px;
  font-weight: 800;
}

.workspace-card-top small {
  color: rgba(248, 241, 228, 0.48);
  font-size: 12px;
  font-weight: 700;
}

.history-workspace-card h2 {
  margin: 0;
  color: rgba(255, 247, 231, 0.98);
  font-size: 21px;
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.history-workspace-card p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 5;
  margin: 0;
  color: rgba(248, 241, 228, 0.82);
  font-size: 14px;
  line-height: 1.72;
}

.workspace-actions {
  justify-content: flex-start;
  margin-top: auto;
}

.workspace-actions button {
  min-height: 34px;
  padding: 0 11px;
  border: 1px solid rgba(246, 200, 111, 0.16);
  border-radius: 12px;
  color: rgba(248, 241, 228, 0.74);
  font-weight: 760;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.workspace-actions button:hover {
  color: #1a1208;
  border-color: rgba(246, 200, 111, 0.42);
  background: linear-gradient(135deg, #ffe1a3, #d89531);
}

.workspace-actions .danger {
  color: rgba(255, 118, 107, 0.92);
  border-color: rgba(255, 118, 107, 0.2);
  background: rgba(255, 118, 107, 0.08);
}

.workspace-actions .danger:hover {
  color: #ffe7e5;
  border-color: rgba(255, 118, 107, 0.36);
  background: rgba(255, 118, 107, 0.16);
}

.history-state {
  display: grid;
  justify-items: center;
  padding: 72px 24px;
  text-align: center;
  color: var(--nx-text-muted);
}

.state-icon {
  display: grid;
  width: 54px;
  height: 54px;
  margin-bottom: 18px;
  place-items: center;
  border: 1px solid rgba(184, 216, 204, 0.18);
  border-radius: 18px;
  color: var(--nx-accent-strong);
  background: var(--nx-accent-soft);
}

.state-icon.is-loading {
  animation: pulseState 1.2s ease-in-out infinite;
}

.state-icon.is-error {
  color: #fecaca;
  border-color: rgba(248, 113, 113, 0.22);
  background: var(--nx-danger-soft);
}

.state-title {
  color: var(--nx-text);
  font-size: 18px;
  font-weight: 700;
}

.state-desc {
  max-width: 420px;
  margin-top: 8px;
  color: var(--nx-text-muted);
  font-size: 14px;
  line-height: 1.7;
}

.state-button {
  min-height: 38px;
  margin-top: 20px;
  padding: 0 16px;
  border-radius: 12px;
  color: #08110f;
  font-weight: 700;
  background: var(--nx-accent);
}

.table-action {
  margin-right: 10px;
  padding: 6px 8px;
  border-radius: 9px;
  color: var(--nx-accent-strong);
  background: transparent;
}

.table-action:hover {
  background: var(--nx-accent-soft);
}

.table-action.danger {
  color: #fca5a5;
}

.table-action.danger:hover {
  background: var(--nx-danger-soft);
}

:deep(.history-table),
:deep(.el-table),
:deep(.el-table__expanded-cell) {
  background: transparent !important;
  color: rgba(226, 232, 240, 0.9) !important;
}

:deep(.el-table th.el-table__cell) {
  color: rgba(148, 163, 184, 0.88) !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12) !important;
  background: rgba(15, 23, 42, 0.72) !important;
}

:deep(.el-table tr),
:deep(.el-table td.el-table__cell) {
  background: transparent !important;
}

:deep(.el-table td.el-table__cell) {
  color: rgba(226, 232, 240, 0.9) !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.08) !important;
}

:deep(.el-table__empty-text) {
  color: rgba(148, 163, 184, 0.72) !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background: var(--nx-accent-soft) !important;
}

:deep(.el-table__inner-wrapper::before),
:deep(.el-table__border-left-patch),
:deep(.el-table--border::after),
:deep(.el-table--border::before) {
  background: rgba(148, 163, 184, 0.08) !important;
}

:deep(.el-table__fixed-right),
:deep(.el-table__fixed-right-patch) {
  background: var(--nx-panel-strong) !important;
}

@keyframes pulseState {
  0%,
  100% {
    opacity: 0.62;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.04);
  }
}

@media (max-width: 720px) {
  .history-shell {
    width: calc(100% - 28px);
    padding: 28px 0;
  }

  .history-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .history-workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .history-page {
    width: 100%;
    overflow-x: hidden;
    -webkit-overflow-scrolling: touch;
  }

  .history-shell {
    width: 100%;
    padding: 22px 14px calc(28px + env(safe-area-inset-bottom));
  }

  .history-header {
    gap: 14px;
    align-items: stretch;
    flex-direction: column;
    margin-bottom: 16px;
  }

  .history-title {
    font-size: 28px;
    line-height: 1.12;
  }

  .history-desc {
    max-width: 100%;
    font-size: 14px;
    line-height: 1.65;
  }

  .history-action-button {
    width: 100%;
    min-height: 46px;
    justify-content: center;
  }

  .history-panel {
    min-height: 0;
    overflow: visible;
    border-radius: 20px;
  }

  .history-workspace-grid {
    grid-template-columns: 1fr !important;
    gap: 14px;
    padding: 14px;
  }

  .history-workspace-card {
    min-height: auto;
    padding: 18px;
    border-radius: 20px;
  }

  .history-workspace-card h2 {
    font-size: 20px;
    line-height: 1.25;
  }

  .history-workspace-card p {
    font-size: 14px;
    line-height: 1.65;
  }

  .workspace-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .workspace-actions button {
    min-height: 42px;
  }
}
</style>
