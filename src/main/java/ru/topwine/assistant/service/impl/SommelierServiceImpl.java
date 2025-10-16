package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.topwine.assistant.configuration.AiProps;
import ru.topwine.assistant.guard.AdviceContext;
import ru.topwine.assistant.guard.AdviceFilterChain;
import ru.topwine.assistant.http.client.ChatClient;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.request.OpenAiRequestFactory;
import ru.topwine.assistant.model.ChatMessage;
import ru.topwine.assistant.service.SommelierService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SommelierServiceImpl implements SommelierService {
    private static final String SYSTEM_PROMPT = """
            You are a friendly sommelier for a casual wine restaurant.
            - Suggest 1–3 options with short reasoning.
            - If missing info (dish, budget, sweetness, allergens), ask one brief follow-up.
            - Do not invent availability; keep it concise.
            """;

    private final ChatClient chatClient;
    private final AiProps aiProps;
    private final AdviceFilterChain filterChain;

    @Override
    public Mono<String> advise(String userMessage) {
        AdviceContext context = new AdviceContext(userMessage);

        Optional<String> earlyReply = filterChain.run(context);
        if (earlyReply.isPresent()) {
            return Mono.just(earlyReply.get());
        }

        OpenAiChatCompletionsRequest request = OpenAiRequestFactory.build(
                aiProps.model(),
                SYSTEM_PROMPT,
                List.of(ChatMessage.user(userMessage)),
                aiProps.temperature(),
                aiProps.maxTokens()
        );
        return chatClient.chat(request);
    }
}