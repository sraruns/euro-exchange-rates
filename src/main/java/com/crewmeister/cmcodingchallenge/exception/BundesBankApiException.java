package com.crewmeister.cmcodingchallenge.exception;

public class BundesBankApiException extends RuntimeException {

    private final int statusCode;

    public BundesBankApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public BundesBankApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
