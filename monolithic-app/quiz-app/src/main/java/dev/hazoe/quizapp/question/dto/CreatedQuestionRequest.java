package dev.hazoe.quizapp.question.dto;


import dev.hazoe.quizapp.question.domain.QuestionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreatedQuestionRequest(
        @NotBlank
        String title,

        @NotEmpty
        List<@NotBlank String> options ,

        @NotBlank
        String answer,

        QuestionLevel level,

        @NotBlank
        String category
) {
}
