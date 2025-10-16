package ru.topwine.assistant.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProps(
        String provider,
        String baseUrl,
        String model,
        String apiKey,
        @Min(1) @Max(300) int timeoutSeconds,
        @Min(1) @Max(8192) int maxTokens,
        @Min(0) @Max(2) double temperature
) {
}