package com.innowise.paymentservice.exception;

public class ForeignServiceException extends RuntimeException {
    public ForeignServiceException(String message) {
        super(message);
    }
}
