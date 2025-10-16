package ru.topwine.assistant.ai.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.topwine.assistant.ai.ChatClient;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.response.OpenAiChatCompletionsResponse;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class OpenAiStyleChatClient implements ChatClient {
    private final WebClient httpClient;
    private final String apiKey;

    @Override
    public Mono<String> chat(OpenAiChatCompletionsRequest request) {
        long startNanos = System.nanoTime();

        return httpClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isBlank()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("<empty body>")
                                .map(body -> {
                                    String msg = "Provider error: status=" + resp.statusCode() + ", body=" + body;
                                    log.error(msg);
                                    return new RuntimeException(msg);
                                })
                )
                .bodyToMono(OpenAiChatCompletionsResponse.class)
                .timeout(Duration.ofSeconds(20))
                .map(response -> {
                    String text = (response == null)
                            ? "No response from model."
                            : response.firstMessageContent().orElse("Empty response from model.");
                    long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
                    log.info("LLM call success: took={} ms, chars={}", tookMs, text.length());
                    return text;
                })
                .doOnSubscribe(s -> log.info("LLM call start: model={}, messages={}",
                        request.model(),
                        request.messages() != null ? request.messages().size() : 0))
                .doOnError(ex -> {
                    long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
                    log.error("LLM call failed: took={} ms, error={}", tookMs, ex.toString());
                });
    }
}