package ru.topwine.assistant.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProps(String provider, String baseUrl, String model, String apiKey) {
}