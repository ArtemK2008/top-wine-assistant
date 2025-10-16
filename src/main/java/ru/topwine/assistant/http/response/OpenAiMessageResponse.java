package ru.topwine.assistant.http.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record OpenAiMessageResponse(String role, String content) {
}