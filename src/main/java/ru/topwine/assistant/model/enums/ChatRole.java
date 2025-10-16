package ru.topwine.assistant.model.enums;

public enum ChatRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    private final String apiName;

    ChatRole(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}