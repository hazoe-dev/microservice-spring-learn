package dev.hazoe.quizapp.quiz.controller;

import dev.hazoe.quizapp.quiz.domain.Quiz;
import dev.hazoe.quizapp.quiz.dto.CreatedQuizRequest;
import dev.hazoe.quizapp.quiz.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("api")
public class QuizController {

    private QuizService quizService;

    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("quiz/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PostMapping("quiz")
    public ResponseEntity<Void> createQuiz(
            @Valid @RequestBody CreatedQuizRequest request) {
        Quiz q = quizService.createQuiz(request);
        URI location = URI.create("/api/quiz/" + q.getId());
        return ResponseEntity.created(location).build();
    }

}
