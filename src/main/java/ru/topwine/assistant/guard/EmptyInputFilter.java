package ru.topwine.assistant.guard;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class EmptyInputFilter implements AdviceFilter {

    @Override
    public FilterResult apply(AdviceContext context) {
        String message = context.getSanitizedMessage();
        if (message == null || message.isBlank()) {
            return FilterResult.respond(
                    "Tell me your dish or preferences (e.g., steak, budget, sweetness, allergens)."
            );
        }
        return FilterResult.continueChain();
    }
}