package edu.aitutor.modules.tutoring.model;

import java.util.List;

/**
 * 面试会话DTO
 */
public record TutoringSessionDTO(
    String sessionId,
    String studentProfileText,
    int totalQuestions,
    int currentQuestionIndex,
    List<TutoringQuestionDTO> questions,
    SessionStatus status
) {
    public enum SessionStatus {
        CREATED,      // 会话已创建
        IN_PROGRESS,  // 面试进行中
        COMPLETED,    // 面试已完成
        EVALUATED     // 已生成评估报告
    }
}
