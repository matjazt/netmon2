package com.matjazt.netmon2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
public class Netmon2Application {

    public static void main(String[] args) {
        SpringApplication.run(Netmon2Application.class, args);
    }
}
