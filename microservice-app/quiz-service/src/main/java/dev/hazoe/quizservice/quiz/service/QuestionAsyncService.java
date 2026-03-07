package dev.hazoe.quizservice.quiz.service;

import dev.hazoe.quizservice.quiz.dto.response.QuestionSummaryResponse;
import dev.hazoe.quizservice.quiz.dto.response.QuizQuestionResponse;
import dev.hazoe.quizservice.quiz.feign.QuestionClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionAsyncService {

    private final QuestionClient questionClient;
    private final Executor quizExecutor;

    @Retry(name = "questionService")
    @CircuitBreaker(name = "questionService", fallbackMethod = "getQuestionsFallback")
    @TimeLimiter(name = "questionService")
    public CompletableFuture<List<QuizQuestionResponse>> getQuestionsAsync(List<Long> questionIds) {

        return CompletableFuture.supplyAsync(() -> {

            if (questionIds.isEmpty()) {
                return List.of();
            }

            List<QuestionSummaryResponse> questions =
                    questionClient.getQuestionsByIds(questionIds);

            return questions.stream()
                    .map(q -> new QuizQuestionResponse(
                            q.id(),
                            q.title(),
                            q.options(),
                            q.level()))
                    .toList();

        }, quizExecutor);
    }

    public CompletableFuture<List<QuizQuestionResponse>> getQuestionsFallback(
            List<Long> questionIds,
            Throwable ex
    ) {
        log.error("Question service failed for ids {}: {}", questionIds, ex.getMessage());

        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}