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
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    @ElementCollection
    @CollectionTable(
            name = "question_options",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @Column(name = "option_value")
    private List<String> options = new ArrayList<>();

    private String answer;

    private String level;

    private String category;

    public static Question of(
            String name,
            String answer,
            String level,
            String category,
            List<String> options
    ) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options must not be empty");
        }

        Question q = new Question();
        q.setName(name.trim());
        q.setAnswer(answer.trim());
        q.setLevel(level);
        q.setCategory(category);
        q.setOptions(new ArrayList<>(options)); // defensive copy

        return q;
    }

}
