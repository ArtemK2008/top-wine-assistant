package ru.topwine.assistant.http.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.netty.http.client.PrematureCloseException;
import ru.topwine.assistant.exception.TopWineException;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.response.OpenAiChatCompletionsResponse;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
public class OpenAiStyleChatClient implements ChatClient {

    private final WebClient httpClient;
    private final int timeoutSeconds;

    @Override
    public String chat(OpenAiChatCompletionsRequest request) {
        long startNanos = System.nanoTime();
        try {
            log.info("Запрос к LLM начат: модель={}, сообщений={}",
                    request.model(),
                    request.messages() == null ? 0 : request.messages().size());

            OpenAiChatCompletionsResponse response = httpClient
                    .post()
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
                    .block();

            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
            String text = (response == null) ? "" : response.firstMessageContent().orElse("");
            log.info("Запрос к LLM успешен: время={} мс, символов={}", tookMs, text.length());

            return response == null
                    ? "Модель не вернула ответ."
                    : response.firstNonBlankMessageContent().orElse("Пустой ответ от модели.");

        } catch (Throwable ex) {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.error("Запрос к LLM завершился ошибкой: время={} мс, ошибка={}", tookMs, ex.toString());

            if (ex instanceof TopWineException te) {
                throw te;
            }
            if (isTimeoutThrowable(ex)) {
                throw new TopWineException(TopWineException.Kind.PROVIDER_TIMEOUT, timeoutSeconds);
            }
            throw new TopWineException(TopWineException.Kind.PROVIDER_ERROR, ex.getMessage());
        }
    }

    private boolean isTimeoutThrowable(Throwable t) {
        // direct matches
        if (t instanceof TimeoutException) return true;
        if (t instanceof HttpTimeoutException) return true;
        if (t instanceof SocketTimeoutException) return true;
        if (t instanceof io.netty.handler.timeout.ReadTimeoutException) return true;

        if (t instanceof WebClientRequestException wcre) {
            Throwable cause = wcre.getCause();
            return cause instanceof TimeoutException
                   || cause instanceof HttpTimeoutException
                   || cause instanceof SocketTimeoutException
                   || cause instanceof io.netty.handler.timeout.ReadTimeoutException;
        }

        return t instanceof PrematureCloseException;
    }
}
