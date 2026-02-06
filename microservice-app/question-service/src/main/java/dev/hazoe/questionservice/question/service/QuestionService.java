package dev.hazoe.questionservice.question.service;

import dev.hazoe.questionservice.exception.ResourceNotFoundException;
import dev.hazoe.questionservice.question.domain.Question;
import dev.hazoe.questionservice.question.dto.request.CreatedQuestionRequest;
import dev.hazoe.questionservice.question.dto.response.QuestionSummaryResponse;
import dev.hazoe.questionservice.question.repository.QuestionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    public List<QuestionSummaryResponse> getRandomQuestions( String category, int size) {
        long count = questionRepo.countByCategoryIgnoreCase(category);
        if (count < size) {
            throw new IllegalArgumentException(
                    "Requested " + size + " questions but only found " + count
            );
        }

        int randomOffset = ThreadLocalRandom.current()
                .nextInt((int) (count - size + 1));

        Pageable pageable = PageRequest.of(
                randomOffset / size,
                size
        );

        Page<Question> page = questionRepo.findByCategoryIgnoreCase(category, pageable);
        List<Question> questions = new ArrayList<>(page.getContent());

        Collections.shuffle(questions);
        return questions.stream()
                .map(this::toSummary)
                .toList();
    }

    private QuestionSummaryResponse toSummary(Question q) {
        return new QuestionSummaryResponse(
                q.getId(),
                q.getTitle(),
                q.getOptions(),
                q.getLevel().name()
        );
    }


}
