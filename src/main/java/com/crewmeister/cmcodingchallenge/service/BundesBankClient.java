package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.exception.BundesBankApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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

    private String executeGet(String path) {
        String fullUrl = baseUrl + path;
        log.info("Executing Bundesbank API request: {}", fullUrl);
        try {
            return webClient.get()
                .uri(path)
                .retrieve()
//                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
//                    response -> response.bodyToMono(String.class)
//                        .flatMap(body -> Mono.error(new BundesBankApiException(
//                            "API error: " + response.statusCode() + " - " + body,
//                            response.statusCode().value()))))
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .block();
        } catch (WebClientResponseException e) {
            log.error("Bundesbank API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BundesBankApiException("Failed to fetch data from Bundesbank API", e.getRawStatusCode(), e);
        } catch (Exception e) {
            log.error("Failed to fetch data from Bundesbank API", e);
            throw new BundesBankApiException("Failed to fetch data from Bundesbank API",
                HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }
}
