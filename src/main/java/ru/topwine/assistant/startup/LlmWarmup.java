package ru.topwine.assistant.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.topwine.assistant.configuration.AiProps;
import ru.topwine.assistant.http.client.ChatClient;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.request.OpenAiRequestFactory;

import java.util.List;

@Component
@Profile("!test")
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
        try {
            chatClient.chat(req);
            log.info("LLM warmup done");
        } catch (Exception ex) {
            log.warn("LLM warmup failed: {}", ex.toString());
        }
    }
}
