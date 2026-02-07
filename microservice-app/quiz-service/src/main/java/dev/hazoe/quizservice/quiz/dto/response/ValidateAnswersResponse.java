package dev.hazoe.quizservice.quiz.dto.response;

import java.util.Map;

public record ValidateAnswersResponse(
        int total,
        int correct,
        Map<Long, Boolean> detail
) {}

