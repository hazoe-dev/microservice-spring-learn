package dev.hazoe.quizapp.quiz.repository;

import dev.hazoe.quizapp.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepo extends JpaRepository<Quiz, Long> {

    //join fetch to avoid N+1 query problem
    @Query("""
            SELECT q 
            FROM Quiz q 
            JOIN FETCH q.questions
            WHERE q.id = :id
            """)
    Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);
}
