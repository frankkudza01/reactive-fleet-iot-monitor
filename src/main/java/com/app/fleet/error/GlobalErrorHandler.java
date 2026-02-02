package com.example.fleet.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Object>> handle(Exception ex) {
        return Mono.just(Map.of(
                "ts", Instant.now().toString(),
                "error", "INTERNAL_SERVER_ERROR",
                "message", ex.getMessage() == null ? "Unexpected error" : ex.getMessage()
        ));
    }
}
