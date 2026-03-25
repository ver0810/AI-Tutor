package edu.aitutor.modules.student.service;

import edu.aitutor.common.ai.StructuredOutputInvoker;
import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse.ScoreDetail;
import edu.aitutor.modules.tutoring.model.StudentProfileAnalysisResponse.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历评分服务
 * 使用Spring AI调用LLM对简历进行评分和建议
 */
@Service
public class StudentProfileAnalysisService {
    
    private static final Logger log = LoggerFactory.getLogger(StudentProfileAnalysisService.class);
    
    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<StudentProfileAnalysisResponseDTO> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;
    
    // 中间DTO用于接收AI响应
    private record StudentProfileAnalysisResponseDTO(
        int overallScore,
        ScoreDetailDTO scoreDetail,
        String summary,
        List<String> strengths,
        List<SuggestionDTO> suggestions
    ) {}
    
    private record ScoreDetailDTO(
        int contentScore,
        int structureScore,
        int skillMatchScore,
        int expressionScore,
        int projectScore
    ) {}
    
    private record SuggestionDTO(
        String category,
        String priority,
        String issue,
        String recommendation
    ) {}
    
    public StudentProfileAnalysisService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker structuredOutputInvoker,
            @Value("classpath:prompts/student-profile-analysis-system.st") Resource systemPromptResource,
            @Value("classpath:prompts/student-profile-analysis-user.st") Resource userPromptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(StudentProfileAnalysisResponseDTO.class);
    }
    
    /**
     * 分析学生资料并返回评分和建议
     *
     * @param studentProfileText 学生资料文本内容
     * @return 分析结果
     */
    public StudentProfileAnalysisResponse analyzeStudentProfile(String studentProfileText) {
        log.info("开始分析学生资料，文本长度: {} 字符", studentProfileText.length());

        try {
            // 加载系统提示词
            String systemPrompt = systemPromptTemplate.render();

            // 加载用户提示词并填充变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("content", studentProfileText);
            String userPrompt = userPromptTemplate.render(variables);

            // 添加格式指令到系统提示词
            String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();

            // 调用AI
            StudentProfileAnalysisResponseDTO dto;
            try {
                dto = structuredOutputInvoker.invoke(
                    chatClient,
                    systemPromptWithFormat,
                    userPrompt,
                    outputConverter,
                    ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED,
                    "资料分析失败：",
                    "资料分析",
                    log
                );
                log.debug("AI响应解析成功: overallScore={}", dto.overallScore());
            } catch (Exception e) {
                log.error("资料分析AI调用失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.STUDENT_PROFILE_ANALYSIS_FAILED, "资料分析失败：" + e.getMessage());
            }

            // 转换为业务对象
            StudentProfileAnalysisResponse result = convertToResponse(dto, studentProfileText);
            log.info("资料分析完成，总分: {}", result.overallScore());            
            return result;
            
        } catch (Exception e) {
            log.error("简历分析失败: {}", e.getMessage(), e);
            return createErrorResponse(studentProfileText, e.getMessage());
        }
    }
    
    /**
     * 转换DTO为业务对象
     */
    private StudentProfileAnalysisResponse convertToResponse(StudentProfileAnalysisResponseDTO dto, String originalText) {
        ScoreDetail scoreDetail = new ScoreDetail(
            dto.scoreDetail().contentScore(),
            dto.scoreDetail().structureScore(),
            dto.scoreDetail().skillMatchScore(),
            dto.scoreDetail().expressionScore(),
            dto.scoreDetail().projectScore()
        );
        
        List<Suggestion> suggestions = dto.suggestions().stream()
            .map(s -> new Suggestion(s.category(), s.priority(), s.issue(), s.recommendation()))
            .toList();
        
        return new StudentProfileAnalysisResponse(
            dto.overallScore(),
            scoreDetail,
            dto.summary(),
            dto.strengths(),
            suggestions,
            originalText
        );
    }
    
    /**
     * 创建错误响应
     */
    private StudentProfileAnalysisResponse createErrorResponse(String originalText, String errorMessage) {
        return new StudentProfileAnalysisResponse(
            0,
            new ScoreDetail(0, 0, 0, 0, 0),
            "分析过程中出现错误: " + errorMessage,
            List.of(),
            List.of(new Suggestion(
                "系统",
                "高",
                "AI分析服务暂时不可用",
                "请稍后重试，或检查AI服务是否正常运行"
            )),
            originalText
        );
    }
}
