package com.razonapro.razonaprobackend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.razonapro.razonaprobackend.shared.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String  code;
    private String  message;
    private T       data;
    private Object  errors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder().success(true).message(message).build();
    }

    public static <T> ApiResponse<T> error(ErrorCode ec) {
        return ApiResponse.<T>builder()
                .success(false).code(ec.getCode()).message(ec.getDefaultMessage()).build();
    }

    public static <T> ApiResponse<T> error(ErrorCode ec, String message) {
        return ApiResponse.<T>builder()
                .success(false).code(ec.getCode()).message(message).build();
    }

    public static <T> ApiResponse<T> error(ErrorCode ec, String message, Object errors) {
        return ApiResponse.<T>builder()
                .success(false).code(ec.getCode()).message(message).errors(errors).build();
    }
}