package dev.hazoe.quizservice.quiz.controller;

import dev.hazoe.quizservice.quiz.domain.Quiz;
import dev.hazoe.quizservice.quiz.dto.request.CreatedQuizRequest;
import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuizQuestionResponse;
import dev.hazoe.quizservice.quiz.dto.response.QuizResultResponse;
import dev.hazoe.quizservice.quiz.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("api/quizzes")
public class QuizController {

    private QuizService quizService;

    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PostMapping
    public ResponseEntity<Void> createQuiz(
            @Valid @RequestBody CreatedQuizRequest request) {
        Quiz q = quizService.createQuiz(request);
        URI location = URI.create("/api/quiz/" + q.getId());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuizQuestionResponse>> getQuestionsByQuizId(
            @PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuestionsByQuizId(id));
    }

    @PostMapping("/{id}/submit") //REST pragmatic / task-oriented
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody ValidateAnswersRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(id, request));
    }
}
