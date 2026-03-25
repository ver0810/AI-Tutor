package edu.aitutor.modules.tutoring.repository;

import edu.aitutor.modules.tutoring.model.TutoringAnswerEntity;
import edu.aitutor.modules.tutoring.model.TutoringSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 面试答案Repository
 */
@Repository
public interface TutoringAnswerRepository extends JpaRepository<TutoringAnswerEntity, Long> {
    
    /**
     * 根据会话查找所有答案
     */
    List<TutoringAnswerEntity> findBySessionOrderByQuestionIndex(TutoringSessionEntity session);
    
    /**
     * 根据会话ID查找所有答案
     */
    List<TutoringAnswerEntity> findBySessionIdOrderByQuestionIndex(Long sessionId);
    
    /**
     * 根据会话 sessionId 字符串查找所有答案
     */
    @Query("SELECT a FROM TutoringAnswerEntity a WHERE a.session.sessionId = :sessionId ORDER BY a.questionIndex")
    List<TutoringAnswerEntity> findBySessionSessionIdOrderByQuestionIndex(@Param("sessionId") String sessionId);
}
