package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesHistoryResponse;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesOnDateResponse;
import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class ExchangeRateMapper {

    public ExchangeRatesHistoryResponse toHistoryResponse(
            String baseCurrency,
            LocalDate startDate,
            LocalDate endDate,
            List<ExchangeRate> rates,
            Page<LocalDate> datesPage) {

        Map<String, Map<String, BigDecimal>> ratesMap = rates.stream()
                .collect(Collectors.groupingBy(
                        rate -> rate.getDate().toString(),
                        TreeMap::new,
                        Collectors.toMap(
                                ExchangeRate::getTargetCurrency,
                                ExchangeRate::getRate,
                                (v1, v2) -> v1,
                                TreeMap::new)));

        return ExchangeRatesHistoryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .baseCurrency(baseCurrency)
                .rates(ratesMap)
                .page(datesPage.getNumber())
                .size(datesPage.getSize())
                .totalElements(datesPage.getTotalElements())
                .totalPages(datesPage.getTotalPages())
                .build();
    }

    public ExchangeRatesOnDateResponse toOnDateResponse(
            String baseCurrency,
            LocalDate date,
            List<ExchangeRate> rates) {

        Map<String, BigDecimal> ratesMap = rates.stream()
                .collect(Collectors.toMap(
                        ExchangeRate::getTargetCurrency,
                        ExchangeRate::getRate,
                        (v1, v2) -> v1,
                        TreeMap::new));

        String message = ratesMap.isEmpty()
                ? "No rates available for this date. It may be a weekend or public holiday."
                : null;

        return ExchangeRatesOnDateResponse.builder()
                .baseCurrency(baseCurrency)
                .date(date)
                .rates(ratesMap)
                .message(message)
                .build();
    }
}
