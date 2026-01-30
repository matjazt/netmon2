package com.matjazt.tools;

import io.github.cdimascio.dotenv.Dotenv;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class SpringBootTools {
    public static void loadDotenvIntoSpringEnvironment(ConfigurableEnvironment environment) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();

        Map<String, Object> dotenvMap = new HashMap<>();
        dotenv.entries().forEach(entry -> dotenvMap.put(entry.getKey(), entry.getValue()));

        environment.getPropertySources().addFirst(new MapPropertySource("dotenv", dotenvMap));
    }
}
