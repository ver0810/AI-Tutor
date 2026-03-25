package edu.aitutor.modules.tutoring.service;

import edu.aitutor.common.ai.StructuredOutputInvoker;
import edu.aitutor.common.exception.BusinessException;
import edu.aitutor.common.exception.ErrorCode;
import edu.aitutor.modules.tutoring.model.TutoringQuestionDTO;
import edu.aitutor.modules.tutoring.model.TutoringQuestionDTO.QuestionType;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TutoringQuestionService {

    private static final Logger log = LoggerFactory.getLogger(TutoringQuestionService.class);
    private static final Set<String> INVALID_QUESTION_KEYWORDS = Set.of(
        "简历", "面试官", "求职", "应聘", "岗位"
    );

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<QuestionListDTO> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final int followUpCount;

    private static final double PROJECT_RATIO = 0.20;
    private static final double MYSQL_RATIO = 0.20;
    private static final double REDIS_RATIO = 0.20;
    private static final double JAVA_BASIC_RATIO = 0.10;
    private static final double JAVA_COLLECTION_RATIO = 0.10;
    private static final double JAVA_CONCURRENT_RATIO = 0.10;
    private static final int MAX_FOLLOW_UP_COUNT = 2;

    private record QuestionListDTO(List<QuestionDTO> questions) {}
    private record QuestionDTO(String question, String type, String category, List<String> followUps) {}
    private record QuestionDistribution(int project, int mysql, int redis, int javaBasic, int javaCollection, int javaConcurrent, int spring) {}

    public TutoringQuestionService(
        ChatClient.Builder chatClientBuilder,
        StructuredOutputInvoker structuredOutputInvoker,
        @Value("classpath:prompts/tutor-qa-system.st") Resource systemPromptResource,
        @Value("classpath:prompts/tutor-qa-user.st") Resource userPromptResource,
        @Value("${app.tutor.follow-up-count:1}") int followUpCount
    ) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(QuestionListDTO.class);
        this.followUpCount = Math.max(0, Math.min(followUpCount, MAX_FOLLOW_UP_COUNT));
    }

    public List<TutoringQuestionDTO> generateQuestions(String studentProfileText, int questionCount, List<String> historicalQuestions) {
        log.info("开始生成测验问题，资料长度: {}, 题目数量: {}, 历史题目数: {}",
            studentProfileText.length(), questionCount, historicalQuestions != null ? historicalQuestions.size() : 0);

        QuestionDistribution distribution = calculateDistribution(questionCount);

        try {
            String systemPrompt = systemPromptTemplate.render();
            Map<String, Object> variables = buildPromptVariables(studentProfileText, questionCount, historicalQuestions, distribution);
            String userPrompt = userPromptTemplate.render(variables);
            String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();

            QuestionListDTO dto = structuredOutputInvoker.invoke(
                chatClient,
                systemPromptWithFormat,
                userPrompt,
                outputConverter,
                ErrorCode.TUTORING_QUESTION_GENERATION_FAILED,
                "测验问题生成失败：",
                "结构化问题生成",
                log
            );

            List<TutoringQuestionDTO> aiQuestions = normalizeQuestions(convertToQuestions(dto), questionCount);
            if (aiQuestions.size() < questionCount) {
                log.warn("AI题目有效数量不足，降级补齐默认题：有效={}，目标={}", aiQuestions.size(), questionCount);
                return fillWithDefaultQuestions(aiQuestions, questionCount);
            }
            return aiQuestions;
        } catch (Exception e) {
            log.error("生成测验问题失败，降级默认题：{}", e.getMessage(), e);
            return generateDefaultQuestions(questionCount);
        }
    }

    public List<TutoringQuestionDTO> generateQuestions(String studentProfileText, int questionCount) {
        return generateQuestions(studentProfileText, questionCount, null);
    }

    private Map<String, Object> buildPromptVariables(String studentProfileText, int questionCount,
                                                     List<String> historicalQuestions,
                                                     QuestionDistribution distribution) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("question", "请基于课程资料生成测验题");
        variables.put("context", studentProfileText);
        variables.put("questionCount", questionCount);
        variables.put("projectCount", distribution.project());
        variables.put("mysqlCount", distribution.mysql());
        variables.put("redisCount", distribution.redis());
        variables.put("javaBasicCount", distribution.javaBasic());
        variables.put("javaCollectionCount", distribution.javaCollection());
        variables.put("javaConcurrentCount", distribution.javaConcurrent());
        variables.put("springCount", distribution.spring());
        variables.put("followUpCount", followUpCount);
        variables.put("studentProfileText", studentProfileText);
        variables.put("historicalQuestions",
            historicalQuestions == null || historicalQuestions.isEmpty()
                ? "暂无历史提问"
                : String.join("\n", historicalQuestions));
        return variables;
    }

    private QuestionDistribution calculateDistribution(int total) {
        int project = Math.max(1, (int) Math.round(total * PROJECT_RATIO));
        int mysql = Math.max(1, (int) Math.round(total * MYSQL_RATIO));
        int redis = Math.max(1, (int) Math.round(total * REDIS_RATIO));
        int javaBasic = Math.max(1, (int) Math.round(total * JAVA_BASIC_RATIO));
        int javaCollection = (int) Math.round(total * JAVA_COLLECTION_RATIO);
        int javaConcurrent = (int) Math.round(total * JAVA_CONCURRENT_RATIO);
        int spring = Math.max(0, total - project - mysql - redis - javaBasic - javaCollection - javaConcurrent);
        return new QuestionDistribution(project, mysql, redis, javaBasic, javaCollection, javaConcurrent, spring);
    }

    private List<TutoringQuestionDTO> convertToQuestions(QuestionListDTO dto) {
        if (dto == null || dto.questions() == null) {
            return List.of();
        }
        List<TutoringQuestionDTO> questions = new ArrayList<>();
        int index = 0;
        for (QuestionDTO q : dto.questions()) {
            if (q == null || q.question() == null || q.question().isBlank()) {
                continue;
            }
            QuestionType type = parseQuestionType(q.type());
            questions.add(TutoringQuestionDTO.create(index++, q.question().trim(), type, normalizeCategory(q.category(), type)));

            List<String> followUps = sanitizeFollowUps(q.followUps());
            for (int i = 0; i < followUps.size(); i++) {
                questions.add(TutoringQuestionDTO.create(
                    index++,
                    followUps.get(i),
                    type,
                    buildFollowUpCategory(normalizeCategory(q.category(), type), i + 1)
                ));
            }
        }
        return questions;
    }

    private QuestionType parseQuestionType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return QuestionType.JAVA_BASIC;
        }
        try {
            return QuestionType.valueOf(typeStr.trim().toUpperCase());
        } catch (Exception e) {
            return QuestionType.JAVA_BASIC;
        }
    }

    private String normalizeCategory(String category, QuestionType type) {
        if (category != null && !category.isBlank()) {
            return category.trim();
        }
        return switch (type) {
            case PROJECT -> "项目经历";
            case MYSQL -> "MySQL";
            case REDIS -> "Redis";
            case JAVA_COLLECTION -> "Java集合";
            case JAVA_CONCURRENT -> "Java并发";
            case SPRING, SPRING_BOOT -> "Spring";
            default -> "Java基础";
        };
    }

    private List<String> sanitizeFollowUps(List<String> followUps) {
        if (followUpCount == 0 || followUps == null || followUps.isEmpty()) {
            return List.of();
        }
        return followUps.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .limit(followUpCount)
            .toList();
    }

    private List<TutoringQuestionDTO> normalizeQuestions(List<TutoringQuestionDTO> generated, int questionCount) {
        List<TutoringQuestionDTO> normalized = generated.stream()
            .filter(this::isQuestionValid)
            .limit(questionCount)
            .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < normalized.size(); i++) {
            TutoringQuestionDTO q = normalized.get(i);
            normalized.set(i, TutoringQuestionDTO.create(i, q.question(), q.type(), q.category()));
        }
        return normalized;
    }

    private boolean isQuestionValid(TutoringQuestionDTO q) {
        if (q == null || q.question() == null || q.question().isBlank()) {
            return false;
        }
        String text = q.question().trim();
        if (text.length() < 8) {
            return false;
        }
        return INVALID_QUESTION_KEYWORDS.stream().noneMatch(text::contains);
    }

    private List<TutoringQuestionDTO> fillWithDefaultQuestions(List<TutoringQuestionDTO> existing, int targetCount) {
        List<TutoringQuestionDTO> result = new ArrayList<>(existing);
        List<TutoringQuestionDTO> fallback = generateDefaultQuestions(targetCount);
        int cursor = 0;
        while (result.size() < targetCount && cursor < fallback.size()) {
            TutoringQuestionDTO candidate = fallback.get(cursor++);
            result.add(TutoringQuestionDTO.create(result.size(), candidate.question(), candidate.type(), candidate.category()));
        }
        return result;
    }

    private List<TutoringQuestionDTO> generateDefaultQuestions(int count) {
        List<TutoringQuestionDTO> questions = new ArrayList<>();
        String[][] defaultQuestions = {
            {"请总结该课程资料中最关键的三个知识点，并说明它们的联系。", "JAVA_BASIC", "核心概念"},
            {"结合资料内容，解释一个你认为最容易混淆的概念，并举例说明。", "JAVA_BASIC", "概念辨析"},
            {"如果要把这份资料教给新同学，你会如何组织讲解顺序？", "PROJECT", "学习路径"},
            {"请从资料中挑选一个技术点，说明其在真实项目中的应用场景。", "PROJECT", "实践应用"},
            {"资料中涉及的数据存储设计里，哪些部分可以用 MySQL 优化？", "MYSQL", "MySQL"},
            {"如果该资料主题用于高并发场景，Redis 可以承担什么角色？", "REDIS", "Redis"},
            {"请比较资料中两个相关技术方案的优缺点，并给出选择依据。", "JAVA_COLLECTION", "方案对比"},
            {"针对资料中一个核心流程，给出异常排查步骤。", "JAVA_CONCURRENT", "问题定位"},
            {"请给出一条可执行的复习计划，说明每天的学习目标。", "SPRING", "复习计划"},
            {"如果要设计一套测验来检验资料掌握度，你会出哪些题型？", "SPRING_BOOT", "学习评估"}
        };

        int index = 0;
        for (int i = 0; i < Math.min(count, defaultQuestions.length); i++) {
            String mainQuestion = defaultQuestions[i][0];
            QuestionType type = QuestionType.valueOf(defaultQuestions[i][1]);
            String category = defaultQuestions[i][2];
            questions.add(TutoringQuestionDTO.create(index++, mainQuestion, type, category));
            for (int j = 0; j < followUpCount && index < count; j++) {
                questions.add(TutoringQuestionDTO.create(index++, buildDefaultFollowUp(mainQuestion, j + 1), type, buildFollowUpCategory(category, j + 1)));
            }
        }
        return questions.stream().limit(count).toList();
    }

    private String buildFollowUpCategory(String category, int order) {
        String baseCategory = (category == null || category.isBlank()) ? "追问" : category;
        return baseCategory + "（追问" + order + "）";
    }

    private String buildDefaultFollowUp(String mainQuestion, int order) {
        if (order == 1) {
            return "围绕“" + mainQuestion + "”，请结合课程资料给出一个更具体的例子。";
        }
        return "围绕“" + mainQuestion + "”，如果要在考试中拿高分，你会如何组织回答结构？";
    }
}
