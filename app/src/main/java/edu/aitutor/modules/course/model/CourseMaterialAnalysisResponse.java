package edu.aitutor.modules.course.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 课程资料核心分析响应 (完全适配前端字段名)
 */
public record CourseMaterialAnalysisResponse(
    @JsonProperty("summary") String summary,
    @JsonProperty("tags") List<String> tags,
    @JsonProperty("learningPath") List<LearningStep> learningPath,
    @JsonProperty("difficulty") Integer difficulty
) {
    public record LearningStep(
        @JsonProperty("step") Integer step,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description
    ) {}
}
