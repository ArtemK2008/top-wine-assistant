package ru.topwine.assistant.service.session;

import ru.topwine.assistant.model.session.ConversationContext;

import java.util.Optional;

public interface ConversationStore {
    Optional<ConversationContext> get(String clientId);

    void save(String clientId, ConversationContext context);

    void clear(String clientId);
}