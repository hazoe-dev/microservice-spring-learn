package dev.hazoe.quizapp.question.service;

import dev.hazoe.quizapp.question.domain.Question;
import dev.hazoe.quizapp.question.repository.QuestionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {
    private QuestionRepo questionRepo;

    @Autowired
    public QuestionService(QuestionRepo questionRepo) {
        this.questionRepo = questionRepo;
    }

    public List<Question> getAllQuestions() {
        return questionRepo.findAll();
    }
}
