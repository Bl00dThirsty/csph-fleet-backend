package com.gpl.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

/**
 * Réponse API standardisée pour tous les microservices.
 * Supporte le pattern ok() et l'alias success().
 *
 * @param <T> Type du payload
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ErrorDetails error;
    private Instant timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDetails {
        private String code;
        private String description;
        private String field;
        private String traceId;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    // Alias methods to support both ok() and success() conventions
    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ok(data, message);
    }

    public static <T> ApiResponse<T> error(String code, String description) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder().code(code).description(description).build())
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String description, String field) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder().code(code).description(description).field(field).build())
                .timestamp(Instant.now())
                .build();
    }
}
