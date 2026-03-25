package edu.aitutor.infrastructure.mapper;

import edu.aitutor.modules.course.model.CourseMaterialEntity;
import edu.aitutor.modules.course.model.CourseMaterialListItemDTO;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.model.TutorChatDTO;
import edu.aitutor.modules.course.model.TutorChatDTO.*;
import edu.aitutor.modules.course.model.TutorChatMessageEntity;
import edu.aitutor.modules.course.model.TutorChatSessionEntity;
import org.mapstruct.*;

import java.util.Collection;
import java.util.List;

/**
 * RAG聊天相关实体到DTO的映射器
 * 使用MapStruct自动生成转换代码
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = CourseMaterialMapper.class
)
public interface TutorChatMapper {
    
    /**
     * 将会话实体转换为会话DTO
     */
    @Mapping(target = "courseMaterialIds", source = "session", qualifiedByName = "extractCourseMaterialIds")
    SessionDTO toSessionDTO(TutorChatSessionEntity session);
    
    /**
     * 将消息实体转换为消息DTO
     */
    @Mapping(target = "type", source = "message", qualifiedByName = "getTypeString")
    MessageDTO toMessageDTO(TutorChatMessageEntity message);
    
    /**
     * 将消息实体列表转换为消息DTO列表
     */
    List<MessageDTO> toMessageDTOList(List<TutorChatMessageEntity> messages);
    
    /**
     * 将知识库实体集合转换为知识库名称列表
     * 支持 Set 和 List 类型
     */
    @Named("extractKnowledgeBaseNames")
    default List<String> extractKnowledgeBaseNames(Collection<CourseMaterialEntity> courseMaterials) {
        return courseMaterials.stream()
            .map(CourseMaterialEntity::getName)
            .toList();
    }
    
    /**
     * 从会话实体中提取知识库ID列表
     */
    @Named("extractCourseMaterialIds")
    default List<Long> extractCourseMaterialIds(TutorChatSessionEntity session) {
        return session.getCourseMaterialIds();
    }
    
    /**
     * 获取消息类型字符串
     */
    @Named("getTypeString")
    default String getTypeString(TutorChatMessageEntity message) {
        return message.getTypeString();
    }
    
    /**
     * 将会话实体转换为会话列表项DTO
     * 需要特殊处理：提取知识库名称列表和处理isPinned的null值
     */
    @Mapping(target = "knowledgeBaseNames", source = "session.courseMaterials", qualifiedByName = "extractKnowledgeBaseNames")
    @Mapping(target = "isPinned", source = "session", qualifiedByName = "getIsPinnedWithDefault")
    SessionListItemDTO toSessionListItemDTO(TutorChatSessionEntity session);
    
    /**
     * 处理isPinned的null值，默认为false
     */
    @Named("getIsPinnedWithDefault")
    default Boolean getIsPinnedWithDefault(TutorChatSessionEntity session) {
        return session.getIsPinned() != null ? session.getIsPinned() : false;
    }
    
    /**
     * 将会话实体和消息列表转换为会话详情DTO
     * 注意：这个方法需要手动实现，因为需要组合多个数据源
     * 知识库列表的转换在Service层使用CourseMaterialMapper完成
     */
    default SessionDetailDTO toSessionDetailDTO(
            TutorChatSessionEntity session, 
            List<TutorChatMessageEntity> messages,
            List<CourseMaterialListItemDTO> courseMaterials) {
        List<MessageDTO> messageDTOs = toMessageDTOList(messages);
        
        return new SessionDetailDTO(
            session.getId(),
            session.getTitle(),
            courseMaterials,
            messageDTOs,
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }
}

