package com.matjazt.netmon2.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class NetworkConfiguration {

    Integer alertingDelay = 300; // in seconds, default 5 minutes

    String notificationEmailAddress;

    String reminderTimeOfDay;
    Integer reminderIntervalDays;

    ZoneId timezone = ZoneId.of("UTC");
}
