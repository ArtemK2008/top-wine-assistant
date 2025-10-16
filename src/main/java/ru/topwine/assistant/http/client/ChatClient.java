package ru.topwine.assistant.http.client;

import reactor.core.publisher.Mono;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;

public interface ChatClient {
    Mono<String> chat(OpenAiChatCompletionsRequest request);
}