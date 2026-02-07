package dev.hazoe.quizservice.quiz.repository;

import dev.hazoe.quizservice.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepo extends JpaRepository<Quiz, Long> {
}
