package ru.topwine.assistant.service;

public interface SommelierService {
    String advise(String clientId, String userMessage);

    default String advise(String userMessage) {
        return advise("default", userMessage);
    }
}