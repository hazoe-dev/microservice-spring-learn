package dev.hazoe.quizapp.question.controller;

import dev.hazoe.quizapp.question.domain.Question;
import dev.hazoe.quizapp.question.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api")
public class QuestionController {

    private QuestionService questionService;

    @Autowired
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("questions")
    public ResponseEntity<List<Question>> getQuestions() {
        return ResponseEntity.ok(questionService.getAllQuestions());
    }
}
