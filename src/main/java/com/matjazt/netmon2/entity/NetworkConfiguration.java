package com.matjazt.netmon2.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class NetworkConfiguration {

    Integer reportingInterval;
    Integer alertingDelay;

    String notificationEmailAddress;

    String reminderTimeOfDay;
    Integer reminderIntervalDays;

    ZoneId timezone = ZoneId.of("UTC");

    public boolean IsValid() {
        return reportingInterval != null
                && reportingInterval > 0
                && alertingDelay != null
                && alertingDelay > 0;
    }
}
