package edu.aitutor.modules.tutoring.model;

/**
 * 提交答案响应
 */
public record SubmitAnswerResponse(
    boolean hasNextQuestion,
    TutoringQuestionDTO nextQuestion,
    int currentIndex,
    int totalQuestions
) {}
