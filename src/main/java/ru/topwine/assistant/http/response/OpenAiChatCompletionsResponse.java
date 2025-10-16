package ru.topwine.assistant.http.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record OpenAiChatCompletionsResponse(List<OpenAiChoiceResponse> choices) {

    public Optional<String> firstMessageContent() {
        if (choices == null || choices.isEmpty()) return Optional.empty();
        OpenAiChoiceResponse first = choices.getFirst();
        if (first == null || first.message() == null) return Optional.empty();
        return Optional.ofNullable(first.message().content());
    }
}