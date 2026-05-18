package com.razonapro.razonaprobackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
