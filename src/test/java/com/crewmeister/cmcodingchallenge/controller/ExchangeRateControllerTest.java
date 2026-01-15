package com.crewmeister.cmcodingchallenge.controller;

import com.crewmeister.cmcodingchallenge.dto.ConversionResult;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesHistoryResponse;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesOnDateResponse;
import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService service;

    @Test
    void getCurrencies_returns200() throws Exception {
        when(service.getCurrencies()).thenReturn(List.of(
                new Currency("USD", "US Dollar"),
                new Currency("GBP", "British Pound")
        ));

        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("USD"));
    }

    @Test
    void getExchangeRatesHistory_returns200() throws Exception {
        ExchangeRatesHistoryResponse response = ExchangeRatesHistoryResponse.builder()
                .baseCurrency("EUR")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 15))
                .rates(Map.of("2024-01-15", Map.of("USD", new BigDecimal("1.0856"))))
                .page(0).size(20).totalElements(1).totalPages(1)
                .build();

        when(service.getExchangeRatesHistory(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/exchange-rates/history")
                        .param("from_date", "2024-01-01")
                        .param("to_date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("EUR"));
    }

    @Test
    void getExchangeRatesOnDate_returns200() throws Exception {
        ExchangeRatesOnDateResponse response = ExchangeRatesOnDateResponse.builder()
                .baseCurrency("EUR")
                .date(LocalDate.of(2024, 1, 15))
                .rates(Map.of("USD", new BigDecimal("1.0856")))
                .build();

        when(service.getExchangeRatesOnDate(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/exchange-rates/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("EUR"));
    }

    @Test
    void convertCurrency_returns200() throws Exception {
        ConversionResult result = ConversionResult.builder()
                .fromCurrency("EUR")
                .toCurrency("USD")
                .originalAmount(BigDecimal.TEN)
                .convertedAmount(new BigDecimal("10.8560"))
                .exchangeRate(new BigDecimal("1.0856"))
                .date(LocalDate.of(2024, 1, 15))
                .build();

        when(service.convertCurrency(any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/convert-currency")
                        .param("from_currency", "EUR")
                        .param("to_currency", "USD")
                        .param("amount", "10")
                        .param("on_date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedAmount").value(10.856));
    }

    @Test
    void convertCurrency_futureDateReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/convert-currency")
                        .param("from_currency", "EUR")
                        .param("on_date", "2099-01-01"))
                .andExpect(status().isBadRequest());
    }
}
