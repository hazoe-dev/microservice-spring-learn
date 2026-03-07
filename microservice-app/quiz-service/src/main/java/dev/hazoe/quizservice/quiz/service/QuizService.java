package dev.hazoe.quizservice.quiz.service;

import dev.hazoe.quizservice.exception.ResourceNotFoundException;
import dev.hazoe.quizservice.quiz.domain.Quiz;
import dev.hazoe.quizservice.quiz.dto.request.CreatedQuizRequest;
import dev.hazoe.quizservice.quiz.dto.request.RandomQuestionRequest;
import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuestionSummaryResponse;
import dev.hazoe.quizservice.quiz.dto.response.QuizQuestionResponse;
import dev.hazoe.quizservice.quiz.dto.response.QuizResultResponse;
import dev.hazoe.quizservice.quiz.dto.response.ValidateAnswersResponse;
import dev.hazoe.quizservice.quiz.feign.QuestionClient;
import dev.hazoe.quizservice.quiz.repository.QuizRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    public static final String QUIZ_NOT_FOUND_WITH_ID = "Quiz not found with id: ";

    private final QuizRepo quizRepo;

    private final QuestionClient questionClient;

    private final RestTemplate restTemplate;

    private final QuestionAsyncService questionAsyncService;

    @Transactional(readOnly = true)
    public List<QuestionSummaryResponse> getQuestionsByIds(List<Long> ids) {

        String url = "http://QUESTION-SERVICE/api/questions/by-ids?ids={ids}";

        ResponseEntity<List<QuestionSummaryResponse>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        },
                        String.join(",", ids.stream().map(String::valueOf).toList())
                );

        return response.getBody();
    }

    @Transactional
    public Quiz createQuiz(CreatedQuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setTitle(request.title().trim());

        List<QuestionSummaryResponse> questions =
                questionClient.getRandomQuestions(
                        new RandomQuestionRequest(
                                request.category(),
                                request.numOfQuestion())
                );
        List<Long> questionIds = questions.stream()
                .map(QuestionSummaryResponse::id)
                .toList();

        quiz.setQuestionIds(questionIds);

        return quizRepo.save(quiz);
    }

    @Transactional(readOnly = true)
    public Quiz getQuizById(Long id) {
        return quizRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(QUIZ_NOT_FOUND_WITH_ID + id)
                );
    }

    public CompletableFuture<List<QuizQuestionResponse>> getQuestionsByQuizId(Long quizId) {

        List<Long> questionIds = getQuestionIds(quizId); //self call but OK because quizRepo.findById(id) has its transactional

        return questionAsyncService.getQuestionsAsync(questionIds); //  go through proxy
    }

    @Transactional(readOnly = true)
    public List<Long> getQuestionIds(Long quizId) {
        Quiz quiz = getQuizById(quizId);
        return new ArrayList<>(quiz.getQuestionIds());
    }

    @Transactional(readOnly = true)
    public QuizResultResponse submitQuiz(Long quizId, ValidateAnswersRequest request) {
        Quiz quiz = getQuizById(quizId);
        ValidateAnswersResponse validation =
                questionClient.validateAnswers(
                        new ValidateAnswersRequest(
                                request.answers()
                        )
                );

        int total = quiz.getQuestionIds().size();
        int correct = validation.correct();

        double score = total == 0
                ? 0
                : (correct * 100.0 / total);

        return new QuizResultResponse(
                quizId,
                total,
                correct,
                score
        );

    }
}
