package com.a1s.subscribegeneratorapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@PropertySource("classpath:msisdn.properties")
@ConfigurationProperties(prefix = "app")
public class ReadExcelProperties {
    private List<String> excelList = new ArrayList<>();

    public List<String> getMsisdnList() {
        return this.excelList;
    }
}
