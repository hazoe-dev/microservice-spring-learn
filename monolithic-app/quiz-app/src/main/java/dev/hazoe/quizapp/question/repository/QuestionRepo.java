package dev.hazoe.quizapp.question.repository;

import dev.hazoe.quizapp.question.domain.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepo extends JpaRepository<Question, Long> {
    Page<Question> findByCategoryEqualsIgnoreCase(String category, Pageable pageable);
}
