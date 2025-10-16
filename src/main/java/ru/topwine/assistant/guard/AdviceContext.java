package ru.topwine.assistant.guard;

import lombok.Getter;
import lombok.Setter;

@Getter
public class AdviceContext {
    private final String originalMessage;
    @Setter
    private String sanitizedMessage;

    public AdviceContext(String originalMessage) {
        this.originalMessage = originalMessage;
        this.sanitizedMessage = originalMessage;
    }

}