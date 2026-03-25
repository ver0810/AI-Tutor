package edu.aitutor.modules.course.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.infrastructure.file.FileStorageService;
import edu.aitutor.infrastructure.mapper.CourseMaterialMapper;
import edu.aitutor.modules.course.model.CourseMaterialEntity;
import edu.aitutor.modules.course.model.CourseMaterialListItemDTO;
import edu.aitutor.modules.course.model.CourseMaterialStatsDTO;
import edu.aitutor.modules.course.model.TutorChatMessageEntity.MessageType;
import edu.aitutor.modules.course.model.MaterialVectorStatus;
import edu.aitutor.modules.course.repository.CourseMaterialRepository;
import edu.aitutor.modules.course.repository.TutorChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 知识库查询服务
 * 负责知识库列表和详情的查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMaterialListService {

    private final CourseMaterialRepository knowledgeBaseRepository;
    private final TutorChatMessageRepository ragChatMessageRepository;
    private final CourseMaterialMapper knowledgeBaseMapper;
    private final FileStorageService fileStorageService;

    /**
     * 获取课程资料列表（支持课程ID隔离、状态过滤和排序）
     * 
     * @param courseId 课程ID，null 表示不过滤
     * @param vectorStatus 向量化状态，null 表示不过滤
     * @param sortBy 排序字段，null 或 "time" 表示按时间排序
     * @return 课程资料列表
     */
    public List<CourseMaterialListItemDTO> listCourseMaterials(Long courseId, MaterialVectorStatus vectorStatus, String sortBy) {
        List<CourseMaterialEntity> entities;
        
        if (courseId != null) {
            if (vectorStatus != null) {
                entities = knowledgeBaseRepository.findByCourseIdAndVectorStatusOrderByUploadedAtDesc(courseId, vectorStatus);
            } else {
                entities = knowledgeBaseRepository.findByCourseIdOrderByUploadedAtDesc(courseId);
            }
        } else {
            // 如果指定了状态，按状态过滤
            if (vectorStatus != null) {
                entities = knowledgeBaseRepository.findByVectorStatusOrderByUploadedAtDesc(vectorStatus);
            } else {
                // 否则获取所有课程资料
                entities = knowledgeBaseRepository.findAllByOrderByUploadedAtDesc();
            }
        }
        
        // 如果指定了排序字段，在内存中排序
        if (sortBy != null && !sortBy.isBlank() && !sortBy.equalsIgnoreCase("time")) {
            entities = sortEntities(entities, sortBy);
        }
        
        return knowledgeBaseMapper.toListItemDTOList(entities);
    }

    /**
     * 获取所有课程资料列表（保持向后兼容）
     */
    public List<CourseMaterialListItemDTO> listCourseMaterials() {
        return listCourseMaterials(null, null, null);
    }

    /**
     * 按向量化状态获取课程资料列表（保持向后兼容）
     */
    public List<CourseMaterialListItemDTO> listCourseMaterialsByStatus(MaterialVectorStatus vectorStatus) {
        return listCourseMaterials(null, vectorStatus, null);
    }

    /**
     * 根据ID获取知识库详情
     */
    public Optional<CourseMaterialListItemDTO> getCourseMaterial(Long id) {
        return knowledgeBaseRepository.findById(id)
            .map(knowledgeBaseMapper::toListItemDTO);
    }

    /**
     * 根据ID获取知识库实体（用于删除等操作）
     */
    public Optional<CourseMaterialEntity> getCourseMaterialEntity(Long id) {
        return knowledgeBaseRepository.findById(id);
    }

    /**
     * 根据ID列表获取知识库名称列表
     */
    public List<String> getCourseMaterialNames(List<Long> ids) {
        return ids.stream()
            .map(id -> knowledgeBaseRepository.findById(id)
                .map(CourseMaterialEntity::getName)
                .orElse("未知知识库"))
            .toList();
    }

    /**
     * 获取第一个知识库的 courseId（用于数据隔离）
     * @param ids 知识库ID列表
     * @return courseId，如果没有则返回 null
     */
    public Long getPrimaryCourseId(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return knowledgeBaseRepository.findById(ids.get(0))
            .map(CourseMaterialEntity::getCourseId)
            .orElse(null);
    }

    // ========== 分类管理 ==========

    /**
     * 获取所有分类
     */
    public List<String> getAllCategories() {
        return knowledgeBaseRepository.findAllCategories();
    }

    /**
     * 根据分类获取知识库列表
     */
    public List<CourseMaterialListItemDTO> listByCategory(String category) {
        List<CourseMaterialEntity> entities;
        if (category == null || category.isBlank()) {
            entities = knowledgeBaseRepository.findByCategoryIsNullOrderByUploadedAtDesc();
        } else {
            entities = knowledgeBaseRepository.findByCategoryOrderByUploadedAtDesc(category);
        }
        return knowledgeBaseMapper.toListItemDTOList(entities);
    }

    /**
     * 更新知识库分类
     */
    @Transactional
    public void updateCategory(Long id, String category) {
        CourseMaterialEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("知识库不存在"));
        entity.setCategory(category != null && !category.isBlank() ? category : null);
        knowledgeBaseRepository.save(entity);
        log.info("更新知识库分类: id={}, category={}", id, category);
    }

    // ========== 搜索功能 ==========

    /**
     * 按关键词搜索知识库
     */
    public List<CourseMaterialListItemDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return listCourseMaterials();
        }
        return knowledgeBaseMapper.toListItemDTOList(
            knowledgeBaseRepository.searchByKeyword(keyword.trim())
        );
    }

    // ========== 排序功能 ==========

    /**
     * 按指定字段排序获取知识库列表（保持向后兼容）
     */
    public List<CourseMaterialListItemDTO> listSorted(String sortBy) {
        return listCourseMaterials(null, null, sortBy);
    }

    /**
     * 在内存中对实体列表排序
     */
    private List<CourseMaterialEntity> sortEntities(List<CourseMaterialEntity> entities, String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "size" -> entities.stream()
                .sorted((a, b) -> Long.compare(b.getFileSize(), a.getFileSize()))
                .toList();
            case "access" -> entities.stream()
                .sorted((a, b) -> Integer.compare(b.getAccessCount(), a.getAccessCount()))
                .toList();
            case "question" -> entities.stream()
                .sorted((a, b) -> Integer.compare(b.getQuestionCount(), a.getQuestionCount()))
                .toList();
            default -> entities; // time 已经在数据库层面排序了
        };
    }

    // ========== 统计功能 ==========

    /**
     * 获取知识库统计信息
     * 总提问次数从用户消息数统计，确保多知识库提问只算一次
     */
    public CourseMaterialStatsDTO getStatistics() {
        return new CourseMaterialStatsDTO(
            knowledgeBaseRepository.count(),
            ragChatMessageRepository.countByType(MessageType.USER),  // 真正的提问次数
            knowledgeBaseRepository.sumAccessCount(),
            knowledgeBaseRepository.countByVectorStatus(MaterialVectorStatus.COMPLETED),
            knowledgeBaseRepository.countByVectorStatus(MaterialVectorStatus.PROCESSING)
        );
    }

    // ========== 下载功能 ==========

    /**
     * 获取资料内容预览（用于 AI 分析）
     */
    public String getMaterialContentPreview(Long id, int maxLength) {
        // 这里根据项目实际存储逻辑获取文本。
        // 如果文本是存储在数据库字段中的，直接读取。
        // 如果是按分片存储在 vector_store，则需要从 vector_store 中聚合。
        // 这里的简化实现是从存储中读取（如果上传时保存了全文），或者通过 repository 查询。
        return knowledgeBaseRepository.findById(id)
            .map(entity -> {
                String content = entity.getContent(); // 假设实体类中有 content 字段存储了完整文本
                if (content == null || content.isBlank()) {
                    return "（暂无文本内容）";
                }
                return content.length() > maxLength ? content.substring(0, maxLength) : content;
            })
            .orElse("资料不存在");
    }

    /**
     * 下载知识库文件
     */
    public byte[] downloadFile(Long id) {
        CourseMaterialEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_MATERIAL_NOT_FOUND, "知识库不存在"));

        String storageKey = entity.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "文件存储信息不存在");
        }

        log.info("下载知识库文件: id={}, filename={}", id, entity.getOriginalFilename());
        return fileStorageService.downloadFile(storageKey);
    }

    /**
     * 获取知识库文件信息（用于下载）
     */
    public CourseMaterialEntity getEntityForDownload(Long id) {
        return knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_MATERIAL_NOT_FOUND, "知识库不存在"));
    }
}

