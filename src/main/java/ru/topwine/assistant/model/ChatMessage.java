package ru.topwine.assistant.model;

import ru.topwine.assistant.model.enums.ChatRole;

public record ChatMessage(ChatRole chatRole, String content) {

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatRole.USER, content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(ChatRole.SYSTEM, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatRole.ASSISTANT, content);
    }
}