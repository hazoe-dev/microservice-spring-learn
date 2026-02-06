package dev.hazoe.questionservice.question.controller;


import dev.hazoe.questionservice.question.domain.Question;
import dev.hazoe.questionservice.question.dto.request.CreatedQuestionRequest;
import dev.hazoe.questionservice.question.dto.request.RandomQuestionRequest;
import dev.hazoe.questionservice.question.dto.response.QuestionSummaryResponse;
import dev.hazoe.questionservice.question.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("api/questions")
public class QuestionController {

    private QuestionService questionService;

    @Autowired
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ResponseEntity<Page<Question>> getQuestionsByCategory(
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        if (category == null || category.isEmpty()) {
            return ResponseEntity.ok(questionService.getAllQuestions(pageable));
        }
        return ResponseEntity.ok(questionService.getQuestionsByCategory(category, pageable));
    }

    @PostMapping
    public ResponseEntity<Void> addQuestion(
            @Valid @RequestBody CreatedQuestionRequest request) {
        Question q = questionService.addQuestion(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(q.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestionById(
            @PathVariable Long id
    ) {
        questionService.deleteQuestionById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteQuestionsByTitle(
            @RequestParam String title // support encode to avoid special characters
    ) {
        int deleted = questionService.deleteQuestionsByTitle(title);//title is not unique

        Map<String, Object> response = new HashMap<>();
        response.put("deletedCount", deleted);
        response.put("title", title);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/random")
    public ResponseEntity<List<QuestionSummaryResponse>> getRandomQuestions(
            @Valid @RequestBody RandomQuestionRequest request) {
        return ResponseEntity.ok(questionService.getRandomQuestions(
                request.category(),
                request.size()
        ));
    }


}
