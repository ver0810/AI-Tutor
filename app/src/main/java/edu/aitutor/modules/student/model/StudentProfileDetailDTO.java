package edu.aitutor.modules.student.model;

import edu.aitutor.common.model.AsyncTaskStatus;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse.LearningStep;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生档案详情DTO
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
    List<Object> aitutors
) {
    /**
     * 分析历史DTO (适配高校教研模式)
     */
    public record AnalysisHistoryDTO(
        Long id,
        Integer overallScore,
        Integer difficulty,
        String summary,
        List<String> tags,
        List<LearningStep> learningPath,
        LocalDateTime analyzedAt
    ) {}
}
