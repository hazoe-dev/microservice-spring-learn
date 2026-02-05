package dev.hazoe.quizapp.question.service;

import dev.hazoe.quizapp.question.domain.Question;
import dev.hazoe.quizapp.question.dto.CreatedQuestionRequest;
import dev.hazoe.quizapp.question.repository.QuestionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {
    private QuestionRepo questionRepo;

    @Autowired
    public QuestionService(QuestionRepo questionRepo) {
        this.questionRepo = questionRepo;
    }


    @Transactional(readOnly = true)
    public Page<Question> getAllQuestions(Pageable pageable) {
        return questionRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Question> getQuestionsByCategory(String category, Pageable pageable) {
        return questionRepo.findByCategoryEqualsIgnoreCase(category, pageable);
    }

    @Transactional
    public Question addQuestion(CreatedQuestionRequest request) {
        if (!request.options().contains(request.answer())) {
            throw new IllegalArgumentException("Answer must be one of the options");
        }

        Question question = Question
                .builder()
                .title(request.title().trim())
                .category(request.category().trim().toLowerCase())
                .answer(request.answer())
                .options(request.options())
                .level(request.level())
                .build();

        return questionRepo.save(question);
    }
}
