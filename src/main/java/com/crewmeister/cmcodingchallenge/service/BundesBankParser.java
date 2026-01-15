package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.dto.xml.CodeListXml;
import com.crewmeister.cmcodingchallenge.dto.xml.ExchangeRateDataXml;
import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BundesBankParser {

    private static final String BASE_CURRENCY = "EUR";
    private final XmlMapper xmlMapper;

    public BundesBankParser() {
        this.xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<Currency> parseCurrencies(String xml) {
        try {
            CodeListXml codeList = xmlMapper.readValue(xml, CodeListXml.class);

            if (codeList.getCodes() == null) {
                return Collections.emptyList();
            }

            return codeList.getCodes().stream()
                    .filter(code -> isValidCurrencyCode(code.getId()))
                    .filter(code -> code.getEnglishName() != null)
                    .map(code -> new Currency(code.getId(), code.getEnglishName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse currencies XML", e);
            return Collections.emptyList();
        }
    }

    public List<ExchangeRate> parseExchangeRates(String xml) {
        try {
            ExchangeRateDataXml data = xmlMapper.readValue(xml, ExchangeRateDataXml.class);

            if (data.getDataSet() == null || data.getDataSet().getSeries() == null) {
                return Collections.emptyList();
            }

            List<ExchangeRate> rates = new ArrayList<>();

            for (ExchangeRateDataXml.SeriesXml series : data.getDataSet().getSeries()) {
                String currency = series.getCurrency();
                if (currency == null || series.getObservations() == null) continue;

                for (ExchangeRateDataXml.ObservationXml obs : series.getObservations()) {
                    ExchangeRate rate = parseObservation(obs, currency);
                    if (rate != null) {
                        rates.add(rate);
                    }
                }
            }
            return rates;
        } catch (Exception e) {
            log.error("Failed to parse exchange rates XML", e);
            return Collections.emptyList();
        }
    }

    private ExchangeRate parseObservation(ExchangeRateDataXml.ObservationXml obs, String targetCurrency) {
        try {
            if (obs.getDimension() == null || obs.getObsValue() == null) return null;

            String date = obs.getDimension().getValue();
            String value = obs.getObsValue().getValue();

            if (date == null || value == null) return null;

            ExchangeRate rate = new ExchangeRate();
            rate.setBaseCurrency(BASE_CURRENCY);
            rate.setTargetCurrency(targetCurrency);
            rate.setDate(LocalDate.parse(date));
            rate.setRate(new BigDecimal(value));
            return rate;
        } catch (Exception e) {
            log.warn("Failed to parse observation for targetCurrency={}", targetCurrency);
            return null;
        }
    }

    private boolean isValidCurrencyCode(String code) {
        return code != null
                && code.length() == 3
                && !code.startsWith("_")
                && code.chars().allMatch(Character::isLetter);
    }
}
