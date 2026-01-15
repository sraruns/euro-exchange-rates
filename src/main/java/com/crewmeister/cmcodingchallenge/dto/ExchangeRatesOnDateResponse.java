package com.crewmeister.cmcodingchallenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRatesOnDateResponse {
    private String baseCurrency;
    private LocalDate date;
    private Map<String, BigDecimal> rates; // targetCurrency -> rate
    private String message;
}
