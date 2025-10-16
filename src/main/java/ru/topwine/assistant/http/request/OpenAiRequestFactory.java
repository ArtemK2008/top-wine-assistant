package ru.topwine.assistant.http.request;

import ru.topwine.assistant.model.ApiChatMessage;
import ru.topwine.assistant.model.ChatMessage;
import ru.topwine.assistant.model.enums.ChatRole;

import java.util.ArrayList;
import java.util.List;

public final class OpenAiRequestFactory {
    private OpenAiRequestFactory() {
    }

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

        for (ChatMessage message : domainMessages) {
            apiMessages.add(ApiChatMessage.builder()
                    .role(message.chatRole().apiName())
                    .content(message.content())
                    .build());
        }

        return OpenAiChatCompletionsRequest.builder()
                .model(model)
                .messages(apiMessages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
}