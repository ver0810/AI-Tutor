package edu.aitutor.modules.tutoring.service;

import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.common.model.AsyncTaskStatus;
import edu.aitutor.modules.tutoring.model.TutoringAnswerEntity;
import edu.aitutor.modules.tutoring.model.TutoringQuestionDTO;
import edu.aitutor.modules.tutoring.model.TutoringReportDTO;
import edu.aitutor.modules.tutoring.model.TutoringSessionEntity;
import edu.aitutor.modules.tutoring.repository.TutoringAnswerRepository;
import edu.aitutor.modules.tutoring.repository.TutoringSessionRepository;
import edu.aitutor.modules.student.model.StudentProfileEntity;
import edu.aitutor.modules.student.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 面试持久化服务
 * 面试会话和答案的持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TutoringPersistenceService {
    
    private final TutoringSessionRepository sessionRepository;
    private final TutoringAnswerRepository answerRepository;
    private final StudentProfileRepository resumeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 保存新的面试会话
     */
    @Transactional(rollbackFor = Exception.class)
    public TutoringSessionEntity saveSession(String sessionId, Long studentProfileId, 
                                              int totalQuestions, 
                                              List<TutoringQuestionDTO> questions) {
        try {
            Optional<StudentProfileEntity> resumeOpt = resumeRepository.findById(studentProfileId);
            if (resumeOpt.isEmpty()) {
                throw new BusinessException(ErrorCode.STUDENT_PROFILE_NOT_FOUND);
            }
            
            TutoringSessionEntity session = new TutoringSessionEntity();
            session.setSessionId(sessionId);
            session.setStudentProfile(resumeOpt.get());
            session.setTotalQuestions(totalQuestions);
            session.setCurrentQuestionIndex(0);
            session.setStatus(TutoringSessionEntity.SessionStatus.CREATED);
            session.setQuestionsJson(objectMapper.writeValueAsString(questions));
            
            TutoringSessionEntity saved = sessionRepository.save(session);
            log.info("面试会话已保存: sessionId={}, studentProfileId={}", sessionId, studentProfileId);
            
            return saved;
        } catch (JacksonException e) {
            log.error("序列化问题列表失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存会话失败");
        }
    }
    
    /**
     * 更新会话状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionStatus(String sessionId, TutoringSessionEntity.SessionStatus status) {
        Optional<TutoringSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            TutoringSessionEntity session = sessionOpt.get();
            session.setStatus(status);
            if (status == TutoringSessionEntity.SessionStatus.COMPLETED ||
                status == TutoringSessionEntity.SessionStatus.EVALUATED) {
                session.setCompletedAt(LocalDateTime.now());
            }
            sessionRepository.save(session);
        }
    }

    /**
     * 更新评估状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateEvaluateStatus(String sessionId, AsyncTaskStatus status, String error) {
        Optional<TutoringSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            TutoringSessionEntity session = sessionOpt.get();
            session.setEvaluateStatus(status);
            if (error != null) {
                session.setEvaluateError(error.length() > 500 ? error.substring(0, 500) : error);
            } else {
                session.setEvaluateError(null);
            }
            sessionRepository.save(session);
            log.debug("评估状态已更新: sessionId={}, status={}", sessionId, status);
        }
    }
    
    /**
     * 更新当前问题索引
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentQuestionIndex(String sessionId, int index) {
        Optional<TutoringSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            TutoringSessionEntity session = sessionOpt.get();
            session.setCurrentQuestionIndex(index);
            session.setStatus(TutoringSessionEntity.SessionStatus.IN_PROGRESS);
            sessionRepository.save(session);
        }
    }
    
    /**
     * 保存面试答案
     */
    @Transactional(rollbackFor = Exception.class)
    public TutoringAnswerEntity saveAnswer(String sessionId, int questionIndex,
                                            String question, String category,
                                            String userAnswer, int score, String feedback) {
        Optional<TutoringSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.TUTORING_SESSION_NOT_FOUND);
        }
        
        TutoringAnswerEntity answer = new TutoringAnswerEntity();
        answer.setSession(sessionOpt.get());
        answer.setQuestionIndex(questionIndex);
        answer.setQuestion(question);
        answer.setCategory(category);
        answer.setUserAnswer(userAnswer);
        answer.setScore(score);
        answer.setFeedback(feedback);
        
        TutoringAnswerEntity saved = answerRepository.save(answer);
        log.info("面试答案已保存: sessionId={}, questionIndex={}, score={}", 
                sessionId, questionIndex, score);
        
        return saved;
    }
    
    /**
     * 保存面试报告
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveReport(String sessionId, TutoringReportDTO report) {
        try {
            Optional<TutoringSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
            if (sessionOpt.isEmpty()) {
                log.warn("会话不存在: {}", sessionId);
                return;
            }

            TutoringSessionEntity session = sessionOpt.get();
            session.setOverallScore(report.overallScore());
            session.setOverallFeedback(report.overallFeedback());
            session.setStrengthsJson(objectMapper.writeValueAsString(report.strengths()));
            session.setImprovementsJson(objectMapper.writeValueAsString(report.improvements()));
            session.setReferenceAnswersJson(objectMapper.writeValueAsString(report.referenceAnswers()));
            session.setStatus(TutoringSessionEntity.SessionStatus.EVALUATED);
            session.setCompletedAt(LocalDateTime.now());

            sessionRepository.save(session);

            // 查询已存在的答案，建立索引
            List<TutoringAnswerEntity> existingAnswers = answerRepository.findBySessionSessionIdOrderByQuestionIndex(sessionId);
            java.util.Map<Integer, TutoringAnswerEntity> answerMap = existingAnswers.stream()
                .collect(java.util.stream.Collectors.toMap(
                    TutoringAnswerEntity::getQuestionIndex,
                    a -> a,
                    (a1, a2) -> a1
                ));

            // 建立参考答案索引
            java.util.Map<Integer, TutoringReportDTO.ReferenceAnswer> refAnswerMap = report.referenceAnswers().stream()
                .collect(java.util.stream.Collectors.toMap(
                    TutoringReportDTO.ReferenceAnswer::questionIndex,
                    r -> r,
                    (r1, r2) -> r1
                ));

            List<TutoringAnswerEntity> answersToSave = new java.util.ArrayList<>();

            // 遍历所有评估结果，更新或创建答案记录
            for (TutoringReportDTO.QuestionEvaluation eval : report.questionDetails()) {
                TutoringAnswerEntity answer = answerMap.get(eval.questionIndex());

                if (answer == null) {
                    // 未回答的题目，创建新记录
                    answer = new TutoringAnswerEntity();
                    answer.setSession(session);
                    answer.setQuestionIndex(eval.questionIndex());
                    answer.setQuestion(eval.question());
                    answer.setCategory(eval.category());
                    answer.setUserAnswer(null);  // 未回答
                    log.debug("为未回答的题目 {} 创建答案记录", eval.questionIndex());
                }

                // 更新评分和反馈
                answer.setScore(eval.score());
                answer.setFeedback(eval.feedback());

                // 设置参考答案和关键点
                TutoringReportDTO.ReferenceAnswer refAns = refAnswerMap.get(eval.questionIndex());
                if (refAns != null) {
                    answer.setReferenceAnswer(refAns.referenceAnswer());
                    if (refAns.keyPoints() != null && !refAns.keyPoints().isEmpty()) {
                        answer.setKeyPointsJson(objectMapper.writeValueAsString(refAns.keyPoints()));
                    }
                }

                answersToSave.add(answer);
            }

            answerRepository.saveAll(answersToSave);
            log.info("面试报告已保存: sessionId={}, score={}, 答案数={}",
                sessionId, report.overallScore(), answersToSave.size());

        } catch (JacksonException e) {
            log.error("序列化报告失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 根据会话ID获取会话
     */
    public Optional<TutoringSessionEntity> findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }
    
    /**
     * 获取简历的所有面试记录
     */
    public List<TutoringSessionEntity> findByStudentProfileId(Long studentProfileId) {
        return sessionRepository.findByStudentProfileIdOrderByCreatedAtDesc(studentProfileId);
    }
    
    /**
     * 删除简历的所有面试会话
     * 由于TutoringSessionEntity设置了cascade = CascadeType.ALL, orphanRemoval = true
     * 删除会话会自动删除关联的答案
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionsByStudentProfileId(Long studentProfileId) {
        List<TutoringSessionEntity> sessions = sessionRepository.findByStudentProfileIdOrderByCreatedAtDesc(studentProfileId);
        if (!sessions.isEmpty()) {
            sessionRepository.deleteAll(sessions);
            log.info("已删除 {} 个面试会话（包含所有答案）", sessions.size());
        }
    }
    
    /**
     * 删除单个面试会话
     * 由于TutoringSessionEntity设置了cascade = CascadeType.ALL, orphanRemoval = true
     * 删除会话会自动删除关联的答案
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionBySessionId(String sessionId) {
        Optional<TutoringSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            sessionRepository.delete(sessionOpt.get());
            log.info("已删除面试会话: sessionId={}", sessionId);
        } else {
            throw new BusinessException(ErrorCode.TUTORING_SESSION_NOT_FOUND);
        }
    }
    
    /**
     * 查找未完成的面试会话（CREATED或IN_PROGRESS状态）
     */
    public Optional<TutoringSessionEntity> findUnfinishedSession(Long studentProfileId) {
        List<TutoringSessionEntity.SessionStatus> unfinishedStatuses = List.of(
            TutoringSessionEntity.SessionStatus.CREATED,
            TutoringSessionEntity.SessionStatus.IN_PROGRESS
        );
        return sessionRepository.findFirstByStudentProfileIdAndStatusInOrderByCreatedAtDesc(studentProfileId, unfinishedStatuses);
    }
    
    /**
     * 根据会话ID查找所有答案
     */
    public List<TutoringAnswerEntity> findAnswersBySessionId(String sessionId) {
        return answerRepository.findBySessionSessionIdOrderByQuestionIndex(sessionId);
    }

    /**
     * 获取简历的历史提问列表（限制最近的 N 条）
     */
    public List<String> getHistoricalQuestionsByResumeId(Long studentProfileId) {
        // 限制只查询最近的 10 个会话，避免加载过多数据
        List<TutoringSessionEntity> sessions = sessionRepository.findByStudentProfileIdOrderByCreatedAtDesc(studentProfileId);
        
        return sessions.stream()
            .map(TutoringSessionEntity::getQuestionsJson)
            .filter(json -> json != null && !json.isEmpty())
            .flatMap(json -> {
                try {
                    List<TutoringQuestionDTO> questions = objectMapper.readValue(json, 
                        new TypeReference<List<TutoringQuestionDTO>>() {});
                    return questions.stream().map(TutoringQuestionDTO::question);
                } catch (Exception e) {
                    log.error("解析历史问题JSON失败", e);
                    return java.util.stream.Stream.empty();
                }
            })
            .distinct()
            .limit(30) // 核心改动：只保留最近的 30 道题
            .toList();
    }
}
