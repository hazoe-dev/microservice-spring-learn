package dev.hazoe.quizservice.quiz.service;

import dev.hazoe.quizservice.exception.ResourceNotFoundException;
import dev.hazoe.quizservice.quiz.domain.Quiz;
import dev.hazoe.quizservice.quiz.dto.request.CreatedQuizRequest;
import dev.hazoe.quizservice.quiz.dto.request.ValidateAnswersRequest;
import dev.hazoe.quizservice.quiz.dto.response.QuestionSummaryResponse;
import dev.hazoe.quizservice.quiz.dto.response.QuizQuestionResponse;
import dev.hazoe.quizservice.quiz.dto.response.QuizResultResponse;
import dev.hazoe.quizservice.quiz.dto.response.ValidateAnswersResponse;
import dev.hazoe.quizservice.quiz.repository.QuestionClient;
import dev.hazoe.quizservice.quiz.repository.QuizRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    public static final String QUIZ_NOT_FOUND_WITH_ID = "Quiz not found with id: ";

    private final QuizRepo quizRepo;

    private final QuestionClient questionClient;


    @Transactional
    public Quiz createQuiz(CreatedQuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setTitle(request.title().trim());

        List<QuestionSummaryResponse> questions =
                questionClient.getRandomQuestions(
                        request.category(),
                        request.numOfQuestion()
                );
        List<Long> questionIds = questions.stream()
                .map(QuestionSummaryResponse::id)
                .toList();

        quiz.setQuestionIds(questionIds);

        return quizRepo.save(quiz);
    }

    public Quiz getQuizById(Long id) {
        return quizRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(QUIZ_NOT_FOUND_WITH_ID + id)
                );
    }

    public List<QuizQuestionResponse> getQuestionsByQuizId(Long quizId) {
        Quiz quiz = getQuizById(quizId);
        List<QuestionSummaryResponse> questions =
                questionClient.getQuestionsByIds(
                        quiz.getQuestionIds()
                );
        return questions.stream()
                .map(q -> new QuizQuestionResponse(
                        q.id(),
                        q.title(),
                        q.options(),
                        q.level()
                ))
                .toList();
    }

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
