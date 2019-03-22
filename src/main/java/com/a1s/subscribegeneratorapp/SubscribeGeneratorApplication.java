package com.a1s.subscribegeneratorapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan//({"com.a1s.subscribegeneratorapp", "src.main.resources"})
public class SubscribeGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscribeGeneratorApplication.class, args);
    }
}
