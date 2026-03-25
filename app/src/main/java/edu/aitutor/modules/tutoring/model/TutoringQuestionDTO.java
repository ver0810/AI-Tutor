package edu.aitutor.modules.tutoring.model;

/**
 * 面试问题DTO
 */
public record TutoringQuestionDTO(
    int questionIndex,
    String question,
    QuestionType type,
    String category,      // 问题类别：项目经历、Java基础、集合、并发、MySQL、Redis、Spring、SpringBoot
    String userAnswer,    // 用户回答
    Integer score,        // 单题得分 (0-100)
    String feedback       // 单题反馈
) {
    public enum QuestionType {
        PROJECT,          // 项目经历
        JAVA_BASIC,       // Java基础
        JAVA_COLLECTION,  // Java集合
        JAVA_CONCURRENT,  // Java并发
        MYSQL,            // MySQL
        REDIS,            // Redis
        SPRING,           // Spring
        SPRING_BOOT       // Spring Boot
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
