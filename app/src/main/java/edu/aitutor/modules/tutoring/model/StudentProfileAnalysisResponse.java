package edu.aitutor.modules.tutoring.model;

import java.util.List;

/**
 * 学生档案/简历分析响应DTO
 */
public record StudentProfileAnalysisResponse(
    // 核心总结
    String summary,
    
    // 知识点/技能标签
    List<String> tags,
    
    // 推荐学习路径
    List<LearningStep> learningPath,
    
    // 综合评分 (0-100)
    Integer overallScore,
    
    // 难度评估 (1-5)
    Integer difficulty,
    
    // 原始文本片段
    String originalText
) {
    /**
     * 学习步骤建议
     */
    public record LearningStep(
        Integer step,
        String title,
        String description
    ) {}
}
