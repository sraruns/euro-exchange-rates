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
public class ExchangeRatesHistoryResponse {
    private String baseCurrency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Map<String, BigDecimal>> rates; // date -> targetCurrency -> rate
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
