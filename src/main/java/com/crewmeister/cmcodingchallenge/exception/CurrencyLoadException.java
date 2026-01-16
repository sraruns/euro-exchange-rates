package com.crewmeister.cmcodingchallenge.exception;

public class CurrencyLoadException extends RuntimeException {

    public CurrencyLoadException(String message) {
        super(message);
    }

    public CurrencyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
