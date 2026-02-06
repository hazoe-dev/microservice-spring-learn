package dev.hazoe.quizservice.quiz.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RandomQuestionRequest(
        @NotNull
        String category,

        @Min(0)
        int size
) {}

