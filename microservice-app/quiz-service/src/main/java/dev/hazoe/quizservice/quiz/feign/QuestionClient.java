package dev.hazoe.quizservice.quiz.feign;

import dev.hazoe.quizservice.quiz.dto.request.RandomQuestionRequest;
import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuestionSummaryResponse;
import dev.hazoe.quizservice.quiz.dto.response.ValidateAnswersResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@FeignClient("QUESTION-SERVICE")
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

