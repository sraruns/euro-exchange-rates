package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.dto.ConversionResult;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesHistoryResponse;
import com.crewmeister.cmcodingchallenge.dto.ExchangeRatesOnDateResponse;
import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.exception.InvalidCurrencyException;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.repository.ExchangeRateRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String BASE_CURRENCY = "EUR";
    private static final LocalDate MIN_DATE = LocalDate.of(2020, 1, 1);
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final BundesBankClient client;
    private final BundesBankParser parser;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    private Cache<String, ExchangeRatesHistoryResponse> historyCache;
    private volatile Set<String> validCurrencyCodes;

    @PostConstruct
    public void init() {
        this.historyCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100)
                .build();

        loadCurrencies();
        refreshCurrencyCodes();
    }


    public List<Currency> getCurrencies() {
        List<Currency> currencies = currencyRepository.findAll();
        if (currencies.isEmpty()) {
            log.error("No currencies found in database");
            throw new RuntimeException("Error loading currencies from the service");
        }
        return currencies;
    }


    @Transactional
    public ExchangeRatesHistoryResponse getExchangeRatesHistory(
            String targetCurrency, LocalDate startDate, LocalDate endDate, int page, int size) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
        if (startDate.isBefore(MIN_DATE)) {
            throw new IllegalArgumentException("from_date cannot be before " + MIN_DATE);
        }

        String validTargetCurrency = validateCurrency(targetCurrency);
        String cacheKey = historyKey(startDate, endDate, page, size);

        ExchangeRatesHistoryResponse cached = historyCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for history {}", cacheKey);
            return cached;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // Check if H2 has complete data for the requested range
        boolean h2HasCompleteData = hasCompleteDataForRange(startDate, endDate);

        if (!h2HasCompleteData) {
            log.info("Fetching exchange rates for {} from {} to {} from Bundesbank API",
                    validTargetCurrency, startDate, endDate);
            String xml = client.fetchExchangeRatesHistory(validTargetCurrency, startDate, endDate);
            List<ExchangeRate> rates = parser.parseExchangeRates(xml);
            if (!rates.isEmpty()) {
                saveRatesIfNotExist(rates);
            }
        } else {
            log.debug("H2 has complete data for range {} to {}", startDate, endDate);
        }

        // Query DB with pagination by dates
        Page<LocalDate> datesPage = exchangeRateRepository.findDistinctDatesByBaseCurrencyAndDateBetween(
                BASE_CURRENCY, startDate, endDate, pageable);

        List<ExchangeRate> ratesForDates = datesPage.hasContent()
                ? exchangeRateRepository.findByBaseCurrencyAndDateIn(BASE_CURRENCY, datesPage.getContent())
                : Collections.emptyList();

        ExchangeRatesHistoryResponse response = buildHistoryResponse(startDate, endDate, ratesForDates, datesPage);
        historyCache.put(cacheKey, response);
        return response;
    }

    private String historyKey(LocalDate start, LocalDate end, int page, int size) {
        return start + ":" + end + ":" + page + ":" + size;
    }

    /**
     * Check if H2 has complete data for the date range.
     * Uses 4-day tolerance to account for weekends/holidays.
     */
    private boolean hasCompleteDataForRange(LocalDate startDate, LocalDate endDate) {
        Optional<LocalDate> minDate = exchangeRateRepository.findMinDateByBaseCurrencyAndDateBetween(
                BASE_CURRENCY, startDate, endDate);
        Optional<LocalDate> maxDate = exchangeRateRepository.findMaxDateByBaseCurrencyAndDateBetween(
                BASE_CURRENCY, startDate, endDate);

        if (minDate.isEmpty() || maxDate.isEmpty()) {
            return false;
        }

        // Allow 4-day tolerance for weekends/holidays
        long startDiff = Math.abs(ChronoUnit.DAYS.between(startDate, minDate.get()));
        long endDiff = Math.abs(ChronoUnit.DAYS.between(endDate, maxDate.get()));

        return startDiff <= 4 && endDiff <= 4;
    }


    @Transactional
    public ExchangeRatesOnDateResponse getExchangeRatesOnDate(String targetCurrency, LocalDate date) {
        String validTargetCurrency = validateCurrency(targetCurrency);

        List<ExchangeRate> dbRates = exchangeRateRepository.findByBaseCurrencyAndDate(BASE_CURRENCY, date);

        List<ExchangeRate> rates;
        if (!dbRates.isEmpty()) {
            log.debug("Found {} rates for {} on {} in H2", dbRates.size(), BASE_CURRENCY, date);
            rates = dbRates;
        } else {
            log.info("Fetching exchange rates for {} on {} from Bundesbank API", BASE_CURRENCY, date);
            String xml = client.fetchExchangeRatesOnDate(date);
            rates = parser.parseExchangeRates(xml);

            if (!rates.isEmpty()) {
                saveRatesIfNotExist(rates);
            }
        }

        return getExchangeRatesOnDateResponse(date, rates, validTargetCurrency);
    }



    @Transactional
    public ExchangeRate getExchangeRate(String targetCurrency, LocalDate date) {
        String validTargetCurrency = validateCurrency(targetCurrency);

        Optional<ExchangeRate> dbRate = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrencyAndDate(BASE_CURRENCY, validTargetCurrency, date);

        if (dbRate.isPresent()) {
            log.debug("H2 hit for {}/{} on {}", BASE_CURRENCY, validTargetCurrency, date);
            return dbRate.get();
        }

        log.info("Fetching exchange rate for {}/{} on {} from Bundesbank API", BASE_CURRENCY, validTargetCurrency, date);
        String xml = client.fetchExchangeRate(validTargetCurrency, date);
        List<ExchangeRate> rates = parser.parseExchangeRates(xml);

        if (rates.isEmpty()) {
            throw new ExchangeRateNotFoundException(validTargetCurrency, date);
        }

        ExchangeRate rate = rates.get(0);
        saveRateIfNotExist(rate);
        return rate;
    }

    public ConversionResult convertCurrency(
            String fromCurrency, String toCurrency, BigDecimal amount, LocalDate date) {

        String validFromCurrency = fromCurrency.toUpperCase();
        String validToCurrency = toCurrency.toUpperCase();

        BigDecimal convertedAmount;
        BigDecimal exchangeRate;

        if (BASE_CURRENCY.equals(validFromCurrency)) {
            // EUR -> targetCurrency: multiply by rate
            validateCurrency(validToCurrency);
            ExchangeRate rate = getExchangeRate(validToCurrency, date);
            exchangeRate = rate.getRate();
            convertedAmount = amount.multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);
        } else if (BASE_CURRENCY.equals(validToCurrency)) {
            // targetCurrency -> EUR: divide by rate
            validateCurrency(validFromCurrency);
            ExchangeRate rate = getExchangeRate(validFromCurrency, date);
            exchangeRate = rate.getRate();
            convertedAmount = amount.divide(exchangeRate, 4, RoundingMode.HALF_UP);
        } else {
            // Cross-rate: fromCurrency -> EUR -> toCurrency
            validateCurrency(validFromCurrency);
            validateCurrency(validToCurrency);
            ExchangeRate fromRate = getExchangeRate(validFromCurrency, date);
            ExchangeRate toRate = getExchangeRate(validToCurrency, date);
            BigDecimal amountInEur = amount.divide(fromRate.getRate(), 6, RoundingMode.HALF_UP);
            convertedAmount = amountInEur.multiply(toRate.getRate()).setScale(4, RoundingMode.HALF_UP);
            exchangeRate = toRate.getRate().divide(fromRate.getRate(), 6, RoundingMode.HALF_UP);
        }

        return ConversionResult.builder()
                .fromCurrency(validFromCurrency)
                .toCurrency(validToCurrency)
                .originalAmount(amount)
                .convertedAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .date(date)
                .build();
    }

    private void saveRateIfNotExist(ExchangeRate rate) {
        if (!exchangeRateRepository.existsByBaseCurrencyAndTargetCurrencyAndDate(
                rate.getBaseCurrency(), rate.getTargetCurrency(), rate.getDate())) {
            exchangeRateRepository.save(rate);
        }
    }

    private void saveRatesIfNotExist(List<ExchangeRate> rates) {
        rates.forEach(this::saveRateIfNotExist);
    }

    private void refreshCurrencyCodes() {
        this.validCurrencyCodes = getCurrencies().stream()
                .map(Currency::getCode)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void loadCurrencies() {
        List<Currency> dbCurrencies = currencyRepository.findAll();
        if (dbCurrencies.isEmpty()) {
            log.info("Fetching currencies from Bundesbank API");
            String xml = client.fetchCurrencies();
            List<Currency> currencies = parser.parseCurrencies(xml);
            currencyRepository.saveAll(currencies);
            log.info("Saved {} currencies to H2", currencies.size());
        }
    }

    public String validateCurrency(String currency) {
        String normalized = currency.toUpperCase();
        if (!validCurrencyCodes.contains(normalized)) {
            throw new InvalidCurrencyException(currency);
        }
        return normalized;
    }


    private static ExchangeRatesHistoryResponse buildHistoryResponse(
            LocalDate startDate, LocalDate endDate, List<ExchangeRate> rates, Page<LocalDate> datesPage) {
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
                .baseCurrency(BASE_CURRENCY)
                .rates(ratesMap)
                .page(datesPage.getNumber())
                .size(datesPage.getSize())
                .totalElements(datesPage.getTotalElements())
                .totalPages(datesPage.getTotalPages())
                .build();
    }

    private static ExchangeRatesOnDateResponse getExchangeRatesOnDateResponse(LocalDate date, List<ExchangeRate> rates, String targetCurrency) {
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
                .baseCurrency(BASE_CURRENCY)
                .date(date)
                .rates(ratesMap)
                .message(message)
                .build();
    }
}
