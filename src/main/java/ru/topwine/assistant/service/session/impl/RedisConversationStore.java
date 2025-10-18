package ru.topwine.assistant.service.session.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.topwine.assistant.model.session.ConversationContext;
import ru.topwine.assistant.service.session.ConversationStore;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisConversationStore implements ConversationStore {

    private static final String KEY_PREFIX = "conv:";
    private static final Duration TTL = Duration.ofMinutes(40);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ConversationContext> get(String clientId) {
        String json = redis.opsForValue().get(key(clientId));
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ConversationContext.class));
        } catch (Exception e) {
            clear(clientId);
            return Optional.empty();
        }
    }

    @Override
    public void save(String clientId, ConversationContext context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            redis.opsForValue().set(key(clientId), json, TTL);
        } catch (Exception e) {
            // swallow: caching must not break UX
        }
    }

    @Override
    public void clear(String clientId) {
        redis.delete(key(clientId));
    }

    private String key(String clientId) {
        return KEY_PREFIX + clientId;
    }
}
