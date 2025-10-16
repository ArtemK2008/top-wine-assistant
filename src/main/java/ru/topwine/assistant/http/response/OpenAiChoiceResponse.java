package ru.topwine.assistant.http.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import ru.topwine.assistant.model.ApiChatMessage;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record OpenAiChoiceResponse(ApiChatMessage message) {
}