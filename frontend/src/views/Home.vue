<template>
  <section class="page home">
    <div class="workspace">
      <header class="workspace-header">
        <div>
          <p class="eyebrow">AI 文档理解与知识整理助手</p>
          <h1>文枢 NexusDoc</h1>
        </div>
        <div class="header-actions">
          <el-button v-if="aiConfig && !aiConfig.apiKeyConfigured" type="warning" plain @click="openAiDialog">
            配置 API Key
          </el-button>
          <el-button @click="$router.push('/history')">历史记录</el-button>
        </div>
      </header>

      <el-form label-position="top" @submit.prevent>
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="文档标题">
              <el-input v-model="form.title" placeholder="例如：项目需求会议记录" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="6">
            <el-form-item label="文档类型">
              <el-select v-model="form.docType" class="full">
                <el-option v-for="item in docTypes" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="6">
            <el-form-item label="标签">
              <el-input v-model="form.tag" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="文档正文">
          <el-input
            v-model="form.content"
            class="document-input"
            type="textarea"
            :rows="20"
            maxlength="20000"
            show-word-limit
            placeholder="请直接粘贴需要整理、总结或追问的文档内容"
          />
        </el-form-item>

        <div class="actions">
          <el-button type="primary" size="large" :loading="loading" @click="handleGenerate">生成文档工作包</el-button>
          <el-button size="large" @click="clearForm">清空</el-button>
        </div>
      </el-form>
    </div>

    <el-dialog v-model="aiDialogVisible" title="硅基流动 API 配置" width="560px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="API Key">
          <el-input
            v-model="aiForm.apiKey"
            type="password"
            show-password
            placeholder="请输入 SiliconFlow API Key"
          />
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input v-model="aiForm.baseUrl" />
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="aiForm.model" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="aiDialogVisible = false">取消</el-button>
          <el-button :loading="testingAi" @click="handleTestAi">测试连接</el-button>
          <el-button type="primary" :loading="savingAi" @click="handleSaveAi">保存配置</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getAiConfig, testAiConfig, updateAiConfig } from '../api/ai';
import { generateDocument } from '../api/document';
import { ANONYMOUS_USER_ID } from '../config/user';

const router = useRouter();
const loading = ref(false);
const savingAi = ref(false);
const testingAi = ref(false);
const aiDialogVisible = ref(false);
const aiConfig = ref(null);
const docTypes = ['通用总结', '会议纪要', '工作任务', '学习资料', '政策公告', '合同初读', '内容创作'];
const form = reactive({
  title: '',
  docType: '通用总结',
  tag: '',
  content: ''
});
const aiForm = reactive({
  apiKey: '',
  baseUrl: 'https://api.siliconflow.cn/v1/chat/completions',
  model: 'Qwen/Qwen3-8B'
});

onMounted(loadAiConfig);

async function loadAiConfig() {
  aiConfig.value = await getAiConfig();
  aiForm.baseUrl = aiConfig.value.baseUrl || aiForm.baseUrl;
  aiForm.model = aiConfig.value.model || aiForm.model;
  if (!aiConfig.value.apiKeyConfigured) {
    openAiDialog();
  }
}

async function handleGenerate() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和正文');
    return;
  }
  if (aiConfig.value && !aiConfig.value.apiKeyConfigured) {
    ElMessage.warning('请先配置硅基流动 API Key');
    openAiDialog();
    return;
  }
  loading.value = true;
  try {
    const result = await generateDocument({
      userId: ANONYMOUS_USER_ID,
      title: form.title,
      docType: form.docType,
      tag: form.tag,
      content: form.content
    });
    ElMessage.success('生成成功');
    router.push(`/document/${result.documentId}`);
  } catch (error) {
    if (error.message?.includes('API Key')) {
      openAiDialog();
    }
  } finally {
    loading.value = false;
  }
}

function openAiDialog() {
  aiDialogVisible.value = true;
}

async function handleSaveAi() {
  if (!aiForm.apiKey.trim()) {
    ElMessage.warning('请填写 API Key');
    return;
  }
  savingAi.value = true;
  try {
    aiConfig.value = await updateAiConfig({
      apiKey: aiForm.apiKey,
      baseUrl: aiForm.baseUrl,
      model: aiForm.model
    });
    ElMessage.success('AI 配置已保存');
    aiDialogVisible.value = false;
    aiForm.apiKey = '';
  } finally {
    savingAi.value = false;
  }
}

async function handleTestAi() {
  if (!aiForm.apiKey.trim() && (!aiConfig.value || !aiConfig.value.apiKeyConfigured)) {
    ElMessage.warning('请先保存 API Key');
    return;
  }
  testingAi.value = true;
  try {
    if (aiForm.apiKey.trim()) {
      await updateAiConfig({
        apiKey: aiForm.apiKey,
        baseUrl: aiForm.baseUrl,
        model: aiForm.model
      });
      aiConfig.value = await getAiConfig();
      aiForm.apiKey = '';
    }
    await testAiConfig();
    ElMessage.success('AI 连接测试成功');
    aiDialogVisible.value = false;
  } finally {
    testingAi.value = false;
  }
}

function clearForm() {
  form.title = '';
  form.tag = '';
  form.content = '';
  form.docType = '通用总结';
}
</script>

<style scoped>
.home {
  padding: 28px 0;
}

.workspace {
  padding: 28px;
  border: 1px solid #d8dee9;
  border-radius: 8px;
  background: #ffffff;
}

.workspace-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.header-actions,
.dialog-actions {
  display: flex;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 12px;
  color: #1d4ed8;
  font-weight: 700;
}

h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
  color: #111827;
}

.full {
  width: 100%;
}

.document-input :deep(.el-textarea__inner) {
  min-height: 420px !important;
  line-height: 1.7;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
