package com.crewmeister.cmcodingchallenge.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;

import java.util.List;

@Data
public class CodeListXml {

    @JacksonXmlProperty(localName = "Code")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<CodeXml> codes;

    @Data
    public static class CodeXml {
        @JacksonXmlProperty(isAttribute = true)
        private String id;

        @JacksonXmlProperty(localName = "Name")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<NameXml> names;

        public String getEnglishName() {
            if (names == null) return null;
            return names.stream()
                    .filter(n -> "en".equals(n.getLang()))
                    .map(NameXml::getValue)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Data
    public static class NameXml {
        @JacksonXmlProperty(isAttribute = true, localName = "lang")
        private String lang;

        @JacksonXmlText
        private String value;
    }
}
