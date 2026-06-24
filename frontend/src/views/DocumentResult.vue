<template>
  <section class="page">
    <div class="panel" v-loading="loading">
      <template v-if="detail">
        <div class="result-header">
          <div>
            <h1 class="page-title">{{ detail.title }}</h1>
            <p class="muted">{{ detail.docType }} · {{ detail.tag || '无标签' }} · {{ formatTime(detail.createTime) }}</p>
          </div>
          <div class="actions">
            <el-button @click="copyResult">复制结果</el-button>
            <el-button type="primary" @click="$router.push(`/chat/${detail.documentId}`)">继续追问</el-button>
            <el-button @click="$router.push('/history')">返回历史</el-button>
          </div>
        </div>
        <el-divider />
        <div class="result-content">{{ detail.resultText }}</div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getDocumentDetail } from '../api/document';

const route = useRoute();
const loading = ref(false);
const detail = ref(null);

onMounted(loadDetail);

async function loadDetail() {
  loading.value = true;
  try {
    detail.value = await getDocumentDetail(route.params.id);
  } finally {
    loading.value = false;
  }
}

async function copyResult() {
  await navigator.clipboard.writeText(detail.value?.resultText || '');
  ElMessage.success('已复制');
}

function formatTime(value) {
  return value ? value.replace('T', ' ') : '';
}
</script>

<style scoped>
.result-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
</style>
