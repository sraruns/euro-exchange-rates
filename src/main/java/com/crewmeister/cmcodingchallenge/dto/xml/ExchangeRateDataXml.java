package com.crewmeister.cmcodingchallenge.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

@Data
public class ExchangeRateDataXml {

    @JacksonXmlProperty(localName = "DataSet")
    private DataSetXml dataSet;

    @Data
    public static class DataSetXml {
        @JacksonXmlProperty(localName = "Series")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<SeriesXml> series;
    }

    @Data
    public static class SeriesXml {
        @JacksonXmlProperty(localName = "SeriesKey")
        private SeriesKeyXml seriesKey;

        @JacksonXmlProperty(localName = "Obs")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ObservationXml> observations;

        public String getCurrency() {
            if (seriesKey == null || seriesKey.getValues() == null) return null;
            return seriesKey.getValues().stream()
                    .filter(v -> "BBK_STD_CURRENCY".equals(v.getId()))
                    .map(ValueXml::getValue)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Data
    public static class SeriesKeyXml {
        @JacksonXmlProperty(localName = "Value")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ValueXml> values;
    }

    @Data
    public static class ValueXml {
        @JacksonXmlProperty(isAttribute = true)
        private String id;

        @JacksonXmlProperty(isAttribute = true)
        private String value;
    }

    @Data
    public static class ObservationXml {
        @JacksonXmlProperty(localName = "ObsDimension")
        private ObsDimensionXml dimension;

        @JacksonXmlProperty(localName = "ObsValue")
        private ObsValueXml obsValue;
    }

    @Data
    public static class ObsDimensionXml {
        @JacksonXmlProperty(isAttribute = true)
        private String value;
    }

    @Data
    public static class ObsValueXml {
        @JacksonXmlProperty(isAttribute = true)
        private String value;
    }
}
