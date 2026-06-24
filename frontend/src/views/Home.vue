<template>
  <section class="page home">
    <div class="hero">
      <p class="eyebrow">AI 文档理解与知识整理助手</p>
      <h1>文枢 NexusDoc</h1>
      <p class="subtitle">
        把冗长、杂乱、难读的文档转化为摘要、要点、提纲、行动清单和可继续追问的知识工作包。
      </p>
      <div class="actions">
        <el-button type="primary" size="large" @click="$router.push('/create')">开始处理文档</el-button>
        <el-button size="large" @click="$router.push('/history')">查看历史记录</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <div class="capability">
          <h3>结构化输出</h3>
          <p>根据文档类型生成摘要、核心要点、待办事项、风险提醒和推荐追问。</p>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="capability">
          <h3>历史沉淀</h3>
          <p>保存原文和 AI 结果，随时回看处理过的文档工作包。</p>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="capability">
          <h3>基于文档追问</h3>
          <p>围绕当前文档继续提问，减少偏题和无依据回答。</p>
        </div>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.home {
  padding: 28px 0;
}

.hero {
  padding: 56px 0 42px;
}

.eyebrow {
  margin: 0 0 12px;
  color: #1d4ed8;
  font-weight: 700;
}

h1 {
  margin: 0;
  font-size: 48px;
  line-height: 1.1;
  color: #111827;
}

.subtitle {
  max-width: 720px;
  margin: 18px 0 28px;
  font-size: 18px;
  line-height: 1.8;
  color: #4b5563;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.capability {
  min-height: 150px;
  padding: 20px;
  border: 1px solid #d8dee9;
  border-radius: 8px;
  background: #ffffff;
}

.capability h3 {
  margin: 0 0 10px;
  color: #111827;
}

.capability p {
  margin: 0;
  line-height: 1.7;
  color: #6b7280;
}
</style>
