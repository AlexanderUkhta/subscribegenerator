package com.a1s.subscribegeneratorapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
public class MsisdnAndExcelProperties {

    private List<String> msisdns;
    private List<String> excelColumns;

    public void setMsisdns(List<String> msisdns) {
        this.msisdns = msisdns;

    }
    public List<String> getMsisdnList() {
        return this.msisdns;

    }

    public void setExcelColumns(List<String> excelColumns) {
        this.excelColumns = excelColumns;

    }
    public List<String> getExcelColumns() {
        return this.excelColumns;

    }


}
