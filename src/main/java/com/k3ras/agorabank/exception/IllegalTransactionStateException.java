package com.k3ras.agorabank.exception;

public class IllegalTransactionStateException extends RuntimeException {

    public IllegalTransactionStateException(String message) {
        super(message);
    }

    public IllegalTransactionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
