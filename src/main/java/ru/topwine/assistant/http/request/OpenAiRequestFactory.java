package ru.topwine.assistant.http.request;

import ru.topwine.assistant.model.ApiChatMessage;
import ru.topwine.assistant.model.ChatMessage;
import ru.topwine.assistant.model.enums.ChatRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OpenAiRequestFactory {

    public static final String KEEP_ALIVE_DEFAULT = "5m";
    public static final int MAX_THREADS_CAP = 16;

    private OpenAiRequestFactory() {
    }

    private static final List<String> STOP_SEQUENCES = List.of(
            "\nПользователь:",
            "\nUser:",
            "Обоснование:",
            "Reasoning:",
            "Шаги:"
    );

    public static OpenAiChatCompletionsRequest build(
            String model,
            String systemPrompt,
            List<ChatMessage> domainMessages,
            double temperature,
            int maxTokens
    ) {
        List<ApiChatMessage> apiMessages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            apiMessages.add(ApiChatMessage.builder()
                    .role(ChatRole.SYSTEM.apiName())
                    .content(systemPrompt)
                    .build());
        }

        if (domainMessages != null) {
            for (ChatMessage message : domainMessages) {
                if (message != null) {
                    apiMessages.add(ApiChatMessage.builder()
                            .role(message.chatRole().apiName())
                            .content(message.content())
                            .build());
                }
            }
        }

        Map<String, Object> options = new HashMap<>();
        options.put("keep_alive", KEEP_ALIVE_DEFAULT);
        options.put("num_thread", computeNumThreads());

        return OpenAiChatCompletionsRequest.builder()
                .model(model)
                .messages(apiMessages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .stop(STOP_SEQUENCES)
                .options(options)
                .build();
    }

    private static int computeNumThreads() {
        int logicalCpus = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(1, logicalCpus - 1);
        return Math.min(threads, MAX_THREADS_CAP);
    }
}
