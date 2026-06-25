# 文枢 NexusDoc 项目总览与开发注意事项

本文档用于说明当前 NexusDoc 项目的技术栈、功能范围、UI 设计方向，以及后续开发中必须保持谨慎的关键事项。

## 1. 项目定位

文枢 NexusDoc 是一个 AI 文档知识工作台。

项目当前不再只是普通 AI 聊天页面，也不只是文档总结工具，而是围绕“文档内容生成知识卡片”的产品体验进行设计：

- 用户粘贴文档、材料、需求或问题。
- 后端调用 AI 生成结构化内容。
- 前端把 AI 结果映射为摘要卡、观点卡、引用卡、任务卡、结构卡等可继续操作的知识卡片。
- 用户可以继续追问、复制、归档、查看历史记录、移动到档案夹。

项目对外仍然可以描述为：

> 基于硅基流动 AI 服务的文档理解、知识整理与卡片生成工作台。

## 2. 项目结构

```text
NexusDoc
├── pom.xml                         后端 Maven 配置
├── src/main/java/com/nexusdoc       Spring Boot 后端源码
├── src/main/resources/application.yml
├── frontend                         Vue 3 前端项目
│   ├── package.json
│   ├── vite.config.js
│   └── src
├── README.md
└── NexusDoc_项目总览与开发注意事项.md
```

## 3. 后端技术栈

- Java 17
- Spring Boot 3.3.5
- Spring Web
- MyBatis-Plus 3.5.9
- MySQL
- Lombok
- Spring Boot Configuration Processor
- Hutool Crypto
- Maven

后端包名统一为：

```text
com.nexusdoc
```

## 4. 前端技术栈

- Vue 3.5
- Vite 5
- Vue Router 4
- Element Plus 2
- Axios
- 原生 CSS / CSS Variables
- Vue Composition API

当前前端目录：

```text
frontend/src
├── App.vue
├── main.js
├── router/index.js
├── api
│   ├── ai.js
│   ├── chat.js
│   ├── document.js
│   └── request.js
├── assets/main.css
├── config/user.js
└── views
    ├── Home.vue
    ├── DocumentHistory.vue
    ├── DocumentResult.vue
    ├── DocumentChat.vue
    └── DocumentCreate.vue
```

## 5. AI 与搜索服务

### 5.1 AI 服务

项目保留 `siliconflow` 配置前缀，用于课程设计叙事和统一配置入口。

配置位置：

```text
src/main/resources/application.yml
```

当前结构：

```yaml
siliconflow:
  api-key: ${SILICONFLOW_API_KEY:}
  base-url: ${SILICONFLOW_BASE_URL:...}
  model: ${SILICONFLOW_MODEL:gpt-5.4}
  temperature: 0.4
  max-tokens: 4096
  stream: false
```

调用链路必须保持：

```text
前端 / App
-> NexusDoc 后端接口
-> DocumentService / ChatService
-> AiService
-> AI Chat Completions API
```

禁止链路：

```text
前端 / App -> AI Chat Completions API
```

### 5.2 联网搜索服务

项目已经加入基于 SerpApi 的联网搜索增强能力。

配置结构：

```yaml
web-search:
  enabled: true
  provider: serpapi
  api-key: ${SERPAPI_API_KEY:}
  base-url: ${SERPAPI_BASE_URL:https://serpapi.com/search}
  engine: ${SERPAPI_ENGINE:google}
  hl: ${SERPAPI_HL:zh-cn}
  gl: ${SERPAPI_GL:cn}
  num: 5
  timeout-seconds: 10
  cache-ttl-seconds: 600
```

联网搜索调用链路必须保持：

```text
前端 / App
-> NexusDoc 后端接口
-> DocumentService / ChatService
-> WebSearchService
-> SerpApi
-> PromptTemplateFactory
-> AiService
-> AI Chat Completions API
```

禁止链路：

```text
前端 / App -> SerpApi
```

## 6. 后端核心模块

### 6.1 Controller

- `AiController`
  - 查询 AI 配置状态。
  - 测试 AI 调用。
- `DocumentController`
  - 文档生成。
  - 历史列表。
  - 文档详情。
  - 删除文档。
- `ChatController`
  - 文档追问。
  - 追问记录列表。

Controller 只负责接收参数、调用 Service、返回统一响应，不允许直接调用第三方 AI 或搜索接口。

### 6.2 Service

- `AiService`
  - 统一封装 AI 文本生成。
- `DocumentService`
  - 文档生成、历史记录、文档详情。
- `ChatService`
  - 基于文档继续追问。
- `WebSearchService`
  - 统一封装 SerpApi 搜索逻辑。

实现类位于：

```text
src/main/java/com/nexusdoc/service/impl
```

### 6.3 AI Prompt

Prompt 构建集中在：

```text
src/main/java/com/nexusdoc/ai/PromptTemplateFactory.java
```

当前 Prompt 目标：

- 默认更像 GPT 式智能回答。
- 创作请求直接创作。
- 文档总结只在明确选择总结模式时使用结构化格式。
- 思维导图模式输出 JSON。
- 趋势分析模式输出趋势与隐藏问题结构。
- 开启联网搜索时自然融合搜索资料，并在需要时输出参考来源。

### 6.4 DTO / VO / Entity / Mapper

DTO：

- `DocumentGenerateRequest`
- `ChatAskRequest`
- `AiTestRequest`

VO：

- `DocumentListVO`
- `DocumentDetailVO`
- `ChatAnswerVO`
- `ChatRecordVO`
- `AiConfigVO`
- `WebSearchResultVO`

Entity：

- `Document`
- `ChatRecord`
- `DocumentPackage`

Mapper：

- `DocumentMapper`
- `ChatRecordMapper`
- `DocumentPackageMapper`

## 7. 前端核心页面

### 7.1 `App.vue`

负责全局应用壳和顶部导航。

当前导航：

- 主页
- 工作台
- 档案夹
- 历史记录
- Ctrl K 命令中心入口

顶部导航只保留一套，避免页面级导航和内容区导航重复。

### 7.2 `Home.vue`

当前主页面已经从传统聊天页面升级为“AI 卡片生成式文档工作台”。

主要模块：

- 左侧工作台 Sidebar
- 快速开始
- 最近工作台
- 卡片筛选器
- 档案夹视图
- Hero 卡片生成展示区
- Generative Input 生成输入区
- Generated Card Grid 结果卡片网格
- Source Rail 来源引用栏
- Ctrl / Cmd + K 命令中心

保留的核心交互：

- 新建工作台
- 草稿去重
- 发送 AI 请求
- 联网搜索开关
- 模式选择
- 置顶
- 移动到档案夹
- 删除
- 复制卡片
- 继续追问
- 查看原始生成文本

### 7.3 `DocumentHistory.vue`

历史记录页面已经从普通表格改为“历史工作台库”。

展示内容：

- 标题
- 类型
- 创建时间
- 摘要预览
- 估算卡片数量
- 打开
- 继续追问
- 删除

保留状态：

- 加载中
- 空状态
- 错误状态
- 重新加载

### 7.4 `DocumentResult.vue` / `DocumentChat.vue`

用于文档详情和基于文档继续追问。

后续如果继续 UI 统一，需要避免重新引入默认白色 Element Plus 风格。

## 8. 当前功能清单

### 8.1 文档处理能力

- 智能回答
- 通用总结
- 会议纪要
- 提取重点
- 正式改写
- 思维导图 JSON
- 小说设定
- 趋势分析
- 风险提醒
- 行动清单
- 隐藏问题分析
- 后续趋势判断

### 8.2 AI 对话能力

- 用户输入需求或文档。
- 后端生成 AI 结果。
- 前端将结果映射为知识卡片。
- 支持继续追问。
- 支持原始文本查看。

### 8.3 联网搜索能力

- 前端只传 `enableWebSearch=true/false`。
- 后端读取 `SERPAPI_API_KEY`。
- 后端调用 SerpApi。
- 搜索结果作为 AI Prompt 上下文。
- 前端展示来源引用卡片。

### 8.4 卡片工作台能力

当前前端会根据 AI 返回文本映射为：

- 摘要卡
- 观点卡
- 引用卡
- 任务卡
- 生成卡
- 结构卡

注意：目前卡片映射是前端规则初版。如果后端未来返回更结构化 JSON，卡片系统可以进一步增强。

### 8.5 工作台管理能力

- 新建工作台
- 草稿会话去重
- 最近工作台列表
- 置顶 / 取消置顶
- 移动到档案夹
- 删除确认
- 档案夹查看
- 历史工作台库

### 8.6 命令中心

触发方式：

- 顶部 `Ctrl K` 按钮
- Windows / Linux：`Ctrl + K`
- macOS：`Cmd + K`

当前命令：

- 生成摘要卡
- 提取观点卡
- 生成引用卡
- 生成任务卡
- 生成思维导图节点
- 分析趋势与风险
- 打开历史记录
- 打开档案夹
- 开启联网搜索
- 新建工作台

## 9. UI 设计说明

### 9.1 最新视觉方向

当前 UI 方向是：

```text
Warm Glass Generative Card Workspace
暖色通透 AI 卡片生成式文档工作台
```

设计参考：

- Gamma：卡片生成感、内容展示感、模块化结果。
- Vercel v0：一句话生成体验、生成式界面感。
- Perplexity：来源引用卡片、可信资料展示。
- Raycast：Ctrl K 命令中心、效率工具感。

### 9.2 颜色系统

当前全局 token 在：

```text
frontend/src/assets/main.css
```

核心色彩：

- 奶油白背景
- 暖白玻璃卡片
- 蜜桃金 / 香槟金强调色
- 柔和珊瑚色
- 雾蓝和灰青作为辅助色
- 暗棕黑文字
- 柔和暖色阴影

设计目标：

- 明亮但不刺眼。
- 温暖但不土气。
- 通透但不廉价。
- 有 Gamma 式卡片展示感。
- 避免蓝紫霓虹 AI 模板味。

### 9.3 页面结构

```text
App Shell
├── Warm Glass Header
├── Workspace Layout
│   ├── Sidebar
│   └── Main Workspace
│       ├── Hero
│       ├── Generative Input
│       ├── Generated Card Grid
│       ├── Source Rail
│       └── Raw Response
└── Command Center
```

### 9.4 交互风格

- 输入区像生成控制器，不是普通聊天输入框。
- AI 回复优先进入卡片区。
- 普通消息气泡降级为辅助形式。
- 卡片 hover 轻微上浮。
- 生成中显示 skeleton shimmer。
- 来源以 Citation Card 展示，不裸露长链接。
- 命令中心居中浮层，支持快捷键。

## 10. API 接口

前端通过 Axios 统一请求：

```text
frontend/src/api/request.js
```

基础路径：

```text
/api
```

核心前端 API：

- `getAiConfig()`
- `testAiConfig(prompt)`
- `generateDocument(data)`
- `listDocuments(userId)`
- `getDocumentDetail(documentId)`
- `deleteDocument(documentId)`
- `askDocument(data)`
- `listChatRecords(documentId)`

后端统一返回：

```text
ApiResponse<T>
```

前端拦截器会读取 `result.data`，如果 `code !== 200` 则弹出错误。

## 11. 运行方式

### 11.1 后端

```bash
mvn spring-boot:run
```

默认端口：

```text
8080
```

### 11.2 前端

```bash
cd frontend
npm install
npm run dev
```

或使用 pnpm：

```bash
cd frontend
pnpm install
pnpm run dev
```

默认端口：

```text
5173
```

访问地址：

```text
http://127.0.0.1:5173/
```

## 12. 必须保持谨慎的要点

### 12.1 API Key 安全

必须遵守：

- 不允许把真实 AI API Key 写进 Java 代码。
- 不允许把真实 AI API Key 写进 Vue 前端。
- 不允许把真实 AI API Key 写进 App 客户端。
- 不允许把真实 AI API Key 写进 `application.yml`。
- 不允许把真实 API Key 写进日志。
- 不允许把真实 API Key 写进运行报告。
- 不允许把真实 API Key 写进测试示例。
- 不允许把真实 API Key 提交到 Git。
- 不允许在浏览器控制台、前端源码、网络请求中暴露第三方密钥。

正确方式：

```bash
export SILICONFLOW_API_KEY='your_ai_api_key'
export SERPAPI_API_KEY='your_serpapi_api_key'
```

前端只请求自己的后端接口。

### 12.2 第三方服务调用边界

禁止：

```text
前端 -> AI API
前端 -> SerpApi
Controller -> AI API
Controller -> SerpApi
Mapper -> AI API
Mapper -> SerpApi
```

允许：

```text
Controller -> Service -> AiService / WebSearchService
```

### 12.3 不随意改后端接口

当前前端已经依赖这些接口路径：

```text
/api/ai/config
/api/ai/test
/api/document/generate
/api/document/list
/api/document/detail/{documentId}
/api/document/{documentId}
/api/chat/ask
/api/chat/list
```

除非明确要求，不要改路径。

### 12.4 不随意改数据库结构

不要为了前端视觉改数据库表。

如果确实需要新增字段，必须先说明原因和影响范围。

### 12.5 不破坏已有功能

后续 UI 优化必须保留：

- AI 调用
- 联网搜索
- 历史记录
- 文档详情
- 文档追问
- 档案夹
- 置顶
- 移动
- 删除
- 新建工作台草稿去重
- 输入框固定
- 左右独立滚动

### 12.6 不回退用户已有修改

工作区可能存在用户或前序开发改动。

要求：

- 不使用 `git reset --hard`。
- 不使用 `git checkout --` 回退未知文件。
- 不删除用户未确认的改动。
- 如果遇到无关改动，忽略即可。
- 如果遇到同文件改动，必须先读懂再继续。

### 12.7 前端视觉谨慎点

不要再回到：

- 蓝紫霓虹 AI 模板风。
- 大面积黑底强光效。
- 传统后台管理系统风格。
- 默认白色 Element Plus 组件。
- 过重边框和重复大色块。
- 两套重复导航。
- 全局 body 滚动导致左右栏互相影响。

当前应保持：

- 暖色通透。
- 玻璃卡片。
- 卡片生成式工作台。
- 顶部唯一主导航。
- 左侧内容导航。
- 主区卡片画布。
- 底部固定生成输入区。

### 12.8 Prompt 逻辑谨慎点

不要把所有用户输入都强行套进“文档摘要、核心要点、行动清单”格式。

应遵守：

- 用户要创作，就直接创作。
- 用户要问答，就直接回答。
- 用户要总结，才总结。
- 用户要思维导图，才输出 JSON。
- 用户要趋势分析，才输出趋势分析结构。
- 联网搜索结果自然融入回答，不机械堆叠标签。

### 12.9 日志谨慎点

日志可以记录：

```text
开始调用 AI 服务
AI 服务调用成功
AI 服务调用失败
```

禁止记录：

```text
API Key
Authorization Header
完整长文档原文
完整 SerpApi URL
真实第三方密钥
```

### 12.10 构建与验证谨慎点

前端修改后至少运行：

```bash
cd frontend
pnpm run build
```

或：

```bash
cd frontend
npm run build
```

后端修改后至少运行：

```bash
mvn test
```

或：

```bash
mvn spring-boot:run
```

如果构建失败，不能把失败状态交付为“完成”。

## 13. 后续建议

建议下一阶段继续完善：

- 卡片编辑。
- 卡片拖拽排序。
- 卡片组合。
- 卡片导出 Markdown / Word。
- 来源卡点击展开详情。
- 后端直接返回结构化卡片 JSON。
- 历史记录中保存卡片结构。
- 档案夹支持自定义新建、重命名、批量移动。
- 命令中心支持键盘上下选择。
- 移动端 Sidebar 抽屉化。

## 14. 管理者摘要

NexusDoc 当前已经从普通 AI 聊天页面升级为 AI 卡片生成式文档工作台。

项目使用 Spring Boot + Vue 3 + MySQL，后端统一管理 AI 和联网搜索调用，前端不暴露任何第三方密钥。用户可以输入文档或需求，系统调用后端 AI 服务生成内容，并在前端转化为摘要卡、观点卡、引用卡、任务卡、结构卡等知识卡片。

UI 方向已经从深色 AI 模板风调整为暖色通透的生成式工作台风格，更接近 Gamma、Vercel v0、Perplexity、Raycast 等产品的体验方向。

后续开发最重要的是：继续保护 API Key，不破坏后端接口和历史功能，不随意改数据库，不回退已有改动，并保持“文档知识卡片工作台”的产品定位。
