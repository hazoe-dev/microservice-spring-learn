package dev.hazoe.quizapp.question.service;

import dev.hazoe.quizapp.common.exception.ResourceNotFoundException;
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
        if (request.selectedOption() >= request.options().size()) {
            throw new IllegalArgumentException("Answer must be one of the options");
        }

        Question question = Question
                .builder()
                .title(request.title().trim())
                .category(request.category().trim().toLowerCase())
                .correctOptionIndex(request.selectedOption())
                .options(request.options())
                .level(request.level())
                .build();

        return questionRepo.save(question);
    }

    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        return questionRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Question not found with id: " + id)
                );
    }

    @Transactional
    public void deleteQuestionById(Long id) {
        int deleted = questionRepo.deleteByIdReturningCount(id);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Question not found with id: " + id);
        }
    }

    @Transactional
    public int deleteQuestionsByTitle(String title) {
        int deletedCount = questionRepo.deleteAllByTitleIgnoreCase(title);
        if (deletedCount == 0) {
            throw new ResourceNotFoundException(
                    "No questions found with title: " + title
            );
        }
        return deletedCount;
    }
}
