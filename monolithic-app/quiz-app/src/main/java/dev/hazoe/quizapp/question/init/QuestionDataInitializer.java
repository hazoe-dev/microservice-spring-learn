package dev.hazoe.quizapp.question.init;

import dev.hazoe.quizapp.question.domain.Question;
import dev.hazoe.quizapp.question.repository.QuestionRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionDataInitializer implements CommandLineRunner {

    private final QuestionRepo questionRepository;

    public QuestionDataInitializer(QuestionRepo questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {

        questionRepository.saveAll(List.of(
                Question.of(
                        "What is Java?",
                        0,
                        "EASY",
                        "PROGRAMMING",
                        List.of("Programming language", "Database", "Operating system")
                ),
                Question.of(
                        "What is PostgreSQL?",
                        1,
                        "EASY",
                        "DATABASE",
                        List.of("NoSQL database", "Relational database", "Web server")
                ),

                // =======================
                // JAVA QUESTIONS
                // =======================

                Question.of(
                        "What is JVM?",
                        1,
                        "EASY",
                        "JAVA",
                        List.of(
                                "Java Visual Model",
                                "Java Virtual Machine",
                                "Java Variable Method",
                                "Joint Virtual Memory"
                        )
                ),

                Question.of(
                        "Which keyword is used to inherit a class in Java?",
                        2,
                        "EASY",
                        "JAVA",
                        List.of(
                                "implements",
                                "inherits",
                                "extends",
                                "super"
                        )
                ),

                Question.of(
                        "What does the 'final' keyword mean in Java?",
                        1,
                        "MEDIUM",
                        "JAVA",
                        List.of(
                                "Can be overridden",
                                "Cannot be changed",
                                "Runs faster",
                                "Used only for classes"
                        )
                ),

                // =======================
                // PYTHON QUESTIONS
                // =======================

                Question.of(
                        "What is Python?",
                        2,
                        "EASY",
                        "PYTHON",
                        List.of(
                                "A database",
                                "A web server",
                                "A high-level programming language",
                                "An operating system"
                        )
                ),

                Question.of(
                        "Which keyword is used to define a function in Python?",
                        2,
                        "EASY",
                        "PYTHON",
                        List.of(
                                "function",
                                "define",
                                "def",
                                "fun"
                        )
                ),

                Question.of(
                        "What is the output of len([1, 2, 3]) in Python?",
                        1,
                        "EASY",
                        "PYTHON",
                        List.of(
                                "2",
                                "3",
                                "Error",
                                "None"
                        )
                )
        ));

    }
}
