package dev.hazoe.quizservice.quiz.feign;

import dev.hazoe.quizservice.quiz.dto.request.RandomQuestionRequest;
import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuestionSummaryResponse;
import dev.hazoe.quizservice.quiz.dto.response.ValidateAnswersResponse;
import dev.hazoe.quizservice.quiz.feign.fallback.QuestionClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@FeignClient(
        name = "QUESTION-SERVICE",
        fallback = QuestionClientFallback.class
)
public interface QuestionClient {

    @PostMapping("/api/questions/random")
    List<QuestionSummaryResponse> getRandomQuestions(
            @RequestBody RandomQuestionRequest request
    );

    @GetMapping("/api/questions/by-ids")
    List<QuestionSummaryResponse> getQuestionsByIds(
            @RequestParam List<Long> ids
    );

    @PostMapping("/api/questions/validate")
    ValidateAnswersResponse validateAnswers(
            @RequestBody ValidateAnswersRequest request
    );
}

