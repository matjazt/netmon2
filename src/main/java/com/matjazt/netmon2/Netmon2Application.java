package com.matjazt.netmon2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableIntegration
@EnableScheduling
public class Netmon2Application {

    public static void main(String[] args) {
        SpringApplication.run(Netmon2Application.class, args);
    }
}
