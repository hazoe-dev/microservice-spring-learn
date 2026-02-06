package dev.hazoe.questionservice.question.dto;


import dev.hazoe.questionservice.question.domain.QuestionLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatedQuestionRequest(
        @NotBlank
        String title,

        @NotEmpty
        List<@NotBlank String> options ,

        @NotNull
        @Min(0)
        Integer selectedOption,

        QuestionLevel level,

        @NotBlank
        String category
) {
}
