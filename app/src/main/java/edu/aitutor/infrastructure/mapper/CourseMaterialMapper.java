package edu.aitutor.infrastructure.mapper;

import edu.aitutor.modules.course.model.CourseMaterialEntity;
import edu.aitutor.modules.course.model.CourseMaterialListItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 知识库实体到DTO的映射器
 * 使用MapStruct自动生成转换代码
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CourseMaterialMapper {
    
    /**
     * 将知识库实体转换为列表项DTO
     */
    CourseMaterialListItemDTO toListItemDTO(CourseMaterialEntity entity);
    
    /**
     * 将知识库实体列表转换为列表项DTO列表
     */
    List<CourseMaterialListItemDTO> toListItemDTOList(List<CourseMaterialEntity> entities);
}

