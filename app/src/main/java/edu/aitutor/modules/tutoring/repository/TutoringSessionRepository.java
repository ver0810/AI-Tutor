package edu.aitutor.modules.tutoring.repository;

import edu.aitutor.modules.tutoring.model.TutoringSessionEntity;
import edu.aitutor.modules.tutoring.model.TutoringSessionEntity.SessionStatus;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 面试会话Repository
 */
@Repository
public interface TutoringSessionRepository extends JpaRepository<TutoringSessionEntity, Long> {

    /**
     * 根据会话ID查找
     */
    Optional<TutoringSessionEntity> findBySessionId(String sessionId);

    /**
     * 根据会话ID查找（同时加载关联的简历）
     */
    @Query("SELECT s FROM TutoringSessionEntity s JOIN FETCH s.studentProfile WHERE s.sessionId = :sessionId")
    Optional<TutoringSessionEntity> findBySessionIdWithStudentProfile(@Param("sessionId") String sessionId);
    
    /**
     * 根据简历查找所有面试记录
     */
    List<TutoringSessionEntity> findByStudentProfileOrderByCreatedAtDesc(StudentProfileEntity resume);
    
    /**
     * 根据简历ID查找所有面试记录
     */
    List<TutoringSessionEntity> findByStudentProfileIdOrderByCreatedAtDesc(Long studentProfileId);
    
    /**
     * 查找简历的未完成面试（CREATED或IN_PROGRESS状态）
     */
    Optional<TutoringSessionEntity> findFirstByStudentProfileIdAndStatusInOrderByCreatedAtDesc(
        Long studentProfileId, 
        List<SessionStatus> statuses
    );
    
    /**
     * 根据简历ID和状态查找会话
     */
    Optional<TutoringSessionEntity> findByStudentProfileIdAndStatusIn(
        Long studentProfileId,
        List<SessionStatus> statuses
    );

    /**
     * 根据课程ID查找所有考试记录
     */
    List<TutoringSessionEntity> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    /**
     * 根据课程ID和状态查找
     */
    List<TutoringSessionEntity> findByCourseIdAndStatusInOrderByCreatedAtDesc(
            Long courseId,
            List<SessionStatus> statuses
    );
}
