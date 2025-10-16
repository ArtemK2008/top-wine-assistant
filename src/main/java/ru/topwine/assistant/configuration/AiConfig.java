package ru.topwine.assistant.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.topwine.assistant.http.client.ChatClient;
import ru.topwine.assistant.http.client.OpenAiStyleChatClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class AiConfig {

    private final AiProps aiProps;

    @Bean
    @Qualifier("llmWebClient")
    public WebClient llmWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(aiProps.timeoutSeconds()))
                .wiretap(true);

        return builder
                .baseUrl(aiProps.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers -> {
                    if (aiProps.apiKey() != null && !aiProps.apiKey().isBlank()) {
                        headers.setBearerAuth(aiProps.apiKey());
                    }
                })
                .build();
    }

    @Bean
    public ChatClient chatClient(@Qualifier("llmWebClient") WebClient llmWebClient) {
        return new OpenAiStyleChatClient(llmWebClient, aiProps.timeoutSeconds());
    }
}