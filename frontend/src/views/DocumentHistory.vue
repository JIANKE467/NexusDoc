<template>
  <section class="history-page">
    <div class="history-shell">
      <div class="history-header">
        <div>
          <p class="history-kicker">Knowledge Archive</p>
          <h1 class="history-title">历史记录</h1>
          <p class="history-desc">已处理的文档会沉淀在这里，方便继续查看、追问和复用。</p>
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
          <div class="state-title">暂无历史记录</div>
          <div class="state-desc">开始处理第一份文档后，它会出现在这里。</div>
          <button class="state-button" type="button" @click="$router.push('/')">处理新文档</button>
        </div>

        <el-table v-else class="history-table" :data="documents">
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="docType" label="类型" width="110" />
          <el-table-column prop="tag" label="标签" width="120">
            <template #default="{ row }">{{ row.tag || '未设置' }}</template>
          </el-table-column>
          <el-table-column prop="summaryPreview" label="摘要预览" min-width="260" show-overflow-tooltip />
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <button class="table-action" type="button" @click="$router.push(`/document/${row.documentId}`)">
                查看
              </button>
              <button class="table-action" type="button" @click="$router.push(`/chat/${row.documentId}`)">
                追问
              </button>
              <button class="table-action danger" type="button" @click="remove(row.documentId)">
                删除
              </button>
            </template>
          </el-table-column>
        </el-table>
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
</script>

<style scoped>
.history-page {
  min-height: 100%;
  overflow: auto;
  color: var(--nx-text);
  background:
    radial-gradient(circle at 20% 20%, rgba(37, 99, 235, 0.22), transparent 32%),
    radial-gradient(circle at 80% 15%, rgba(34, 211, 238, 0.12), transparent 28%),
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
  color: #04111f;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.92), rgba(59, 130, 246, 0.88));
  box-shadow: 0 12px 30px rgba(34, 211, 238, 0.18);
}

.history-action-button:hover,
.state-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 40px rgba(34, 211, 238, 0.24);
}

.history-panel {
  min-height: 420px;
  overflow: hidden;
  border: 1px solid var(--nx-border);
  border-radius: 24px;
  background: rgba(8, 18, 38, 0.82);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(20px);
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
  border: 1px solid rgba(34, 211, 238, 0.22);
  border-radius: 18px;
  color: #67e8f9;
  background: rgba(34, 211, 238, 0.1);
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
  color: #04111f;
  font-weight: 700;
  background: linear-gradient(135deg, #67e8f9, #60a5fa);
}

.table-action {
  margin-right: 10px;
  padding: 6px 8px;
  border-radius: 9px;
  color: rgba(165, 243, 252, 0.9);
  background: transparent;
}

.table-action:hover {
  background: rgba(34, 211, 238, 0.1);
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
  background: rgba(34, 211, 238, 0.08) !important;
}

:deep(.el-table__inner-wrapper::before),
:deep(.el-table__border-left-patch),
:deep(.el-table--border::after),
:deep(.el-table--border::before) {
  background: rgba(148, 163, 184, 0.08) !important;
}

:deep(.el-table__fixed-right),
:deep(.el-table__fixed-right-patch) {
  background: rgba(8, 18, 38, 0.94) !important;
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
}
</style>
