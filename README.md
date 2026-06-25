# 文枢 NexusDoc

文枢 NexusDoc 是一个 AI 文档理解与知识整理助手，支持粘贴文档、选择处理模式、调用硅基流动 API 生成结构化文档工作包、查看历史记录和基于文档继续追问。

## 技术栈

- 后端：Java 17、Spring Boot、Spring Web、MyBatis-Plus、MySQL、Lombok
- 前端：Vue 3、Vite、Element Plus、Axios、Vue Router
- AI：硅基流动 Chat Completions API

## 后端启动

1. 创建数据库并执行脚本：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

2. 配置硅基流动 API Key。推荐复制本地配置模板：

```bash
cp application-local.example.yml application-local.yml
```

然后把 `application-local.yml` 中的 `siliconflow.api-key` 改成你的硅基流动 API Key。本地配置文件已加入 `.gitignore`，不会提交到仓库。

也可以使用环境变量：

```bash
export MYSQL_URL='jdbc:mysql://localhost:3306/nexusdoc?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export MYSQL_USERNAME='root'
export MYSQL_PASSWORD='你的数据库密码'
export SILICONFLOW_API_KEY='你的硅基流动 API Key'
```

3. 启动后端：

```bash
mvn spring-boot:run
```

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认前端地址为 `http://localhost:5173`，后端地址为 `http://localhost:8080`。

## 核心接口

- `GET /api/ai/config`：查看硅基流动模型配置状态，不返回 API Key
- `POST /api/ai/test`：测试硅基流动模型调用
- `POST /api/document/generate`：生成文档工作包
- `GET /api/document/list?userId=1`：获取历史记录
- `GET /api/document/detail/{documentId}`：查看文档详情
- `DELETE /api/document/{documentId}`：删除文档
- `POST /api/chat/ask`：文档追问
- `GET /api/chat/list?documentId=1`：获取追问记录

## 硅基流动配置

默认模型已经配置为：

```text
Qwen/Qwen3-8B
```

默认接口地址：

```text
https://api.siliconflow.cn/v1/chat/completions
```

启动后可用以下接口确认配置：

```bash
curl http://localhost:8080/api/ai/config
```

返回中的 `apiKeyConfigured` 必须为 `true`，AI 生成功能才能调用成功。测试模型：

```bash
curl -X POST http://localhost:8080/api/ai/test \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"请用一句话介绍文枢 NexusDoc"}'
```
