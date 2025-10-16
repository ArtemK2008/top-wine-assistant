package ru.topwine.assistant.service;

import reactor.core.publisher.Mono;

public interface SommelierService {
    Mono<String> advise(String userMessage);
}
