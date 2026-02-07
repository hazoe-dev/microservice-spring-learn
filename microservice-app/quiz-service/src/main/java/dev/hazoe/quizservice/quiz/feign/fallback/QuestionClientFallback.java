package dev.hazoe.quizservice.quiz.feign.fallback;

import dev.hazoe.quizservice.quiz.dto.request.RandomQuestionRequest;
import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuestionSummaryResponse;
import dev.hazoe.quizservice.quiz.dto.response.ValidateAnswersResponse;
import dev.hazoe.quizservice.quiz.feign.QuestionClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class QuestionClientFallback implements QuestionClient {

    @Override
    public List<QuestionSummaryResponse> getRandomQuestions(
            RandomQuestionRequest request) {
        return Collections.emptyList();
    }

    @Override
    public List<QuestionSummaryResponse> getQuestionsByIds(List<Long> ids) {
        // degrade gracefully
        return Collections.emptyList();
    }

    @Override
    public ValidateAnswersResponse validateAnswers(ValidateAnswersRequest request) {
        return new ValidateAnswersResponse(
                request.answers().size(),
                0,
                Collections.emptyMap()
        );
    }
}

