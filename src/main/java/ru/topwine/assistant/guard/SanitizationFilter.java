package ru.topwine.assistant.guard;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class SanitizationFilter implements AdviceFilter {

    private static final int MAX_CHARS = 2000;

    @Override
    public FilterResult apply(AdviceContext context) {
        String raw = context.getOriginalMessage();
        String trimmed = (raw == null) ? "" : raw.strip();
        if (trimmed.length() > MAX_CHARS) {
            trimmed = trimmed.substring(0, MAX_CHARS) + " …";
        }
        context.setSanitizedMessage(trimmed);
        return FilterResult.continueChain();
    }
}