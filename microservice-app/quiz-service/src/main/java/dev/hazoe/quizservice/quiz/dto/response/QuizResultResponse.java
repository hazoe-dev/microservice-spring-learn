package dev.hazoe.quizservice.quiz.dto.response;

public record QuizResultResponse(
        Long quizId,
        int totalQuestions,
        int correctAnswers,
        double core
) {
}
