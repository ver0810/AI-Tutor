package edu.aitutor.modules.course.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.infrastructure.file.FileStorageService;
import edu.aitutor.modules.course.model.CourseMaterialEntity;
import edu.aitutor.modules.course.model.TutorChatSessionEntity;
import edu.aitutor.modules.course.repository.CourseMaterialRepository;
import edu.aitutor.modules.course.repository.TutorChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库删除服务
 * 负责知识库的删除操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMaterialDeleteService {
    
    private final CourseMaterialRepository knowledgeBaseRepository;
    private final TutorChatSessionRepository sessionRepository;
    private final CourseMaterialVectorService vectorService;
    private final FileStorageService storageService;
    
    /**
     * 删除知识库
     * 包括：RAG会话关联、向量数据、RustFS文件、数据库记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourseMaterial(Long id) {
        // 1. 获取知识库信息
        CourseMaterialEntity kb = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        
        // 2. 删除所有RAG会话中的知识库关联（必须先删除关联，否则外键约束会阻止删除）
        List<TutorChatSessionEntity> sessions = sessionRepository.findByCourseMaterialIds(List.of(id));
        for (TutorChatSessionEntity session : sessions) {
            session.getCourseMaterials().removeIf(kbEntity -> kbEntity.getId().equals(id));
            sessionRepository.save(session);
            log.debug("已从会话中移除知识库关联: sessionId={}, kbId={}", session.getId(), id);
        }
        if (!sessions.isEmpty()) {
            log.info("已从 {} 个会话中移除知识库关联: kbId={}", sessions.size(), id);
        }
        
        // 3. 删除向量数据
        try {
            vectorService.deleteByCourseMaterialId(id);
        } catch (Exception e) {
            log.warn("删除向量数据失败，继续删除知识库: kbId={}, error={}", id, e.getMessage());
        }
        
        // 4. 删除RustFS中的文件（FileStorageService 已内置存在性检查）
        try {
            storageService.deleteCourseMaterial(kb.getStorageKey());
        } catch (Exception e) {
            log.warn("删除RustFS文件失败，继续删除知识库记录: kbId={}, error={}", id, e.getMessage());
        }
        
        // 5. 删除知识库记录（在事务中）
        knowledgeBaseRepository.deleteById(id);
        log.info("知识库已删除: id={}", id);
    }
}

