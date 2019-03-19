package com.a1s.subscribegeneratorapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("{com.a1s.subscribegeneratorapp}, {src.main.resources}")
@EnableScheduling
public class SubscribeGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscribeGeneratorApplication.class, args);
    }
}
