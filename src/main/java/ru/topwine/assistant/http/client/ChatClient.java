package ru.topwine.assistant.http.client;

import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;

public interface ChatClient {
    String chat(OpenAiChatCompletionsRequest request);
}