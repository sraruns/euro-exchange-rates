package com.crewmeister.cmcodingchallenge.controller;

import com.crewmeister.cmcodingchallenge.dto.ConversionResult;
import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import com.crewmeister.cmcodingchallenge.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/currencies")
    public ResponseEntity<List<Currency>> getCurrencies() {
        return ResponseEntity.ok(exchangeRateService.getCurrencies());
    }


    @GetMapping("/exchange-rates/history")
    public ResponseEntity<List<ExchangeRate>> getExchangeRatesHistory(
            @RequestParam(name = "currency", defaultValue = "EUR") String currency,
            @RequestParam(name = "from_date", defaultValue = "2025-01-01")
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        LocalDate endDate = (toDate != null) ? toDate : LocalDate.now();
        if (fromDate.isAfter(endDate)) {
            throw new IllegalArgumentException("from_date must be before or equal to toDate");
        }
        return ResponseEntity.ok(exchangeRateService.getExchangeRates(currency, fromDate, endDate));
    }

    @GetMapping("/exchange-rates/{on_date}")
    public ResponseEntity<List<ExchangeRate>> getExchangeRatesOnDate(
            @RequestParam(defaultValue = "EUR", name = "currency") String currency,
            @PathVariable(name = "on_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRatesOnDate(currency, onDate));
    }

    @GetMapping("/convert-currency")
    public ResponseEntity<ConversionResult> convertCurrencyOnDate(
            @RequestParam("from_currency") @NotBlank String fromCurrency,
            @RequestParam(name = "to_currency", defaultValue = "EUR") @NotBlank String toCurrency,
            @RequestParam(defaultValue = "1") @Positive BigDecimal amount,
            @RequestParam(name = "on_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate) {
        return ResponseEntity.ok(exchangeRateService.convertCurrency(fromCurrency, toCurrency, amount, onDate));
    }

}
