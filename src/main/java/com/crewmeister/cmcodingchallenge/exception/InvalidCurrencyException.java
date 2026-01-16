package com.crewmeister.cmcodingchallenge.exception;

public class InvalidCurrencyException extends RuntimeException {

    private final String currency;

    public InvalidCurrencyException(String currency) {
        super(String.format("Invalid currency code: %s", currency));
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }
}
