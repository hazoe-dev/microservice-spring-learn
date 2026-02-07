package dev.hazoe.quizservice.quiz.dto.response;

import java.util.List;

public record QuestionSummaryResponse(
        Long id,
        String title,
        List<String> options,
        String level
) {}

