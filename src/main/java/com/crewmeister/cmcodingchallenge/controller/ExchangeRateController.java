package com.crewmeister.cmcodingchallenge.controller;

import com.crewmeister.cmcodingchallenge.dto.ConversionResult;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesHistoryResponse;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesOnDateResponse;
import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Exchange Rates", description = "EUR-FX exchange rate operations")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "List all available currencies")
    @GetMapping("/currencies")
    public ResponseEntity<List<Currency>> getCurrencies() {
        return ResponseEntity.ok(exchangeRateService.getCurrencies());
    }


    @Operation(summary = "Get paginated exchange rate history")
    @GetMapping("/exchange-rates/history")
    public ResponseEntity<ExchangeRatesHistoryResponse> getExchangeRatesHistory(
            @RequestParam(name = "currency", defaultValue = "EUR") String currency,
            @RequestParam(name = "from_date", defaultValue = "2020-01-01")
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        LocalDate endDate = (toDate != null) ? toDate : LocalDate.now();
        if (fromDate.isAfter(endDate)) {
            throw new IllegalArgumentException("from_date must be before or equal to toDate");
        }
        return ResponseEntity.ok(exchangeRateService.getExchangeRatesHistory(currency, fromDate, endDate, page, size));
    }

    @Operation(summary = "Get all exchange rates on a specific date")
    @GetMapping("/exchange-rates/{on_date}")
    public ResponseEntity<ExchangeRatesOnDateResponse> getExchangeRatesOnDate(
            @RequestParam(defaultValue = "EUR", name = "currency") String currency,
            @PathVariable(name = "on_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRatesOnDate(currency, onDate));
    }

    @Operation(summary = "Convert amount between currencies on a date")
    @GetMapping("/convert-currency")
    public ResponseEntity<ConversionResult> convertCurrencyOnDate(
            @RequestParam("from_currency") @NotBlank String fromCurrency,
            @RequestParam(name = "to_currency", defaultValue = "EUR") @NotBlank String toCurrency,
            @RequestParam(defaultValue = "1") @Positive BigDecimal amount,
            @RequestParam(name = "on_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate) {
        LocalDate date = (onDate != null) ? onDate : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("on_date must be before or equal today");
        }
        return ResponseEntity.ok(exchangeRateService.convertCurrency(fromCurrency, toCurrency, amount, date));
    }

}
