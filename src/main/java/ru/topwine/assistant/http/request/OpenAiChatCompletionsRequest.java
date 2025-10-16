package ru.topwine.assistant.http.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import ru.topwine.assistant.model.ApiChatMessage;

import java.util.List;

@Builder
public record OpenAiChatCompletionsRequest(String model, List<ApiChatMessage> messages, Double temperature,
                                           @JsonProperty("max_tokens") Integer maxTokens) {
}