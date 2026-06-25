<template>
  <section class="page">
    <div class="panel">
      <h1 class="page-title">处理文档</h1>
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
            type="textarea"
            :rows="16"
            maxlength="20000"
            show-word-limit
            placeholder="请粘贴需要理解和整理的文档内容"
          />
        </el-form-item>
        <div class="actions">
          <el-button type="primary" :loading="loading" @click="handleGenerate">生成文档工作包</el-button>
          <el-button @click="clearForm">清空</el-button>
        </div>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { generateDocument } from '../api/document';
import { ANONYMOUS_USER_ID } from '../config/user';

const router = useRouter();
const loading = ref(false);
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
const form = reactive({
  title: '',
  docType: '通用总结',
  tag: '',
  content: ''
});

async function handleGenerate() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和正文');
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
  } finally {
    loading.value = false;
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
.full {
  width: 100%;
}

.actions {
  display: flex;
  gap: 12px;
}
</style>
