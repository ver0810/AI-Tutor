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
 * 简历历史服务
 * 简历历史和导出简历分析报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileHistoryService {

    private final StudentProfilePersistenceService studentProfilePersistenceService;
    private final TutoringPersistenceService aitutorPersistenceService;
    private final PdfExportService pdfExportService;
    private final ObjectMapper objectMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TutoringMapper aitutorMapper;

    /**
     * 获取所有简历列表
     */
    public List<StudentProfileListItemDTO> getAllStudentProfiles() {
        List<StudentProfileEntity> studentProfiles = studentProfilePersistenceService.findAllStudentProfiles();

        return studentProfiles.stream().map(studentProfile -> {
            // 获取最新分析结果的分数
            Integer latestScore = null;
            java.time.LocalDateTime lastAnalyzedAt = null;
            Optional<StudentProfileAnalysisEntity> analysisOpt = studentProfilePersistenceService.getLatestAnalysis(studentProfile.getId());
            if (analysisOpt.isPresent()) {
                StudentProfileAnalysisEntity analysis = analysisOpt.get();
                latestScore = analysis.getOverallScore();
                lastAnalyzedAt = analysis.getAnalyzedAt();
            }

            // 获取面试次数
            int aitutorCount = aitutorPersistenceService.findByStudentProfileId(studentProfile.getId()).size();

            // 使用 MapStruct 映射
            return new StudentProfileListItemDTO(
                studentProfile.getId(),
                studentProfile.getOriginalFilename(),
                studentProfile.getFileSize(),
                studentProfile.getUploadedAt(),
                studentProfile.getAccessCount(),
                latestScore,
                lastAnalyzedAt,
                aitutorCount
            );
        }).toList();
    }

    /**
     * 获取简历详情（包含分析历史）
     */
    public StudentProfileDetailDTO getStudentProfileDetail(Long id) {
        Optional<StudentProfileEntity> studentProfileOpt = studentProfilePersistenceService.findById(id);
        if (studentProfileOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_NOT_FOUND);
        }

        StudentProfileEntity studentProfile = studentProfileOpt.get();

        // 获取所有分析记录，使用 MapStruct 批量转换
        List<StudentProfileAnalysisEntity> analyses = studentProfilePersistenceService.findAnalysesByStudentProfileId(id);
        List<StudentProfileDetailDTO.AnalysisHistoryDTO> analysisHistory = studentProfileMapper.toAnalysisHistoryDTOList(
            analyses,
            this::extractStrengths,
            this::extractSuggestions
        );

        // 使用 TutoringMapper 转换面试历史
        List<Object> aitutorHistory = aitutorMapper.toAitutorHistoryList(
            aitutorPersistenceService.findByStudentProfileId(id)
        );

        return new StudentProfileDetailDTO(
            studentProfile.getId(),
            studentProfile.getOriginalFilename(),
            studentProfile.getFileSize(),
            studentProfile.getContentType(),
            studentProfile.getStorageUrl(),
            studentProfile.getUploadedAt(),
            studentProfile.getAccessCount(),
            studentProfile.getStudentProfileText(),
            studentProfile.getAnalyzeStatus(),
            studentProfile.getAnalyzeError(),
            analysisHistory,
            aitutorHistory
        );
    }

    /**
     * 从 JSON 提取 strengths
     */
    private List<String> extractStrengths(StudentProfileAnalysisEntity entity) {
        try {
            if (entity.getStrengthsJson() != null) {
                return objectMapper.readValue(
                    entity.getStrengthsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 strengths JSON 失败", e);
        }
        return List.of();
    }

    /**
     * 从 JSON 提取 suggestions
     */
    private List<Object> extractSuggestions(StudentProfileAnalysisEntity entity) {
        try {
            if (entity.getSuggestionsJson() != null) {
                return objectMapper.readValue(
                    entity.getSuggestionsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 suggestions JSON 失败", e);
        }
        return List.of();
    }

    /**
     * 导出简历分析报告为PDF
     */
    public ExportResult exportAnalysisPdf(Long studentProfileId) {
        Optional<StudentProfileEntity> studentProfileOpt = studentProfilePersistenceService.findById(studentProfileId);
        if (studentProfileOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_NOT_FOUND);
        }

        StudentProfileEntity studentProfile = studentProfileOpt.get();
        Optional<StudentProfileAnalysisResponse> analysisOpt = studentProfilePersistenceService.getLatestAnalysisAsDTO(studentProfileId);
        if (analysisOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_NOT_FOUND);
        }

        try {
            byte[] pdfBytes = pdfExportService.exportStudentProfileAnalysis(studentProfile, analysisOpt.get());
            String filename = "简历分析报告_" + studentProfile.getOriginalFilename() + ".pdf";

            return new ExportResult(pdfBytes, filename);
        } catch (Exception e) {
            log.error("导出PDF失败: studentProfileId={}", studentProfileId, e);
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "导出PDF失败: " + e.getMessage());
        }
    }

    /**
     * PDF导出结果
     */
    public record ExportResult(byte[] pdfBytes, String filename) {}
}

