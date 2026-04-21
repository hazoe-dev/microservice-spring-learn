package dev.hazoe.quizservice.quiz.controller;

import dev.hazoe.quizservice.quiz.domain.Quiz;
import dev.hazoe.quizservice.quiz.dto.request.CreatedQuizRequest;
import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuizQuestionResponse;
import dev.hazoe.quizservice.quiz.dto.response.QuizResultResponse;
import dev.hazoe.quizservice.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
@Slf4j
@RestController
@RequestMapping("api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PostMapping
    public ResponseEntity<Void> createQuiz(
            @Valid @RequestBody CreatedQuizRequest request) {
        Quiz q = quizService.createQuiz(request);
        URI location = URI.create("/api/quizzes/" + q.getId());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}/questions")
    public CompletableFuture<ResponseEntity<List<QuizQuestionResponse>>> getQuestionsByQuizId(
            @PathVariable Long id) {
        return quizService.getQuestionsByQuizId(id)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Failed to get quiz questions", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/{id}/submit") //REST pragmatic / task-oriented
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody ValidateAnswersRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(id, request));
    }
}
