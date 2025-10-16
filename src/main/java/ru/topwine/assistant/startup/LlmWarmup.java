package ru.topwine.assistant.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.topwine.assistant.configuration.AiProps;
import ru.topwine.assistant.http.client.ChatClient;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.request.OpenAiRequestFactory;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
class LlmWarmup {
    private final ChatClient chatClient;
    private final AiProps ai;

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        OpenAiChatCompletionsRequest req = OpenAiRequestFactory.build(
                ai.model(), "You are a system.", List.of(), 0.0, 1
        );
        chatClient.chat(req)
                .timeout(Duration.ofSeconds(60))
                .subscribe(
                        ok -> log.info("LLM warmup ok"),
                        err -> log.warn("LLM warmup failed: {}", err.toString())
                );
    }
}