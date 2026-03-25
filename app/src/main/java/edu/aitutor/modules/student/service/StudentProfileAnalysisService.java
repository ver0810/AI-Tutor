package edu.aitutor.modules.student.service;

import edu.aitutor.common.ai.StructuredOutputInvoker;
import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse.LearningStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 简历分析服务 (适配高校学习业务)
 */
@Service
public class StudentProfileAnalysisService {
    
    private static final Logger log = LoggerFactory.getLogger(StudentProfileAnalysisService.class);
    
    private final ChatClient chatClient;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final BeanOutputConverter<StudentProfileAnalysisResponseDTO> outputConverter;

    @Value("classpath:prompts/student-profile-analysis-system.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/student-profile-analysis-user.st")
    private Resource userPromptResource;

    // AI 响应的内部 DTO
    public record StudentProfileAnalysisResponseDTO(
        String summary,
        List<String> tags,
        List<LearningStepDTO> learningPath,
        Integer overallScore,
        Integer difficulty
    ) {
        public record LearningStepDTO(
            Integer step,
            String title,
            String description
        ) {}
    }
    
    public StudentProfileAnalysisService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker structuredOutputInvoker) {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.outputConverter = new BeanOutputConverter<>(StudentProfileAnalysisResponseDTO.class);
    }
    
    public StudentProfileAnalysisResponse analyzeStudentProfile(String studentProfileText) {
        log.info("开始分析学生档案，长度: {}", studentProfileText.length());

        try {
            String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            String userPromptTemplate = userPromptResource.getContentAsString(StandardCharsets.UTF_8);
            String userPrompt = userPromptTemplate.replace("{content}", studentProfileText);

            // 调用结构化输出解析器
            StudentProfileAnalysisResponseDTO dto = structuredOutputInvoker.invoke(
                chatClient,
                systemPrompt,
                userPrompt,
                outputConverter,
                ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED,
                "档案分析失败：",
                "StudentProfileAnalysis",
                log
            );

            return convertToResponse(dto, studentProfileText);
            
        } catch (Exception e) {
            log.error("档案分析失败", e);
            throw new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED, e.getMessage());
        }
    }
    
    private StudentProfileAnalysisResponse convertToResponse(StudentProfileAnalysisResponseDTO dto, String originalText) {
        List<LearningStep> steps = dto.learningPath().stream()
            .map(s -> new LearningStep(s.step(), s.title(), s.description()))
            .toList();
            
        return new StudentProfileAnalysisResponse(
            dto.summary(),
            dto.tags(),
            steps,
            dto.overallScore(),
            dto.difficulty(),
            originalText
        );
    }
}
