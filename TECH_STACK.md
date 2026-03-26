# AI-tutor 技术栈说明

本项目是一个前后端分离的 AI 智能辅导系统，核心技术选型追求高性能、现代语言特性及 AI 原生集成。

## 后端技术栈 (Backend)
- **核心框架**: Java 21 + Spring Boot 4.0 (Modular Design)
- **AI 集成**: Spring AI 2.0 (OpenAI 兼容模式，集成阿里云 DashScope / 通义千问)
- **数据库**: 
    - **关系型**: PostgreSQL 16+
    - **向量库**: pgvector (用于 RAG 知识检索)
- **中间件**:
    - **缓存 & 锁**: Redis (通过 Redisson 4.0 接入，支持分布式限流)
    - **对象存储**: MinIO / AWS S3 (存储原始课程资料与简历)
- **文档处理**:
    - **解析**: Apache Tika (支持 PDF, DOCX, TXT 文本提取)
    - **导出**: iText 8 (生成 PDF 学情报告，支持中文字体嵌入)
- **工具库**:
    - **对象映射**: MapStruct (高性能 DTO 转换)
    - **代码简化**: Lombok
    - **响应式**: Project Reactor (用于流式 SSE 输出)

## 前端技术栈 (Frontend)
- **核心框架**: React 18.3 + TypeScript
- **构建工具**: Vite 5
- **样式方案**: Tailwind CSS 4.0 + PostCSS (现代响应式设计)
- **交互动画**: Framer Motion (平滑的 UI 动效)
- **状态管理 & API**: Axios (基于拦截器的请求封装)
- **组件库**: 
    - **图标**: Lucide React
    - **图表**: Recharts (展示学生能力雷达图)
    - **Markdown**: React Markdown + Remark GFM (渲染 AI 回答内容)
    - **代码高亮**: React Syntax Highlighter
    - **长列表**: React Virtuoso (优化聊天记录性能)

## 开发与部署
- **容器化**: Docker & Docker Compose (预置 PostgreSQL, Redis, MinIO 环境)
- **构建工具**: Gradle 8.14
- **版本控制**: Git
