package ru.topwine.assistant.model;

import jakarta.validation.constraints.NotBlank;

public final class ChatDto {
    private ChatDto() {}

    public record ChatRequest(@NotBlank String message) {}
    public record ChatResponse(String reply) {}
}