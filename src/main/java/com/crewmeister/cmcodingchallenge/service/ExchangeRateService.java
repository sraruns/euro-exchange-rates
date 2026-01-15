package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.dto.ConversionResult;
import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.repository.ExchangeRateRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final BundesBankClient client;
    private final BundesBankParser parser;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    // Caffeine caches
    private Cache<String, List<Currency>> currencyCache;
    private Cache<String, ExchangeRate> rateCache;

    @PostConstruct
    public void initCaches() {
        // Currency cache: long TTL since currencies rarely change
        this.currencyCache =
                Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).maximumSize(1).build();

        // Rate cache: shorter TTL, keyed by "currency:date"
        this.rateCache =
                Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(10000).build();
    }

    public List<Currency> getCurrencies() {
        return currencyCache.get("all", key -> {
            // Check H2
            List<Currency> dbCurrencies = currencyRepository.findAll();
            if (!dbCurrencies.isEmpty()) {
                log.debug("Loaded {} currencies from H2", dbCurrencies.size());
                return dbCurrencies;
            }

            // Fetch from API
            log.info("Fetching currencies from Bundesbank API");
            String xml = client.fetchCurrencies();
            List<Currency> currencies = parser.parseCurrencies(xml);

            // Save to H2
            currencyRepository.saveAll(currencies);
            log.info("Saved {} currencies to H2", currencies.size());

            return currencies;
        });
    }


    @Transactional
    public List<ExchangeRate> getExchangeRates(
            String currency, LocalDate startDate, LocalDate endDate) {
        // Check H2
        List<ExchangeRate> dbRates =
                exchangeRateRepository.findByCurrencyAndDateBetween(
                        currency.toUpperCase(), startDate, endDate);

        if (!dbRates.isEmpty()) {
            log.debug("Found {} rates for {} in H2", dbRates.size(), currency);
            return dbRates;
        }

        // Fetch from API
        log.info(
                "Fetching exchange rates for {} from {} to {} from Bundesbank API",
                currency,
                startDate,
                endDate);
        String xml = client.fetchExchangeRates(currency, startDate, endDate);
        List<ExchangeRate> rates = parser.parseExchangeRates(xml);

        if (!rates.isEmpty()) {
            exchangeRateRepository.saveAll(rates);
            rates.forEach(rate -> rateCache.put(cacheKey(rate.getCurrency(), rate.getDate()), rate));
        }

        return rates;
    }

    @Transactional
    public List<ExchangeRate> getExchangeRatesOnDate(String currency, LocalDate date) {
        List<ExchangeRate> rates;

        // Check H2 first
        List<ExchangeRate> dbRates = exchangeRateRepository.findByDate(date);
        if (!dbRates.isEmpty()) {
            log.debug("Found {} rates for {} in H2", dbRates.size(), date);
            rates = dbRates;
        } else {
            // Fetch from API
            log.info("Fetching exchange rates for {} from Bundesbank API", date);
            String xml = client.fetchExchangeRatesOnDate(date);
            rates = parser.parseExchangeRates(xml);

            if (!rates.isEmpty()) {
                exchangeRateRepository.saveAll(rates);
                rates.forEach(r -> rateCache.put(cacheKey(r.getCurrency(), r.getDate()), r));
            }
        }

        // Filter by currency if specified
        return rates.stream()
                .filter(r -> currency == null || currency.isBlank() || r.getCurrency().equalsIgnoreCase(currency))
                .collect(Collectors.toList());
    }

    @Transactional
    public ExchangeRate getExchangeRate(String currency, LocalDate date) {
        String cacheKey = cacheKey(currency, date);

        // Check cache
        ExchangeRate cached = rateCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for {}", cacheKey);
            return cached;
        }

        // Check H2
        Optional<ExchangeRate> dbRate =
                exchangeRateRepository.findByCurrencyAndDate(currency.toUpperCase(), date);
        if (dbRate.isPresent()) {
            log.debug("H2 hit for {}", cacheKey);
            rateCache.put(cacheKey, dbRate.get());
            return dbRate.get();
        }

        // Fetch from API
        log.info("Fetching exchange rate for {} on {} from Bundesbank API", currency, date);
        String xml = client.fetchExchangeRate(currency, date);
        List<ExchangeRate> rates = parser.parseExchangeRates(xml);

        if (rates.isEmpty()) {
            throw new ExchangeRateNotFoundException(currency, date);
        }

        ExchangeRate rate = rates.get(0);

        // Save to H2
        exchangeRateRepository.save(rate);
        rateCache.put(cacheKey, rate);

        return rate;
    }

    public ConversionResult convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount, LocalDate date) {
        ExchangeRate rate = getExchangeRate(fromCurrency, date);
        BigDecimal convertedAmount = amount.divide(rate.getRate(), 4, RoundingMode.HALF_UP);

        return ConversionResult.builder()
                .fromCurrency(fromCurrency.toUpperCase())
                .toCurrency(toCurrency.toUpperCase())
                .originalAmount(amount)
                .convertedAmount(convertedAmount)
                .exchangeRate(rate.getRate())
                .date(date)
                .build();
    }

    private String cacheKey(String currency, LocalDate date) {
        return currency.toUpperCase() + ":" + date;
    }
}
