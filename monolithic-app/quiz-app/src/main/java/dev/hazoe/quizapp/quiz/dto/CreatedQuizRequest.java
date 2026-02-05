package dev.hazoe.quizapp.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatedQuizRequest(
        @NotBlank
        String title,

        @Min(1)
        int numOfQuestion,

        @NotBlank
        @Size(max = 50)
        String category
) {
}
