# AI Tutor（高校学习助手）

AI Tutor 是一个面向高校课程学习场景的智能助手项目，提供「课程资料管理」「资料分析」「基于资料的问答」「学习测评与报告导出」四条核心能力链路。

本仓库当前处于持续重构阶段，本文档只描述**当前代码已具备或可直接验证**的能力，不做超出现状的承诺。

---

## 1. 项目定位

一句话定位：

> 面向高校教学与自学场景的 AI 学习助手，帮助用户完成资料沉淀、知识提问、学习分析与测评反馈。

典型使用路径：

1. 上传课程资料（PDF/DOCX/TXT 等）
2. 异步完成资料解析与向量化
3. 在资料范围内进行问答检索
4. 发起学习测评并查看结果
5. 导出分析/测评 PDF 报告

---

## 2. 当前核心能力（与代码一致）

### 2.1 课程资料模块

- 上传课程资料并保存元数据
- 课程资料列表、详情、分类、搜索、删除、下载
- 资料向量化任务异步处理（含重试）
- 手动触发重新向量化

后端入口：

- `CourseMaterialController`
- 主要路由前缀：`/api/course/materials/*`

### 2.2 资料分析模块

- 基于资料内容生成结构化分析结果
- 返回总结、标签、难度、学习路径等信息
- 支持前端分析展示与报告导出场景

后端入口：

- `CourseMaterialController#get /api/course/materials/{id}/analysis`
- `StudentProfileController#get /api/student/profiles/{id}/export/analysis`

### 2.3 智能问答模块（RAG）

- 支持基于指定资料集合进行问答
- 支持普通响应与 SSE 流式响应
- 支持会话化聊天接口（创建会话、历史记录、置顶、删除等）

后端入口：

- `CourseMaterialController#query` 与 `query/stream`
- `TutorChatController`（`/api/rag-chat/*`）

### 2.4 学习测评模块

- 创建测评会话、获取题目、提交答案、提前交卷
- 生成测评报告、获取测评详情、删除测评记录
- 导出测评 PDF

后端入口：

- `TutoringController`
- 路由前缀：`/api/tutoring/*`

---

## 3. 关键工程亮点（用于面试）

### 3.1 异步任务链路（Redis Stream）

- 使用异步任务发布/订阅模板封装统一处理流程
- 覆盖资料分析、向量化、测评评估等长耗时任务
- 具备重试次数控制与失败回写

### 3.2 RAG 检索问答

- 对课程资料进行解析、切分、向量化
- 问答时结合向量检索结果进行生成，降低幻觉风险
- 提供 SSE 流式输出，提升交互体验

### 3.3 结构化报告导出

- 结合分析/测评结果生成可分享的 PDF 报告
- 可用于课程复盘与学习反馈归档

### 3.4 基础治理能力

- 限流注解（`@RateLimit`）用于关键接口保护
- 统一错误码与业务异常处理

---

## 4. 技术栈

### 后端

- Java 21+
- Spring Boot 4
- Spring AI 2
- PostgreSQL + pgvector
- Redis（Stream）
- Gradle

### 前端

- React 18 + TypeScript
- Vite
- Tailwind CSS

---

## 5. 目录结构（简化）

```text
AI-tutor/
├── app/                      # 后端
│   ├── src/main/java/edu/aitutor/
│   │   ├── common/           # 通用组件（异常、限流、异步模板等）
│   │   ├── infrastructure/   # 基础设施（Redis、文件、导出等）
│   │   └── modules/          # 业务模块（course/student/tutoring）
│   └── src/main/resources/   # 配置与提示词
├── frontend/                 # 前端
│   └── src/
│       ├── api/
│       ├── components/
│       └── pages/
├── PRODUCT_SCOPE.md          # 产品能力边界（事实版）
└── DEMO_SCRIPT.md            # 演示脚本与面试讲稿
```

---

## 6. 快速启动

### 6.1 环境要求

- JDK 21+
- Node.js 18+
- PostgreSQL（启用 pgvector）
- Redis 6+

### 6.2 后端启动

```bash
./gradlew :app:bootRun
```

### 6.3 前端启动

```bash
cd frontend
npm install
npm run dev
```

---

## 7. 当前已知事项（如实说明）

项目仍在重构收口中，部分历史页面与 API 契约仍在统一，短期重点是：

1. 术语一致性收口（旧语义向高校学习语义过渡）
2. 前端 history/tutoring 类型契约修正
3. 端到端演示链路稳定性增强（上传→分析→问答→测评→导出）

这部分不影响你作为面试项目展示核心架构亮点，但建议在演示前按 `DEMO_SCRIPT.md` 完成一次全链路走查。

---

## 8. License

AGPL-3.0
