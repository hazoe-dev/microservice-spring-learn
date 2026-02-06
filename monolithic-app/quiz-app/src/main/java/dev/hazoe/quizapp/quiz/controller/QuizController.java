package dev.hazoe.quizapp.quiz.controller;

import dev.hazoe.quizapp.quiz.dto.response.QuizResultResponse;
import dev.hazoe.quizapp.quiz.dto.request.SubmitQuizRequest;
import dev.hazoe.quizapp.quiz.domain.Quiz;
import dev.hazoe.quizapp.quiz.dto.request.CreatedQuizRequest;
import dev.hazoe.quizapp.quiz.dto.response.QuizQuestionResponse;
import dev.hazoe.quizapp.quiz.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("api")
public class QuizController {

    private QuizService quizService;

    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("quizzes/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PostMapping("quizzes")
    public ResponseEntity<Void> createQuiz(
            @Valid @RequestBody CreatedQuizRequest request) {
        Quiz q = quizService.createQuiz(request);
        URI location = URI.create("/api/quiz/" + q.getId());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("quizzes/{id}/questions")
    public ResponseEntity<List<QuizQuestionResponse>> getQuestionsByQuizId(
            @PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuestionsByQuizId(id));
    }

    @PostMapping("quizzes/{id}/submit") //REST pragmatic / task-oriented
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody SubmitQuizRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(id, request));
    }
}
