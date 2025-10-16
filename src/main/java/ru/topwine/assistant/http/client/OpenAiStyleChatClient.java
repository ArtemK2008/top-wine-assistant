package ru.topwine.assistant.http.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.topwine.assistant.exception.TopWineException;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.response.OpenAiChatCompletionsResponse;

@Slf4j
@RequiredArgsConstructor
public class OpenAiStyleChatClient implements ChatClient {
    private final WebClient httpClient;
    private final int timeoutSeconds;

    @Override
    public Mono<String> chat(OpenAiChatCompletionsRequest request) {
        long startNanos = System.nanoTime();

        return httpClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("<empty body>")
                                .map(body -> new TopWineException(
                                        TopWineException.Kind.PROVIDER_ERROR,
                                        "status=" + clientResponse.statusCode() + ", body=" + body
                                ))
                )
                .bodyToMono(OpenAiChatCompletionsResponse.class)
                .doOnSubscribe(s -> log.info("Запрос к LLM начат: модель={}, сообщений={}",
                        request.model(),
                        request.messages() == null ? 0 : request.messages().size()))
                .doOnSuccess(response -> {
                    long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
                    String text = (response == null)
                            ? ""
                            : response.firstMessageContent().orElse("");
                    log.info("Запрос к LLM успешен: время={} мс, символов={}", tookMs, text.length());
                })
                .doOnError(ex -> {
                    long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
                    log.error("Запрос к LLM завершился ошибкой: время={} мс, ошибка={}", tookMs, ex.toString());
                })
                .onErrorMap(this::isTimeoutThrowable, ex ->
                        new TopWineException(
                                TopWineException.Kind.PROVIDER_TIMEOUT,
                                timeoutSeconds
                        ))
                .map(response -> response == null
                        ? "Модель не вернула ответ."
                        : response.firstMessageContent().orElse("Пустой ответ от модели."));
    }

    private boolean isTimeoutThrowable(Throwable throwable) {
        if (throwable instanceof java.util.concurrent.TimeoutException) return true;

        if (throwable instanceof io.netty.handler.timeout.ReadTimeoutException) return true;

        if (throwable instanceof org.springframework.web.reactive.function.client.WebClientRequestException wcre) {
            Throwable cause = wcre.getCause();
            return cause instanceof java.util.concurrent.TimeoutException
                   || cause instanceof io.netty.handler.timeout.ReadTimeoutException;
        }
        return throwable instanceof reactor.netty.http.client.PrematureCloseException;
    }
}