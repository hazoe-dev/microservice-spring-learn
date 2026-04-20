package dev.hazoe.quizservice.quiz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean
    public Executor quizExecutor() {
        return Executors.newFixedThreadPool(30);
    }

}