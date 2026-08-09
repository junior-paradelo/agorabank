package com.k3ras.agorabank.exception;

public class DuplicateIdempotentRequestException extends RuntimeException {

    public DuplicateIdempotentRequestException(String message) {
        super(message);
    }

    public DuplicateIdempotentRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
