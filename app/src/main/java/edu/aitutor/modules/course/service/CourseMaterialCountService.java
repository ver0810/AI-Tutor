package edu.aitutor.modules.course.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.modules.course.model.CourseMaterialEntity;
import edu.aitutor.modules.course.repository.CourseMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 知识库计数服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMaterialCountService {

    private final CourseMaterialRepository knowledgeBaseRepository;

    /**
     * 批量更新知识库提问计数（使用单条 SQL 批量更新）
     * 每个知识库的 questionCount +1，表示该知识库参与回答的次数
     *
     * @param knowledgeBaseIds 知识库ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestionCounts(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return;
        }

        // 去重
        List<Long> uniqueIds = knowledgeBaseIds.stream().distinct().toList();

        // 验证所有知识库是否存在
        Set<Long> existingIds = new HashSet<>(knowledgeBaseRepository.findAllById(uniqueIds)
                .stream().map(CourseMaterialEntity::getId).toList());

        for (Long id : uniqueIds) {
            if (!existingIds.contains(id)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在: " + id);
            }
        }

        // 批量更新（单条 SQL）
        int updated = knowledgeBaseRepository.incrementQuestionCountBatch(uniqueIds);
        log.debug("批量更新知识库提问计数: ids={}, updated={}", uniqueIds, updated);
    }
}
