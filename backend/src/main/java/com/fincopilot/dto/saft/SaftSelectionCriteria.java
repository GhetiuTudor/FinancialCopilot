package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class SaftSelectionCriteria {

    @JacksonXmlProperty(localName = "PeriodStart", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String periodStart;

    @JacksonXmlProperty(localName = "PeriodStartYear", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String periodStartYear;

    @JacksonXmlProperty(localName = "PeriodEnd", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String periodEnd;

    @JacksonXmlProperty(localName = "PeriodEndYear", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String periodEndYear;
}
