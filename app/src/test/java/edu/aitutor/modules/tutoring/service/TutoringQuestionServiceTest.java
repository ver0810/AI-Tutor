package edu.aitutor.modules.tutoring.service;

import edu.aitutor.common.ai.StructuredOutputInvoker;
import edu.aitutor.modules.tutoring.model.TutoringQuestionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TutoringQuestionServiceTest {

    private TutoringQuestionService service;

    @BeforeEach
    void setUp() throws Exception {
        ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
        ChatClient chatClient = Mockito.mock(ChatClient.class);
        StructuredOutputInvoker invoker = Mockito.mock(StructuredOutputInvoker.class);
        when(builder.build()).thenReturn(chatClient);

        Resource systemPrompt = new ByteArrayResource("system prompt".getBytes(StandardCharsets.UTF_8));
        Resource userPrompt = new ByteArrayResource(
            "question={question}\ncontext={context}\nstudentProfileText={studentProfileText}".getBytes(StandardCharsets.UTF_8)
        );

        service = new TutoringQuestionService(builder, invoker, systemPrompt, userPrompt, 1);
    }

    @Test
    @DisplayName("当 AI 生成失败时，应该降级到默认题库并返回目标题量")
    void shouldFallbackToDefaultQuestionsWhenAiFails() {
        List<TutoringQuestionDTO> questions = service.generateQuestions("课程资料内容", 6, List.of("历史问题A"));

        assertNotNull(questions);
        assertEquals(6, questions.size());
        assertTrue(questions.stream().noneMatch(q -> q.question() == null || q.question().isBlank()));
    }

    @Test
    @DisplayName("isQuestionValid: 过短问题应判定为无效")
    void shouldMarkShortQuestionInvalid() throws Exception {
        Method method = TutoringQuestionService.class.getDeclaredMethod("isQuestionValid", TutoringQuestionDTO.class);
        method.setAccessible(true);

        TutoringQuestionDTO q = TutoringQuestionDTO.create(0, "太短", TutoringQuestionDTO.QuestionType.JAVA_BASIC, "Java基础");
        boolean valid = (boolean) method.invoke(service, q);

        assertFalse(valid);
    }

    @Test
    @DisplayName("isQuestionValid: 包含旧业务关键词应判定为无效")
    void shouldMarkLegacyKeywordQuestionInvalid() throws Exception {
        Method method = TutoringQuestionService.class.getDeclaredMethod("isQuestionValid", TutoringQuestionDTO.class);
        method.setAccessible(true);

        TutoringQuestionDTO q = TutoringQuestionDTO.create(
            0,
            "请从简历项目中挑一个最有代表性的案例说明技术难点",
            TutoringQuestionDTO.QuestionType.PROJECT,
            "项目经历"
        );
        boolean valid = (boolean) method.invoke(service, q);

        assertFalse(valid);
    }

    @Test
    @DisplayName("isQuestionValid: 合法学习场景问题应判定为有效")
    void shouldAcceptLearningScenarioQuestion() throws Exception {
        Method method = TutoringQuestionService.class.getDeclaredMethod("isQuestionValid", TutoringQuestionDTO.class);
        method.setAccessible(true);

        TutoringQuestionDTO q = TutoringQuestionDTO.create(
            0,
            "请结合课程资料解释事务隔离级别在高并发场景下的取舍",
            TutoringQuestionDTO.QuestionType.MYSQL,
            "MySQL"
        );
        boolean valid = (boolean) method.invoke(service, q);

        assertTrue(valid);
    }
}
