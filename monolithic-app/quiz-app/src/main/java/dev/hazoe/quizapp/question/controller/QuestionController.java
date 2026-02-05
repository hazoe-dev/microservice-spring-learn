package dev.hazoe.quizapp.question.controller;

import dev.hazoe.quizapp.question.domain.Question;
import dev.hazoe.quizapp.question.dto.CreatedQuestionRequest;
import dev.hazoe.quizapp.question.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api")
public class QuestionController {

    private QuestionService questionService;

    @Autowired
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("questions")
    public ResponseEntity<Page<Question>> getQuestionsByCategory(
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        if (category == null || category.isEmpty()) {
            return ResponseEntity.ok(questionService.getAllQuestions(pageable));
        }
        return ResponseEntity.ok(questionService.getQuestionsByCategory(category, pageable));
    }

    @PostMapping("questions")
    public ResponseEntity<Question> addQuestion(
            @Valid @RequestBody CreatedQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.addQuestion(request));
    }
}
