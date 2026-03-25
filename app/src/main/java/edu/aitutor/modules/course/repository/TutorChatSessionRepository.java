package edu.aitutor.modules.course.repository;

import edu.aitutor.modules.course.model.TutorChatSessionEntity;
import edu.aitutor.modules.course.model.TutorChatSessionEntity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 
 * RAG聊天会话Repository
 */
@Repository
public interface TutorChatSessionRepository extends JpaRepository<TutorChatSessionEntity, Long> {

    /**
     * 按更新时间倒序获取所有活跃会话
     */
    List<TutorChatSessionEntity> findByStatusOrderByUpdatedAtDesc(SessionStatus status);

    /**
     * 获取所有会话（按更新时间倒序）
     */
    List<TutorChatSessionEntity> findAllByOrderByUpdatedAtDesc();

    /**
     * 获取所有会话（按置顶状态和更新时间排序：置顶的在前，然后按更新时间倒序）
     */
    @Query("SELECT s FROM TutorChatSessionEntity s ORDER BY s.isPinned DESC, s.updatedAt DESC")
    List<TutorChatSessionEntity> findAllOrderByPinnedAndUpdatedAtDesc();

    /**
     * 根据知识库ID查找相关会话
     */
    @Query("SELECT DISTINCT s FROM TutorChatSessionEntity s JOIN s.courseMaterials kb WHERE kb.id IN :kbIds ORDER BY s.updatedAt DESC")
    List<TutorChatSessionEntity> findByCourseMaterialIds(@Param("kbIds") List<Long> knowledgeBaseIds);

    /**
     * 获取会话详情（带消息列表和知识库）
     * 注意：使用 DISTINCT 避免笛卡尔积导致的重复数据
     */
    @Query("SELECT DISTINCT s FROM TutorChatSessionEntity s LEFT JOIN FETCH s.courseMaterials WHERE s.id = :id")
    Optional<TutorChatSessionEntity> findByIdWithMessagesAndCourseMaterials(@Param("id") Long id);

    /**
     * 获取会话（带知识库，不带消息）
     */
    @Query("SELECT s FROM TutorChatSessionEntity s LEFT JOIN FETCH s.courseMaterials WHERE s.id = :id")
    Optional<TutorChatSessionEntity> findByIdWithCourseMaterials(@Param("id") Long id);

    /**
     * 根据课程ID查找会话
     */
    List<TutorChatSessionEntity> findByCourseIdOrderByUpdatedAtDesc(Long courseId);
}
