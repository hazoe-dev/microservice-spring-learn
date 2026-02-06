package dev.hazoe.quizapp.quiz.service;

import dev.hazoe.quizapp.common.exception.ResourceNotFoundException;
import dev.hazoe.quizapp.question.domain.Question;
import dev.hazoe.quizapp.quiz.dto.response.QuizResultResponse;
import dev.hazoe.quizapp.quiz.dto.request.SubmitQuizRequest;
import dev.hazoe.quizapp.question.repository.QuestionRepo;
import dev.hazoe.quizapp.quiz.domain.Quiz;
import dev.hazoe.quizapp.quiz.dto.request.CreatedQuizRequest;
import dev.hazoe.quizapp.quiz.dto.response.QuizQuestionResponse;
import dev.hazoe.quizapp.quiz.repository.QuizRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class QuizService {

    public static final String QUIZ_NOT_FOUND_WITH_ID = "Quiz not found with id: ";

    private final QuizRepo quizRepo;

    private final QuestionRepo questionRepo;

    @Autowired
    public QuizService(QuizRepo quizRepo,
                       QuestionRepo questionRepo) {
        this.quizRepo = quizRepo;
        this.questionRepo = questionRepo;
    }

    @Transactional
    public Quiz createQuiz(CreatedQuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setTitle(request.title().trim());

        List<Question> questions = getQuestions(request);
        quiz.setQuestions(questions);

        return quizRepo.save(quiz);
    }

    private List<Question> getQuestions(CreatedQuizRequest request) {
        String category = request.category();
        int size = request.numOfQuestion();

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
        return questions;
    }


    @Transactional(readOnly = true)
    public Quiz getQuizById(Long id) {
        return quizRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(QUIZ_NOT_FOUND_WITH_ID + id)
                );
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> getQuestionsByQuizId(Long quizId) {
        Quiz quiz = quizRepo.findByIdWithQuestions(quizId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(QUIZ_NOT_FOUND_WITH_ID + quizId)
                );
        return quiz.getQuestions().stream()
                .map(question -> new QuizQuestionResponse(
                        question.getId(),
                        question.getTitle(),
                        question.getOptions(),
                        question.getLevel()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResultResponse submitQuiz(Long quizId, SubmitQuizRequest request) {
        Quiz quiz = quizRepo.findByIdWithQuestions(quizId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(QUIZ_NOT_FOUND_WITH_ID + quizId)
                );

        Map<Long, Integer> answerMap = request.answers().stream()
                .collect(Collectors.toMap(
                        SubmitQuizRequest.AnswerRequest::questionId,
                        SubmitQuizRequest.AnswerRequest::selectedOptionIndex
                ));

        int correctCount = 0;

        for (Question question : quiz.getQuestions()) {
            Integer selected = answerMap.get(question.getId());
            if (selected != null && selected.equals(question.getCorrectOptionIndex())) {
                correctCount++;
            }
        }

        int total = quiz.getQuestions().size();

        double score = total == 0 ? 0 : (correctCount * 100.0 / total);

        return new QuizResultResponse(
                quizId,
                total,
                correctCount,
                score
        );

    }
}
