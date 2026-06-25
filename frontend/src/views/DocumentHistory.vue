<template>
  <section class="page">
    <div class="panel">
      <div class="header">
        <h1 class="page-title">历史记录</h1>
        <el-button type="primary" @click="$router.push('/')">处理新文档</el-button>
      </div>
      <el-table :data="documents" v-loading="loading" empty-text="暂无历史记录">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="docType" label="类型" width="110" />
        <el-table-column prop="tag" label="标签" width="120" />
        <el-table-column prop="summaryPreview" label="摘要预览" min-width="260" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="$router.push(`/document/${row.documentId}`)">查看</el-button>
            <el-button text type="primary" @click="$router.push(`/chat/${row.documentId}`)">追问</el-button>
            <el-button text type="danger" @click="remove(row.documentId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { deleteDocument, listDocuments } from '../api/document';
import { ANONYMOUS_USER_ID } from '../config/user';

const loading = ref(false);
const documents = ref([]);

onMounted(loadDocuments);

async function loadDocuments() {
  loading.value = true;
  try {
    documents.value = await listDocuments(ANONYMOUS_USER_ID);
  } finally {
    loading.value = false;
  }
}

async function remove(documentId) {
  await ElMessageBox.confirm('确定删除这份文档及其追问记录吗？', '确认删除', { type: 'warning' });
  await deleteDocument(documentId);
  ElMessage.success('删除成功');
  await loadDocuments();
}

function formatTime(value) {
  return value ? value.replace('T', ' ') : '';
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
