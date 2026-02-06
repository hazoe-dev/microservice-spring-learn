package dev.hazoe.quizapp.quiz.dto;

import dev.hazoe.quizapp.question.domain.QuestionLevel;

import java.util.List;

public record QuizQuestionResponse(
        Long id,
        String title,
        List<String> options,
        QuestionLevel level
) {
}
