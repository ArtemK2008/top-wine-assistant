package ru.topwine.assistant.model.session;

import ru.topwine.assistant.model.wine.AvailableWineFilter;

import java.time.Instant;
import java.util.List;

public record ConversationContext(
        List<String> recentUserMessages,
        List<String> recentAssistantReplies,
        List<Long> lastShownWineStockIds,
        List<Long> lastShownDishIds,
        AvailableWineFilter lastUsedFilter,
        Instant updatedAt
) {
    public static ConversationContext empty() {
        return new ConversationContext(List.of(), List.of(), List.of(), List.of(), null, Instant.now());
    }
}
