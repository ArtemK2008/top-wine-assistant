package ru.topwine.assistant.guard;

import lombok.Builder;
import ru.topwine.assistant.model.enums.FilterDecision;

import java.util.Optional;

public interface AdviceFilter {

    FilterResult apply(AdviceContext context);

    @Builder
    record FilterResult(FilterDecision decision, Optional<String> immediateReply) {
        public static FilterResult continueChain() {
            return new FilterResult(FilterDecision.CONTINUE, Optional.empty());
        }

        public static FilterResult respond(String reply) {
            return new FilterResult(FilterDecision.RESPOND_IMMEDIATELY, Optional.ofNullable(reply));
        }
    }
}