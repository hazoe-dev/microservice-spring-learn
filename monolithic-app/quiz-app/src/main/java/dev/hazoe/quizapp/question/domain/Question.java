package dev.hazoe.quizapp.question.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Builder
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String title;

    @ElementCollection
    @CollectionTable(
            name = "question_options",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @Column(name = "option_value")
    private List<String> options = new ArrayList<>();

    Integer correctOptionIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private QuestionLevel level =  QuestionLevel.EASY;

    private String category;

    public static Question of(
            String name,
            Integer answerIndex,
            String level,
            String category,
            List<String> options
    ) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options must not be empty");
        }

        Question q = new Question();
        q.setTitle(name.trim());
        q.setCorrectOptionIndex(answerIndex);
        q.setLevel(QuestionLevel.from(level));
        q.setCategory(category.trim().toLowerCase());
        q.setOptions(new ArrayList<>(options)); // defensive copy

        return q;
    }

}
