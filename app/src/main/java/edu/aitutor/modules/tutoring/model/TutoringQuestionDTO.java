package edu.aitutor.modules.tutoring.model;

/**
 * 测验问题DTO (通用化设计)
 */
public record TutoringQuestionDTO(
    int questionIndex,
    String question,
    QuestionType type,
    String category,      // 题目类别：基于资料内容自动生成，不再写死
    String userAnswer,    // 用户回答
    Integer score,        // 单题得分 (0-100)
    String feedback       // 单题反馈
) {
    public enum QuestionType {
        CONCEPT,          // 基础概念
        PRACTICE,         // 实践应用
        ANALYSIS,         // 深度分析
        SCENARIO,         // 场景模拟
        OTHER             // 其他
    }
    
    /**
     * 创建新问题（未回答状态）
     */
    public static TutoringQuestionDTO create(int index, String question, QuestionType type, String category) {
        return new TutoringQuestionDTO(index, question, type, category, null, null, null);
    }
    
    /**
     * 添加用户回答
     */
    public TutoringQuestionDTO withAnswer(String answer) {
        return new TutoringQuestionDTO(questionIndex, question, type, category, answer, score, feedback);
    }
    
    /**
     * 添加评分和反馈
     */
    public TutoringQuestionDTO withEvaluation(int score, String feedback) {
        return new TutoringQuestionDTO(questionIndex, question, type, category, userAnswer, score, feedback);
    }
}
