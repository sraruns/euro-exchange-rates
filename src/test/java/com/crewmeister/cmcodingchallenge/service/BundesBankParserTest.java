package com.crewmeister.cmcodingchallenge.service;

import com.crewmeister.cmcodingchallenge.entity.Currency;
import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BundesBankParserTest {

    private BundesBankParser parser;

    @BeforeEach
    void setUp() {
        parser = new BundesBankParser();
    }

    @Test
    void parseCurrencies_validXml_returnsCurrencies() {
        String xml = "<Structure><Structures><Codelists><Codelist>" +
                "<Code id=\"USD\"><Name xml:lang=\"en\">US Dollar</Name></Code>" +
                "<Code id=\"GBP\"><Name xml:lang=\"en\">British Pound</Name></Code>" +
                "</Codelist></Codelists></Structures></Structure>";

        List<Currency> currencies = parser.parseCurrencies(xml);

        assertEquals(2, currencies.size());
        assertTrue(currencies.stream().anyMatch(c -> "USD".equals(c.getCode())));
    }

    @Test
    void parseCurrencies_emptyXml_returnsEmptyList() {
        List<Currency> currencies = parser.parseCurrencies("<Structure/>");
        assertTrue(currencies.isEmpty());
    }

    @Test
    void parseCurrencies_invalidXml_returnsEmptyList() {
        List<Currency> currencies = parser.parseCurrencies("not xml");
        assertTrue(currencies.isEmpty());
    }

    @Test
    void parseExchangeRates_validXml_returnsRates() {
        String xml = "<GenericData><DataSet><Series>" +
                "<SeriesKey><Value id=\"BBK_STD_CURRENCY\" value=\"USD\"/></SeriesKey>" +
                "<Obs><ObsDimension value=\"2024-01-15\"/><ObsValue value=\"1.0856\"/></Obs>" +
                "</Series></DataSet></GenericData>";

        List<ExchangeRate> rates = parser.parseExchangeRates(xml);

        assertEquals(1, rates.size());
        ExchangeRate rate = rates.get(0);
        assertEquals("EUR", rate.getBaseCurrency());
        assertEquals("USD", rate.getTargetCurrency());
        assertEquals(LocalDate.of(2024, 1, 15), rate.getDate());
        assertEquals(new BigDecimal("1.0856"), rate.getRate());
    }

    @Test
    void parseExchangeRates_emptyDataSet_returnsEmptyList() {
        String xml = "<GenericData><DataSet/></GenericData>";
        assertTrue(parser.parseExchangeRates(xml).isEmpty());
    }

    @Test
    void parseExchangeRates_invalidXml_returnsEmptyList() {
        assertTrue(parser.parseExchangeRates("invalid").isEmpty());
    }
}
