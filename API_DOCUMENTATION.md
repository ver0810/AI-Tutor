# AI-tutor API 接口文档

## 1. 学生档案 (Student Profiles)
- `POST /api/student/profiles/upload`: 上传简历并自动开始 AI 分析 (Multipart file)。
- `GET /api/student/profiles`: 获取所有已分析的学生档案列表。
- `GET /api/student/profiles/{id}`: 获取特定档案的详细分析结果（技能、优势等）。
- `GET /api/student/profiles/{id}/export/analysis`: 导出 AI 分析报告为 PDF 格式。
- `POST /api/student/profiles/{id}/reanalyze`: 触发重新分析。
- `DELETE /api/student/profiles/{id}`: 删除档案及关联数据。

## 2. 课程资料 (Course Materials)
- `POST /api/course/materials/upload`: 上传课程文件并触发向量化处理。
- `GET /api/course/materials/list`: 分页/条件查询课程资料列表。
- `GET /api/course/materials/{id}/analysis`: 获取 AI 对该资料生成的摘要与核心考点。
- `POST /api/course/materials/query`: 针对选定资料进行单次提问。
- `POST /api/course/materials/query/stream`: 针对选定资料进行**流式**提问 (SSE)。
- `PUT /api/course/materials/{id}/category`: 修改资料分类。
- `GET /api/course/materials/stats`: 获取资料库统计信息（总数、存储占用等）。

## 3. RAG 聊天会话 (Tutor Chat)
- `POST /api/tutor-chat/sessions`: 创建一个新的 AI 聊天会话（可关联多个资料）。
- `GET /api/tutor-chat/sessions`: 获取历史会话列表。
- `GET /api/tutor-chat/sessions/{sessionId}`: 获取会话详情及历史消息记录。
- `POST /api/tutor-chat/sessions/{sessionId}/messages/stream`: 在会话中发送新消息 (SSE 响应)。
- `PUT /api/tutor-chat/sessions/{sessionId}/pin`: 置顶/取消置顶会话。

## 4. 测评辅导 (Tutoring / Assessment)
- `POST /api/tutoring/sessions`: 发起一次针对性测评（指定题目数量、范围）。
- `GET /api/tutoring/sessions/{sessionId}/question`: 获取当前待回答的题目。
- `POST /api/tutoring/sessions/{sessionId}/answers`: 提交当前题目的答案并自动进入下一题。
- `GET /api/tutoring/sessions/{sessionId}/report`: 结束测评并获取完整的学情评估报告。
- `GET /api/tutoring/sessions/{sessionId}/export`: 下载 PDF 版评估报告。

## 通用返回结构
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```
