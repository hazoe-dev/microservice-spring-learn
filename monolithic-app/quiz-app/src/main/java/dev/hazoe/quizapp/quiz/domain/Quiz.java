package dev.hazoe.quizapp.quiz.domain;

import dev.hazoe.quizapp.question.domain.Question;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "questions")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, //collect available ids before connecting to DB, not need always flush to get ID
            generator = "quiz_seq") //not compatible to mysql, work with postgres
    @SequenceGenerator( //performance
            name = "quiz_seq",
            sequenceName = "quiz_seq",
            allocationSize = 10
    )
    @EqualsAndHashCode.Include
    private Long id;

    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "quiz_question",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> questions = new ArrayList<>();

}
