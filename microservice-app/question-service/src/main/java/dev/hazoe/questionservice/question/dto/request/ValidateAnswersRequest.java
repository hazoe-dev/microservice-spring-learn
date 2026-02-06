package dev.hazoe.questionservice.question.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ValidateAnswersRequest(
        @NotEmpty
        List<@Valid AnswerRequest> answers
) {
    public record AnswerRequest(
            @NotNull
            Long questionId,

            @NotNull
            Integer selectedOptionIndex
    ) {
    }
}
