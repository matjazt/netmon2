package com.matjazt.netmon2.controller;

import com.matjazt.netmon2.dto.response.GreetingResponseDto;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
@Slf4j
public class GreetingController {

    @Value("${greeting.template}")
    private String template;

    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public GreetingResponseDto greeting(@RequestParam(defaultValue = "World") String name) {

        return new GreetingResponseDto(counter.incrementAndGet(), template.formatted(name));
    }
}
