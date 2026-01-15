package com.crewmeister.cmcodingchallenge.exception;

import java.time.LocalDate;

public class ExchangeRateNotFoundException extends RuntimeException {

    private final String currency;
    private final LocalDate date;

    public ExchangeRateNotFoundException(String currency, LocalDate date) {
        super(String.format("Exchange rate not found for %s on %s. This might be a weekend or holiday.", currency, date));
        this.currency = currency;
        this.date = date;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getDate() {
        return date;
    }
}
