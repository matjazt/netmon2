package com.matjazt.netmon2.config;

import com.matjazt.tools.TimingStatistics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimingConfig {

    @Bean
    public TimingStatistics timingStatistics() {
        return new TimingStatistics();
    }
}
