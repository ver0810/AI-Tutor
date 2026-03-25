package edu.aitutor.modules.student.model;

import edu.aitutor.common.model.AsyncTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历详情DTO
 */
public record StudentProfileDetailDTO(
    Long id,
    String filename,
    Long fileSize,
    String contentType,
    String storageUrl,
    LocalDateTime uploadedAt,
    Integer accessCount,
    String studentProfileText,
    AsyncTaskStatus analyzeStatus,
    String analyzeError,
    List<AnalysisHistoryDTO> analyses,
    List<Object> aitutors  // 面试历史由TutoringHistoryService提供
) {
    /**
     * 分析历史DTO
     */
    public record AnalysisHistoryDTO(
        Long id,
        Integer overallScore,
        Integer contentScore,
        Integer structureScore,
        Integer skillMatchScore,
        Integer expressionScore,
        Integer projectScore,
        String summary,
        LocalDateTime analyzedAt,
        List<String> strengths,
        List<Object> suggestions
    ) {}
}

