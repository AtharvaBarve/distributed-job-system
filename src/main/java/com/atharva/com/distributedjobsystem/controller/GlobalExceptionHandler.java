package com.atharva.com.distributedjobsystem.controller;

import com.atharva.com.distributedjobsystem.exception.JobNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ProblemDetail handleDatabaseFailure(CannotGetJdbcConnectionException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Database service is currently unavailable"
        );
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ProblemDetail handleRedisFailure(RedisConnectionFailureException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Redis service is currently unavailable"
        );
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ProblemDetail handleNotFound(JobNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Job not found");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Request validation failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
