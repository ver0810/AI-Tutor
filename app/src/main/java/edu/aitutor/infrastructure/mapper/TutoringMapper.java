package edu.aitutor.infrastructure.mapper;

import edu.aitutor.modules.tutoring.model.TutoringAnswerEntity;
import edu.aitutor.modules.tutoring.model.TutoringDetailDTO;
import edu.aitutor.modules.tutoring.model.TutoringReportDTO;
import edu.aitutor.modules.tutoring.model.TutoringSessionEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * 面试相关的对象映射器
 * 使用MapStruct自动生成转换代码
 * <p>
 * 注意：JSON字段需要在Service层手动处理
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TutoringMapper {

    // ========== QuestionEvaluation 映射 ==========

    /**
     * 将面试答案实体转换为问题评估详情
     */
    @Mapping(target = "questionIndex", source = "questionIndex", qualifiedByName = "nullIndexToZero")
    @Mapping(target = "question", source = "question")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "userAnswer", source = "userAnswer")
    @Mapping(target = "score", source = "score", qualifiedByName = "nullScoreToZero")
    @Mapping(target = "feedback", source = "feedback")
    TutoringReportDTO.QuestionEvaluation toQuestionEvaluation(TutoringAnswerEntity entity);

    /**
     * 批量转换面试答案实体
     */
    List<TutoringReportDTO.QuestionEvaluation> toQuestionEvaluations(List<TutoringAnswerEntity> entities);

    // ========== AnswerDetailDTO 映射 ==========

    /**
     * TutoringAnswerEntity 转换为 AnswerDetailDTO
     * 注意：keyPoints 需要从 JSON 解析后传入
     */
    @Mapping(target = "keyPoints", source = "keyPoints")
    TutoringDetailDTO.AnswerDetailDTO toAnswerDetailDTO(
        TutoringAnswerEntity entity,
        List<String> keyPoints
    );

    /**
     * 批量转换（需要在 Service 层处理 JSON）
     */
    default List<TutoringDetailDTO.AnswerDetailDTO> toAnswerDetailDTOList(
        List<TutoringAnswerEntity> entities,
        java.util.function.Function<TutoringAnswerEntity, List<String>> keyPointsExtractor
    ) {
        return entities.stream()
            .map(e -> toAnswerDetailDTO(e, keyPointsExtractor.apply(e)))
            .toList();
    }

    // ========== TutoringDetailDTO 映射 ==========

    /**
     * TutoringSessionEntity 转换为 TutoringDetailDTO
     * 注意：questions, strengths, improvements, referenceAnswers, answers 需要在 Service 层处理
     */
    @Mapping(target = "status", expression = "java(session.getStatus().toString())")
    @Mapping(target = "evaluateStatus", expression = "java(session.getEvaluateStatus() != null ? session.getEvaluateStatus().name() : null)")
    @Mapping(target = "evaluateError", source = "session.evaluateError")
    @Mapping(target = "questions", source = "questions")
    @Mapping(target = "strengths", source = "strengths")
    @Mapping(target = "improvements", source = "improvements")
    @Mapping(target = "referenceAnswers", source = "referenceAnswers")
    @Mapping(target = "answers", source = "answers")
    TutoringDetailDTO toDetailDTO(
        TutoringSessionEntity session,
        List<Object> questions,
        List<String> strengths,
        List<String> improvements,
        List<Object> referenceAnswers,
        List<TutoringDetailDTO.AnswerDetailDTO> answers
    );

    // ========== TutoringSessionEntity 更新映射 ==========

    /**
     * 从 TutoringReportDTO 更新 TutoringSessionEntity
     * 注意：JSON 字段需要在 Service 层单独设置
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "studentProfile", ignore = true)
    @Mapping(target = "totalQuestions", ignore = true)
    @Mapping(target = "currentQuestionIndex", ignore = true)
    @Mapping(target = "questionsJson", ignore = true)
    @Mapping(target = "strengthsJson", ignore = true)
    @Mapping(target = "improvementsJson", ignore = true)
    @Mapping(target = "referenceAnswersJson", ignore = true)
    @Mapping(target = "answers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    void updateSessionFromReport(TutoringReportDTO report, @MappingTarget TutoringSessionEntity session);

    // ========== 面试历史列表项映射 ==========

    /**
     * TutoringSessionEntity 转换为简要信息 Map
     * 用于 StudentProfileDetailDTO 中的面试历史列表
     */
    default java.util.Map<String, Object> toAitutorHistoryItem(TutoringSessionEntity session) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", session.getId());
        map.put("sessionId", session.getSessionId());
        map.put("totalQuestions", session.getTotalQuestions());
        map.put("status", session.getStatus().toString());
        map.put("evaluateStatus", session.getEvaluateStatus() != null ? session.getEvaluateStatus().name() : null);
        map.put("evaluateError", session.getEvaluateError());
        map.put("overallScore", session.getOverallScore());
        map.put("createdAt", session.getCreatedAt());
        map.put("completedAt", session.getCompletedAt());
        return map;
    }

    /**
     * 批量转换面试历史
     */
    default List<Object> toAitutorHistoryList(List<TutoringSessionEntity> sessions) {
        return sessions.stream()
            .map(this::toAitutorHistoryItem)
            .map(m -> (Object) m)
            .toList();
    }

    // ========== 工具方法 ==========

    @Named("nullIndexToZero")
    default int nullIndexToZero(Integer value) {
        return value != null ? value : 0;
    }

    @Named("nullScoreToZero")
    default int nullScoreToZero(Integer value) {
        return value != null ? value : 0;
    }
}
