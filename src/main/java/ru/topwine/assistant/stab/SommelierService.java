package ru.topwine.assistant.stab;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.topwine.assistant.ai.ChatClient;
import ru.topwine.assistant.configuration.AiProps;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.request.OpenAiRequestFactory;
import ru.topwine.assistant.model.ChatMessage;

import java.util.List;

@Service
public class SommelierService {

    private static final String SYSTEM_PROMPT = """
            You are a friendly sommelier for a casual wine restaurant.
            - Suggest 1–3 options with short reasoning.
            - If missing info (dish, budget, sweetness, allergens), ask one brief follow-up.
            - Do not invent availability; keep it concise.
            """;

    private final ChatClient chatClient;
    private final AiProps aiProps;

    public SommelierService(ChatClient chatClient, AiProps aiProps) {
        this.chatClient = chatClient;
        this.aiProps = aiProps;
    }

    public Mono<String> advise(String userMessage) {
        OpenAiChatCompletionsRequest request = OpenAiRequestFactory.build(
                aiProps.model(),
                SYSTEM_PROMPT,
                List.of(ChatMessage.user(userMessage)),
                0.5,
                300
        );
        return chatClient.chat(request);
    }
}