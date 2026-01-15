package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.dto.ConversionResult;
import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.exception.InvalidCurrencyException;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private BundesBankClient client;
    @Mock private BundesBankParser parser;
    @Mock private CurrencyRepository currencyRepository;
    @Mock private ExchangeRateRepository exchangeRateRepository;
    @Mock private ExchangeRateMapper mapper;

    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        when(currencyRepository.findAll()).thenReturn(List.of(
                new Currency("USD", "US Dollar"),
                new Currency("GBP", "British Pound"),
                new Currency("EUR", "Euro")
        ));
        service = new ExchangeRateService(client, parser, currencyRepository, exchangeRateRepository, mapper);
        service.init();
    }

    @Test
    void getCurrencies_returnsCurrencyList() {
        List<Currency> result = service.getCurrencies();
        assertEquals(3, result.size());
    }

    @Test
    void validateCurrency_validCurrency_returnsNormalized() {
        assertEquals("USD", service.validateCurrency("usd"));
        assertEquals("GBP", service.validateCurrency("GBP"));
    }

    @Test
    void validateCurrency_invalidCurrency_throwsException() {
        assertThrows(InvalidCurrencyException.class, () -> service.validateCurrency("XYZ"));
    }

    @Test
    void getExchangeRate_fromDb_returnsRate() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        ExchangeRate expected = createRate("USD", "1.0856", date);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate("EUR", "USD", date))
                .thenReturn(Optional.of(expected));

        ExchangeRate result = service.getExchangeRate("USD", date);

        assertEquals(expected, result);
        verify(client, never()).fetchExchangeRate(any(), any());
    }

    @Test
    void getExchangeRate_fromApi_returnsAndSaves() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        ExchangeRate expected = createRate("USD", "1.0856", date);

        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate("EUR", "USD", date))
                .thenReturn(Optional.empty());
        when(client.fetchExchangeRate("USD", date)).thenReturn("<xml/>");
        when(parser.parseExchangeRates("<xml/>")).thenReturn(List.of(expected));

        ExchangeRate result = service.getExchangeRate("USD", date);

        assertEquals(expected, result);
    }

    @Test
    void getExchangeRate_notFound_throwsException() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate("EUR", "USD", date))
                .thenReturn(Optional.empty());
        when(client.fetchExchangeRate("USD", date)).thenReturn("<xml/>");
        when(parser.parseExchangeRates("<xml/>")).thenReturn(List.of());

        assertThrows(ExchangeRateNotFoundException.class, () -> service.getExchangeRate("USD", date));
    }

    @Test
    void convertCurrency_eurToUsd_multiplies() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        ExchangeRate rate = createRate("USD", "1.0856", date);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate("EUR", "USD", date))
                .thenReturn(Optional.of(rate));

        ConversionResult result = service.convertCurrency("EUR", "USD", BigDecimal.TEN, date);

        assertEquals("EUR", result.getFromCurrency());
        assertEquals("USD", result.getToCurrency());
        assertEquals(new BigDecimal("10.8560"), result.getConvertedAmount());
    }

    @Test
    void convertCurrency_usdToEur_divides() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        ExchangeRate rate = createRate("USD", "1.0856", date);
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate("EUR", "USD", date))
                .thenReturn(Optional.of(rate));

        ConversionResult result = service.convertCurrency("USD", "EUR", BigDecimal.TEN, date);

        assertEquals("USD", result.getFromCurrency());
        assertEquals("EUR", result.getToCurrency());
        assertEquals(new BigDecimal("9.2115"), result.getConvertedAmount());
    }

    private ExchangeRate createRate(String targetCurrency, String rateValue, LocalDate date) {
        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency("EUR");
        rate.setTargetCurrency(targetCurrency);
        rate.setRate(new BigDecimal(rateValue));
        rate.setDate(date);
        return rate;
    }
}
