package dev.hazoe.quizapp.question.repository;

import dev.hazoe.quizapp.question.domain.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepo extends JpaRepository<Question, Long> {
    Page<Question> findByCategoryEqualsIgnoreCase(String category, Pageable pageable);

    @Modifying
    @Query("delete from Question q where q.id = :id")
    int deleteByIdReturningCount(@Param("id") Long id);


    @Modifying
    @Query("delete from Question q where lower(q.title) = lower(:title)")
    int deleteAllByTitleIgnoreCase(@Param("title") String title);

    @Query(nativeQuery = true,
            value = "SELECT * FROM Question q WHERE q.category = :category ORDER BY random() LIMIT :numOfQuestion")
    List<Question> findRandomQuestionsByCategory(@Param("category") String category,
                                                 @Param("numOfQuestion") int numOfQuestion);
}
