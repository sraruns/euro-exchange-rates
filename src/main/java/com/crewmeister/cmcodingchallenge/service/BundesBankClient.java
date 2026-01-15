package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.exception.BundesBankApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Component
public class BundesBankClient {

    private final WebClient webClient;
    private final String baseUrl;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    public BundesBankClient(WebClient.Builder webClientBuilder,
                            @Value("${bundesbank.api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .build();
    }

    public String fetchCurrencies() {
        log.debug("Fetching currencies from Bundesbank API");
        return executeGet("/metadata/codelist/BBK/CL_BBK_STD_CURRENCY");
    }

    public String fetchExchangeRate(String currency, LocalDate date) {
        log.debug("Fetching exchange rate for {} on {}", currency, date);
        String path = String.format("/data/BBEX3/D.%s.EUR.BB.AC.000?startPeriod=%s&endPeriod=%s",
            currency.toUpperCase(), date, date);
        return executeGet(path);
    }

    public String fetchExchangeRatesHistory(String currency, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching exchange rates for {} from {} to {}", currency, startDate, endDate);
        String path = String.format("/data/BBEX3/D..%s.BB.AC.000?startPeriod=%s&endPeriod=%s",
            currency.toUpperCase(), startDate, endDate);
        return executeGet(path);
    }

    public String fetchExchangeRatesOnDate(LocalDate date) {
        log.debug("Fetching all exchange rates on {}", date);
        String path = String.format("/data/BBEX3/D..EUR.BB.AC.000?startPeriod=%s&endPeriod=%s",
            date, date);
        return executeGet(path);
    }

    @Retryable(
        value = {WebClientResponseException.class, WebClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    @CircuitBreaker(name = "bundesbank")
    private String executeGet(String path) {
        String fullUrl = baseUrl + path;
        log.info("Executing Bundesbank API request: {}", fullUrl);
        try {
            ResponseEntity<String> response = webClient.get()
                .uri(path)
                .retrieve()
                .toEntity(String.class)
                .doOnNext(resp -> logRateLimitHeaders(resp.getHeaders()))
                .timeout(TIMEOUT)
                .block();

            return response != null ? response.getBody() : null;
        } catch (WebClientResponseException e) {
            log.error("Bundesbank API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                String retryAfter = e.getHeaders().getFirst("Retry-After");
                throw new BundesBankApiException(
                    "Rate limit exceeded. Retry after: " + retryAfter + " seconds",
                    HttpStatus.TOO_MANY_REQUESTS.value(), e);
            }
            throw new BundesBankApiException(
                "API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                e.getRawStatusCode(), e);
        } catch (Exception e) {
            log.error("Failed to fetch data from Bundesbank API", e);
            throw new BundesBankApiException("Failed to fetch data from Bundesbank API",
                HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    private void logRateLimitHeaders(HttpHeaders headers) {
        String rateLimit = headers.getFirst("X-RateLimit-Limit");
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        String reset = headers.getFirst("X-RateLimit-Reset");

        if (remaining != null) {
            log.info("Bundesbank API rate limit: {}/{}, resets at {}", remaining, rateLimit, reset);
            try {
                int remainingVal = Integer.parseInt(remaining);
                if (remainingVal < 10) {
                    log.warn("Approaching Bundesbank API rate limit! {} requests remaining", remaining);
                }
            } catch (NumberFormatException ignored) {
                // Ignore parsing errors
            }
        }
    }

}
