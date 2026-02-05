package dev.hazoe.quizapp.quiz.service;

import dev.hazoe.quizapp.common.exception.ResourceNotFoundException;
import dev.hazoe.quizapp.question.domain.Question;
import dev.hazoe.quizapp.question.repository.QuestionRepo;
import dev.hazoe.quizapp.quiz.domain.Quiz;
import dev.hazoe.quizapp.quiz.dto.CreatedQuizRequest;
import dev.hazoe.quizapp.quiz.repository.QuizRepo;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class QuizService {

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
        Page<Question> page = questionRepo
                .findRandomQuestionsByCategory(
                        request.category().trim().toLowerCase(),
                        PageRequest.of(0, request.numOfQuestion()));

        List<Question> questions = new ArrayList<>(page.getContent()); //avoid linked với persistence context

        if (questions.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No questions found for category: " + request.category()
            );
        }

        if (questions.size() < request.numOfQuestion()) {
            throw new IllegalArgumentException(
                    "Requested " + request.numOfQuestion() +
                            " questions but only found " + questions.size()
            );
        }
        Collections.shuffle(questions); //random returned data
        return questions;
    }

    @Transactional(readOnly = true)
    public Quiz getQuizById(Long id) {
        return quizRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Quiz not found with id: " + id)
                );
    }
}
