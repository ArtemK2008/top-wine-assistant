package ru.topwine.assistant.model;

import lombok.Builder;

@Builder
public record ApiChatMessage(String role, String content) {
}