package dev.hazoe.quizservice.quiz.repository;

import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuestionSummaryResponse;
import dev.hazoe.quizservice.quiz.dto.response.ValidateAnswersResponse;

import java.util.List;

public interface QuestionClient {

    List<QuestionSummaryResponse> getRandomQuestions(
            String category,
            int size
    );

    List<QuestionSummaryResponse> getQuestionsByIds(
            List<Long> ids
    );

    ValidateAnswersResponse validateAnswers(
            ValidateAnswersRequest request
    );
}
