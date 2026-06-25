<template>
  <section class="page chat-page">
    <div class="panel" v-loading="loading">
      <template v-if="detail">
        <div class="header">
          <div>
            <h1 class="page-title">{{ detail.title }}</h1>
            <p class="muted">{{ detail.docType }} · {{ detail.tag || '无标签' }}</p>
          </div>
          <el-button @click="$router.push(`/document/${detail.documentId}`)">查看结果</el-button>
        </div>
        <el-collapse>
          <el-collapse-item title="查看文档原文" name="source">
            <div class="source-text">{{ detail.content }}</div>
          </el-collapse-item>
        </el-collapse>
        <div class="records">
          <div v-for="record in records" :key="record.id" class="record">
            <div class="question">问：{{ record.question }}</div>
            <div class="answer">答：{{ record.answer }}</div>
          </div>
          <el-empty v-if="records.length === 0" description="暂无追问记录" />
        </div>
        <div class="ask-box">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            placeholder="基于当前文档继续提问"
          />
          <el-button type="primary" :loading="asking" @click="sendQuestion">发送</el-button>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getDocumentDetail } from '../api/document';
import { askDocument, listChatRecords } from '../api/chat';
import { ANONYMOUS_USER_ID } from '../config/user';

const route = useRoute();
const loading = ref(false);
const asking = ref(false);
const detail = ref(null);
const records = ref([]);
const question = ref('');

onMounted(async () => {
  await loadPage();
});

async function loadPage() {
  loading.value = true;
  try {
    detail.value = await getDocumentDetail(route.params.id);
    records.value = await listChatRecords(route.params.id);
  } finally {
    loading.value = false;
  }
}

async function sendQuestion() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题');
    return;
  }
  asking.value = true;
  try {
    const answer = await askDocument({
      userId: ANONYMOUS_USER_ID,
      documentId: Number(route.params.id),
      question: question.value
    });
    records.value.push({
      id: answer.chatRecordId,
      question: answer.question,
      answer: answer.answer,
      createTime: answer.createTime
    });
    question.value = '';
  } finally {
    asking.value = false;
  }
}
</script>

<style scoped>
.chat-page {
  padding-bottom: 32px;
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.source-text {
  white-space: pre-wrap;
  line-height: 1.75;
  color: #374151;
}

.records {
  display: grid;
  gap: 14px;
  margin: 20px 0;
}

.record {
  padding: 16px;
  border: 1px solid #d8dee9;
  border-radius: 8px;
  background: #f9fafb;
}

.question {
  margin-bottom: 10px;
  font-weight: 700;
  color: #111827;
}

.answer {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #374151;
}

.ask-box {
  display: grid;
  gap: 12px;
}
</style>
