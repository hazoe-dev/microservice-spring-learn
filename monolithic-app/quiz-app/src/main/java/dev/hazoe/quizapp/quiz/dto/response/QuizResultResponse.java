package dev.hazoe.quizapp.quiz.dto.response;

public record QuizResultResponse(
        Long quizId,
        int totalQuestions,
        int correctAnswers,
        double core
) {
}
