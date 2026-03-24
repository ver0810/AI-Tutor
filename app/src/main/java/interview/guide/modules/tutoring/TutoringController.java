package interview.guide.modules.tutoring;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.tutoring.model.*;
import interview.guide.modules.tutoring.service.TutoringHistoryService;
import interview.guide.modules.tutoring.service.TutoringPersistenceService;
import interview.guide.modules.tutoring.service.TutoringSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 测验控制器 (业务重构: 辅导测验)
 * 
 * API映射: /api/tutoring/* (兼容保留)
 * 业务流程: 创建测评 → AI出题 → 学生答题 → AI评估 → 学情报告
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TutoringController {
    
    private final TutoringSessionService sessionService;
    private final TutoringHistoryService historyService;
    private final TutoringPersistenceService persistenceService;
    
    /**
     * 创建辅导/测评会话
     */
    @PostMapping("/api/tutoring/sessions")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<TutoringSessionDTO> createSession(@RequestBody CreateTutoringRequest request) {
        log.info("创建辅导会话，题目数量: {}", request.questionCount());
        TutoringSessionDTO session = sessionService.createSession(request);
        return Result.success(session);
    }
    
    /**
     * 获取会话信息
     */
    @GetMapping("/api/tutoring/sessions/{sessionId}")
    public Result<TutoringSessionDTO> getSession(@PathVariable String sessionId) {
        TutoringSessionDTO session = sessionService.getSession(sessionId);
        return Result.success(session);
    }
    
    /**
     * 获取当前问题
     */
    @GetMapping("/api/tutoring/sessions/{sessionId}/question")
    public Result<Map<String, Object>> getCurrentQuestion(@PathVariable String sessionId) {
        return Result.success(sessionService.getCurrentQuestionResponse(sessionId));
    }
    
    /**
     * 提交答案
     */
    @PostMapping("/api/tutoring/sessions/{sessionId}/answers")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL}, count = 10)
    public Result<SubmitAnswerResponse> submitAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("提交答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        SubmitAnswerResponse response = sessionService.submitAnswer(request);
        return Result.success(response);
    }
    
    /**
     * 生成学籍评估报告
     */
    @GetMapping("/api/tutoring/sessions/{sessionId}/report")
    public Result<TutoringReportDTO> getReport(@PathVariable String sessionId) {
        log.info("生成学情评估报告: {}", sessionId);
        TutoringReportDTO report = sessionService.generateReport(sessionId);
        return Result.success(report);
    }
    
    /**
     * 查找未完成的测评会话
     * GET /api/tutoring/sessions/unfinished/{studentProfileId}
     */
    @GetMapping("/api/tutoring/sessions/unfinished/{studentProfileId}")
    public Result<TutoringSessionDTO> findUnfinishedSession(@PathVariable Long studentProfileId) {
        return Result.success(sessionService.findUnfinishedSessionOrThrow(studentProfileId));
    }
    
    /**
     * 暂存答案（不进入下一题）
     */
    @PutMapping("/api/tutoring/sessions/{sessionId}/answers")
    public Result<Void> saveAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("暂存答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        sessionService.saveAnswer(request);
        return Result.success(null);
    }
    
    /**
     * 提前交卷
     */
    @PostMapping("/api/tutoring/sessions/{sessionId}/complete")
    public Result<Void> completeTutoring(@PathVariable String sessionId) {
        log.info("提前交卷: {}", sessionId);
        sessionService.completeTutoring(sessionId);
        return Result.success(null);
    }
    
    /**
     * 获取测评会话详情
     * GET /api/tutoring/sessions/{sessionId}/details
     */
    @GetMapping("/api/tutoring/sessions/{sessionId}/details")
    public Result<TutoringDetailDTO> getTutoringDetail(@PathVariable String sessionId) {
        TutoringDetailDTO detail = historyService.getTutoringDetail(sessionId);
        return Result.success(detail);
    }
    
    /**
     * 导出评估报告为PDF
     */
    @GetMapping("/api/tutoring/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportTutoringPdf(@PathVariable String sessionId) {
        try {
            byte[] pdfBytes = historyService.exportTutoringPdf(sessionId);
            String filename = URLEncoder.encode("学籍测评报告_" + sessionId + ".pdf", 
                StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("导出PDF报告失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除测评会话
     */
    @DeleteMapping("/api/tutoring/sessions/{sessionId}")
    public Result<Void> deleteTutoring(@PathVariable String sessionId) {
        log.info("删除测评会话: {}", sessionId);
        persistenceService.deleteSessionBySessionId(sessionId);
        return Result.success(null);
    }
}
