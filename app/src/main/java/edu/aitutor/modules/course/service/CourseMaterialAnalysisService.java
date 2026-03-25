package edu.aitutor.modules.course.service;

import edu.aitutor.common.ai.StructuredOutputInvoker;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.modules.course.model.CourseMaterialAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 课程资料智能分析服务
 */
@Slf4j
@Service
public class CourseMaterialAnalysisService {

    private final ChatClient chatClient;
    private final StructuredOutputInvoker invoker;
    private final CourseMaterialListService listService;

    @Value("classpath:prompts/course-material-analysis-system.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/course-material-analysis-user.st")
    private Resource userPromptResource;

    public CourseMaterialAnalysisService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker invoker,
            CourseMaterialListService listService) {
        this.chatClient = chatClientBuilder.build();
        this.invoker = invoker;
        this.listService = listService;
    }

    public CourseMaterialAnalysisResponse analyzeMaterial(Long id) {
        // 1. 获取资料详情
        var materialDto = listService.getCourseMaterial(id)
                .orElseThrow(() -> new IllegalArgumentException("课程资料不存在"));

        // 为了分析，我们获取资料文本预览
        String content = listService.getMaterialContentPreview(id, 4000); 

        // 2. 准备提示词模板
        BeanOutputConverter<CourseMaterialAnalysisResponse> converter = 
                new BeanOutputConverter<>(CourseMaterialAnalysisResponse.class);

        String systemPrompt;
        String userPrompt;
        try {
            systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            userPrompt = userPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("加载分析提示词失败", e);
            throw new RuntimeException("系统配置错误：无法加载分析模板");
        }

        // 3. 调用 AI 分析（利用结构化输出重试机制）
        return invoker.invoke(
                chatClient,
                systemPrompt,
                userPrompt.replace("{materialName}", materialDto.getName())
                         .replace("{content}", content),
                converter,
                ErrorCode.AI_ANALYSIS_ERROR,
                "资料分析失败：",
                "CourseMaterialAnalysis",
                log
        );
    }
}
