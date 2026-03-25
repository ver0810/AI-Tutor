package edu.aitutor.infrastructure.mapper;

import edu.aitutor.modules.student.model.StudentProfileDetailDTO;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import edu.aitutor.modules.student.model.StudentProfileListItemDTO;
import org.mapstruct.*;

import java.util.List;

/**
 * 学生档案相关的对象映射器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StudentProfileMapper {

    // ========== StudentProfileListItemDTO 映射 ==========

    /**
     * StudentProfileEntity 转换为 StudentProfileListItemDTO
     */
    default StudentProfileListItemDTO toListItemDTO(
        StudentProfileEntity profile,
        Integer latestScore,
        java.time.LocalDateTime lastAnalyzedAt,
        Integer tutoringsCount
    ) {
        return new StudentProfileListItemDTO(
            profile.getId(),
            profile.getOriginalFilename(),
            profile.getFileSize(),
            profile.getUploadedAt(),
            profile.getAccessCount(),
            latestScore,
            lastAnalyzedAt,
            tutoringsCount
        );
    }

    @Mapping(target = "filename", source = "originalFilename")
    @Mapping(target = "latestScore", ignore = true)
    @Mapping(target = "lastAnalyzedAt", ignore = true)
    @Mapping(target = "aitutorCount", ignore = true)
    StudentProfileListItemDTO toListItemDTOBasic(StudentProfileEntity entity);

    // ========== StudentProfileDetailDTO 映射 ==========

    @Mapping(target = "filename", source = "originalFilename")
    @Mapping(target = "analyses", ignore = true)
    @Mapping(target = "aitutors", ignore = true)
    StudentProfileDetailDTO toDetailDTOBasic(StudentProfileEntity entity);

    /**
     * 映射分析历史
     */
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "learningPath", source = "learningPath")
    StudentProfileDetailDTO.AnalysisHistoryDTO toAnalysisHistoryDTO(
        edu.aitutor.modules.student.model.StudentProfileAnalysisEntity entity,
        List<String> tags,
        List<edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse.LearningStep> learningPath
    );

    // ========== 工具方法 ==========

    @Named("nullToZero")
    default int nullToZero(Integer value) {
        return value != null ? value : 0;
    }
}
