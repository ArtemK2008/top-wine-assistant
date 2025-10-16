package ru.topwine.assistant.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class TopWineException extends RuntimeException {

    private final Kind kind;
    private final int code;

    public TopWineException(Kind kind, Object... messageArgs) {
        super(kind.format(messageArgs));
        this.kind = kind;
        this.code = kind.getCode();
    }

    public static TopWineException fromCause(Kind kind, Throwable cause) {
        String safeDetail;
        if (cause == null) {
            safeDetail = "Unknown error";
        } else {
            String msg = cause.getMessage();
            safeDetail = (msg == null || msg.isBlank()) ? cause.getClass().getSimpleName() : msg;
        }
        return new TopWineException(kind, safeDetail);
    }

    @Getter
    public enum Kind {
        PROVIDER_TIMEOUT(1001, "Таймаут. Провайдер не ответил за %s секунд"),
        PROVIDER_ERROR(1002, "Ошибка провайдера: %s"),
        VALIDATION(1003, "Ошибка валидации: %s"),
        INTERNAL_ERROR(1999, "Внутренняя ошибка: %s");

        private final int code;
        private final String messageTemplate;

        Kind(int code, String messageTemplate) {
            this.code = code;
            this.messageTemplate = messageTemplate;
        }

        public String format(Object... args) {
            return String.format(messageTemplate, args);
        }
    }
}
