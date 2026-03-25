package edu.aitutor.modules.course;

import edu.aitutor.common.result.Result;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.service.TutorChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * RAG 聊天控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TutorChatController {

    private final TutorChatSessionService sessionService;

    /**
     * 创建新会话
     */
    @PostMapping({"/api/rag-chat/sessions", "/api/tutor-chat/sessions"})
    public Result<SessionDTO> createSession(@Valid @RequestBody TutorChatDTO.CreateSessionRequest request) {
        return Result.success(sessionService.createSession(request));
    }

    /**
     * 获取会话列表
     */
    @GetMapping({"/api/rag-chat/sessions", "/api/tutor-chat/sessions"})
    public Result<List<SessionListItemDTO>> listSessions() {
        return Result.success(sessionService.listSessions());
    }

    /**
     * 获取会话详情（包含消息历史）
     * GET /api/rag-chat/sessions/{sessionId}
     */
    @GetMapping({"/api/rag-chat/sessions/{sessionId}", "/api/tutor-chat/sessions/{sessionId}"})
    public Result<SessionDetailDTO> getSessionDetail(@PathVariable Long sessionId) {
        return Result.success(sessionService.getSessionDetail(sessionId));
    }

    /**
     * 更新会话标题
     */
    @PutMapping({"/api/rag-chat/sessions/{sessionId}/title", "/api/tutor-chat/sessions/{sessionId}/title"})
    public Result<Void> updateSessionTitle(
            @PathVariable Long sessionId,
            @Valid @RequestBody TutorChatDTO.UpdateTitleRequest request) {
        sessionService.updateSessionTitle(sessionId, request.title());
        return Result.success(null);
    }

    /**
     * 切换会话置顶状态
     * PUT /api/rag-chat/sessions/{sessionId}/pin
     */
    @PutMapping({"/api/rag-chat/sessions/{sessionId}/pin", "/api/tutor-chat/sessions/{sessionId}/pin"})
    public Result<Void> togglePin(@PathVariable Long sessionId) {
        sessionService.togglePin(sessionId);
        return Result.success(null);
    }

    /**
     * 更新会话知识库
     */
    @PutMapping({
            "/api/rag-chat/sessions/{sessionId}/knowledge-bases",
            "/api/rag-chat/sessions/{sessionId}/course-materials",
            "/api/tutor-chat/sessions/{sessionId}/knowledge-bases",
            "/api/tutor-chat/sessions/{sessionId}/course-materials"
    })
    public Result<Void> updateSessionCourseMaterials(
            @PathVariable Long sessionId,
            @Valid @RequestBody TutorChatDTO.UpdateCourseMaterialsRequest request) {
        sessionService.updateSessionCourseMaterials(sessionId, request.courseMaterialIds());
        return Result.success(null);
    }

    /**
     * 删除会话
     * DELETE /api/rag-chat/sessions/{sessionId}
     */
    @DeleteMapping({"/api/rag-chat/sessions/{sessionId}", "/api/tutor-chat/sessions/{sessionId}"})
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.success(null);
    }

    /**
     * 发送消息（流式SSE）
     * 流式响应设计：
     * 1. 先同步保存用户消息和创建 AI 消息占位
     * 2. 返回流式响应
     * 3. 流式完成后通过回调更新消息
     */
    @PostMapping(value = {"/api/rag-chat/sessions/{sessionId}/messages/stream", "/api/tutor-chat/sessions/{sessionId}/messages/stream"},
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessageStream(
            @PathVariable Long sessionId,
            @Valid @RequestBody TutorChatDTO.SendMessageRequest request) {

        log.info("收到 RAG 聊天流式请求: sessionId={}, question={}, 线程: {} (虚拟线程: {})",
            sessionId, request.question(), Thread.currentThread(), Thread.currentThread().isVirtual());

        // 1. 准备消息（保存用户消息，创建 AI 消息占位）
        Long messageId = sessionService.prepareStreamMessage(sessionId, request.question());

        // 2. 获取流式响应
        StringBuilder fullContent = new StringBuilder();

        return sessionService.getStreamAnswer(sessionId, request.question())
            .doOnNext(fullContent::append)
            // 使用 ServerSentEvent 包装，转义换行符避免破坏 SSE 格式
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk.replace("\n", "\\n").replace("\r", "\\r"))
                .build())
            .doOnComplete(() -> {
                // 3. 流式完成后更新消息内容
                sessionService.completeStreamMessage(messageId, fullContent.toString());
                log.info("RAG 聊天流式完成: sessionId={}, messageId={}", sessionId, messageId);
            })
            .doOnError(e -> {
                // 错误时也保存已接收的内容
                String content = !fullContent.isEmpty()
                    ? fullContent.toString()
                    : "【错误】回答生成失败：" + e.getMessage();
                sessionService.completeStreamMessage(messageId, content);
                log.error("RAG 聊天流式错误: sessionId={}", sessionId, e);
            });
    }
}
