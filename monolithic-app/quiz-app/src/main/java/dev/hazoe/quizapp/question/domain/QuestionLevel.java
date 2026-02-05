package dev.hazoe.quizapp.question.domain;

import java.util.Arrays;

public enum QuestionLevel {
    EASY, MEDIUM, HARD;
    public static QuestionLevel from(String value) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid level: " + value));
    }
}

