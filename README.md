# 文枢 NexusDoc

文枢 NexusDoc 是一个 AI 文档理解与知识整理助手，支持用户注册登录、粘贴文档、选择处理模式、调用硅基流动 API 生成结构化文档工作包、查看历史记录和基于文档继续追问。

## 技术栈

- 后端：Java 17、Spring Boot、Spring Web、MyBatis-Plus、MySQL、Lombok
- 前端：Vue 3、Vite、Element Plus、Axios、Vue Router、Pinia
- AI：硅基流动 Chat Completions API

## 后端启动

1. 创建数据库并执行脚本：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

2. 设置环境变量：

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

- `POST /api/user/register`：用户注册
- `POST /api/user/login`：用户登录
- `POST /api/document/generate`：生成文档工作包
- `GET /api/document/list?userId=1`：获取历史记录
- `GET /api/document/detail/{documentId}`：查看文档详情
- `DELETE /api/document/{documentId}`：删除下文档
- `POST /api/chat/ask`：文档追问
- `GET /api/chat/list?documentId=1`：获取追问记录
