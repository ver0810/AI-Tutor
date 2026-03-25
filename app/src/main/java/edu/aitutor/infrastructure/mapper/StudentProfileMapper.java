package edu.aitutor.infrastructure.mapper;

import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.student.model.StudentProfileAnalysisEntity;
import edu.aitutor.modules.student.model.StudentProfileDetailDTO;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import edu.aitutor.modules.student.model.StudentProfileListItemDTO;
import org.mapstruct.*;

import java.util.List;

/**
 * 简历相关的对象映射器
 * 使用MapStruct自动生成转换代码
 * <p>
 * 注意：JSON字段(strengthsJson, suggestionsJson)需要在Service层手动处理
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StudentProfileMapper {

    // ========== ScoreDetail 映射 ==========

    /**
     * 将实体基础字段映射到DTO的ScoreDetail
     */
    @Mapping(target = "contentScore", source = "contentScore", qualifiedByName = "nullToZero")
    @Mapping(target = "structureScore", source = "structureScore", qualifiedByName = "nullToZero")
    @Mapping(target = "skillMatchScore", source = "skillMatchScore", qualifiedByName = "nullToZero")
    @Mapping(target = "expressionScore", source = "expressionScore", qualifiedByName = "nullToZero")
    @Mapping(target = "projectScore", source = "projectScore", qualifiedByName = "nullToZero")
    StudentProfileAnalysisResponse.ScoreDetail toScoreDetail(StudentProfileAnalysisEntity entity);

    // ========== StudentProfileListItemDTO 映射 ==========

    /**
     * StudentProfileEntity 转换为 StudentProfileListItemDTO
     * 需要额外传入 latestScore, lastAnalyzedAt, aitutorCount
     */
    default StudentProfileListItemDTO toListItemDTO(
        StudentProfileEntity resume,
        Integer latestScore,
        java.time.LocalDateTime lastAnalyzedAt,
        Integer aitutorCount
    ) {
        return new StudentProfileListItemDTO(
            resume.getId(),
            resume.getOriginalFilename(),
            resume.getFileSize(),
            resume.getUploadedAt(),
            resume.getAccessCount(),
            latestScore,
            lastAnalyzedAt,
            aitutorCount
        );
    }

    /**
     * 简化版：从 StudentProfileEntity 直接映射（其他字段为 null）
     */
    @Mapping(target = "filename", source = "originalFilename")
    @Mapping(target = "latestScore", ignore = true)
    @Mapping(target = "lastAnalyzedAt", ignore = true)
    @Mapping(target = "aitutorCount", ignore = true)
    StudentProfileListItemDTO toListItemDTOBasic(StudentProfileEntity entity);

    // ========== StudentProfileDetailDTO 映射 ==========

    /**
     * StudentProfileEntity 转换为 StudentProfileDetailDTO（不含 analyses 和 aitutors）
     */
    @Mapping(target = "filename", source = "originalFilename")
    @Mapping(target = "analyses", ignore = true)
    @Mapping(target = "aitutors", ignore = true)
    StudentProfileDetailDTO toDetailDTOBasic(StudentProfileEntity entity);

    // ========== AnalysisHistoryDTO 映射 ==========

    /**
     * StudentProfileAnalysisEntity 转换为 AnalysisHistoryDTO
     * 注意：strengths 和 suggestions 需要在 Service 层从 JSON 解析后传入
     */
    @Mapping(target = "strengths", source = "strengths")
    @Mapping(target = "suggestions", source = "suggestions")
    StudentProfileDetailDTO.AnalysisHistoryDTO toAnalysisHistoryDTO(
        StudentProfileAnalysisEntity entity,
        List<String> strengths,
        List<Object> suggestions
    );

    /**
     * 批量转换（需要在 Service 层处理 JSON）
     */
    default List<StudentProfileDetailDTO.AnalysisHistoryDTO> toAnalysisHistoryDTOList(
        List<StudentProfileAnalysisEntity> entities,
        java.util.function.Function<StudentProfileAnalysisEntity, List<String>> strengthsExtractor,
        java.util.function.Function<StudentProfileAnalysisEntity, List<Object>> suggestionsExtractor
    ) {
        return entities.stream()
            .map(e -> toAnalysisHistoryDTO(e, strengthsExtractor.apply(e), suggestionsExtractor.apply(e)))
            .toList();
    }

    // ========== StudentProfileAnalysisEntity 创建映射 ==========

    /**
     * 从 StudentProfileAnalysisResponse 创建 StudentProfileAnalysisEntity
     * 注意：JSON 字段和 Resume 关联需要在 Service 层设置
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentProfile", ignore = true)
    @Mapping(target = "strengthsJson", ignore = true)
    @Mapping(target = "suggestionsJson", ignore = true)
    @Mapping(target = "analyzedAt", ignore = true)
    @Mapping(target = "contentScore", source = "scoreDetail.contentScore")
    @Mapping(target = "structureScore", source = "scoreDetail.structureScore")
    @Mapping(target = "skillMatchScore", source = "scoreDetail.skillMatchScore")
    @Mapping(target = "expressionScore", source = "scoreDetail.expressionScore")
    @Mapping(target = "projectScore", source = "scoreDetail.projectScore")
    StudentProfileAnalysisEntity toAnalysisEntity(StudentProfileAnalysisResponse response);

    /**
     * 更新已有的 StudentProfileAnalysisEntity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentProfile", ignore = true)
    @Mapping(target = "strengthsJson", ignore = true)
    @Mapping(target = "suggestionsJson", ignore = true)
    @Mapping(target = "analyzedAt", ignore = true)
    @Mapping(target = "contentScore", source = "scoreDetail.contentScore")
    @Mapping(target = "structureScore", source = "scoreDetail.structureScore")
    @Mapping(target = "skillMatchScore", source = "scoreDetail.skillMatchScore")
    @Mapping(target = "expressionScore", source = "scoreDetail.expressionScore")
    @Mapping(target = "projectScore", source = "scoreDetail.projectScore")
    void updateAnalysisEntity(StudentProfileAnalysisResponse response, @MappingTarget StudentProfileAnalysisEntity entity);

    // ========== 工具方法 ==========

    @Named("nullToZero")
    default int nullToZero(Integer value) {
        return value != null ? value : 0;
    }
}
