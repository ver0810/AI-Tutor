package edu.aitutor.modules.student.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.infrastructure.export.PdfExportService;
import edu.aitutor.infrastructure.mapper.TutoringMapper;
import edu.aitutor.infrastructure.mapper.StudentProfileMapper;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.tutoring.service.TutoringPersistenceService;
import edu.aitutor.modules.student.model.StudentProfileAnalysisEntity;
import edu.aitutor.modules.student.model.StudentProfileDetailDTO;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import edu.aitutor.modules.student.model.StudentProfileListItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * 资料分析历史服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileHistoryService {

    private final StudentProfilePersistenceService studentProfilePersistenceService;
    private final TutoringPersistenceService tutoringPersistenceService;
    private final PdfExportService pdfExportService;
    private final ObjectMapper objectMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TutoringMapper tutoringMapper;

    /**
     * 获取资料列表
     */
    public List<StudentProfileListItemDTO> getAllStudentProfiles() {
        List<StudentProfileEntity> profiles = studentProfilePersistenceService.findAllStudentProfiles();

        return profiles.stream().map(profile -> {
            Integer latestScore = null;
            java.time.LocalDateTime lastAnalyzedAt = null;
            Optional<StudentProfileAnalysisEntity> analysisOpt = studentProfilePersistenceService.getLatestAnalysis(profile.getId());
            if (analysisOpt.isPresent()) {
                latestScore = analysisOpt.get().getOverallScore();
                lastAnalyzedAt = analysisOpt.get().getAnalyzedAt();
            }

            int count = tutoringPersistenceService.findByStudentProfileId(profile.getId()).size();

            return new StudentProfileListItemDTO(
                profile.getId(),
                profile.getOriginalFilename(),
                profile.getFileSize(),
                profile.getUploadedAt(),
                profile.getAccessCount(),
                latestScore,
                lastAnalyzedAt,
                count
            );
        }).toList();
    }

    /**
     * 获取资料详情
     */
    public StudentProfileDetailDTO getStudentProfileDetail(Long id) {
        StudentProfileEntity profile = studentProfilePersistenceService.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        List<StudentProfileAnalysisEntity> analyses = studentProfilePersistenceService.findAnalysesByStudentProfileId(id);
        
        List<StudentProfileDetailDTO.AnalysisHistoryDTO> analysisHistory = analyses.stream()
            .map(entity -> {
                List<String> tags = extractTags(entity);
                List<StudentProfileAnalysisResponse.LearningStep> path = extractLearningPath(entity);
                return new StudentProfileDetailDTO.AnalysisHistoryDTO(
                    entity.getId(),
                    entity.getOverallScore(),
                    entity.getDifficulty(),
                    entity.getSummary(),
                    tags,
                    path,
                    entity.getAnalyzedAt()
                );
            }).toList();

        List<Object> tutoringHistory = tutoringMapper.toAitutorHistoryList(
            tutoringPersistenceService.findByStudentProfileId(id)
        );

        return new StudentProfileDetailDTO(
            profile.getId(),
            profile.getOriginalFilename(),
            profile.getFileSize(),
            profile.getContentType(),
            profile.getStorageUrl(),
            profile.getUploadedAt(),
            profile.getAccessCount(),
            profile.getStudentProfileText(),
            profile.getAnalyzeStatus(),
            profile.getAnalyzeError(),
            analysisHistory,
            tutoringHistory
        );
    }

    private List<String> extractTags(StudentProfileAnalysisEntity entity) {
        try {
            if (entity.getTagsJson() != null) {
                return objectMapper.readValue(entity.getTagsJson(), new TypeReference<List<String>>() {});
            }
        } catch (JacksonException e) {
            log.error("解析 tags 失败", e);
        }
        return List.of();
    }

    private List<StudentProfileAnalysisResponse.LearningStep> extractLearningPath(StudentProfileAnalysisEntity entity) {
        try {
            if (entity.getLearningPathJson() != null) {
                return objectMapper.readValue(entity.getLearningPathJson(), new TypeReference<List<StudentProfileAnalysisResponse.LearningStep>>() {});
            }
        } catch (JacksonException e) {
            log.error("解析 learningPath 失败", e);
        }
        return List.of();
    }

    /**
     * 导出分析报告 PDF
     */
    public ExportResult exportAnalysisPdf(Long id) {
        StudentProfileEntity profile = studentProfilePersistenceService.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        StudentProfileAnalysisResponse analysis = studentProfilePersistenceService.getLatestAnalysisAsDTO(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_NOT_FOUND));

        try {
            byte[] pdfBytes = pdfExportService.exportStudentProfileAnalysis(profile, analysis);
            String filename = "资料分析报告_" + profile.getOriginalFilename() + ".pdf";
            return new ExportResult(pdfBytes, filename);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "导出PDF失败");
        }
    }

    public record ExportResult(byte[] pdfBytes, String filename) {}
}
