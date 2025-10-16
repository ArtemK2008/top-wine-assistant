package ru.topwine.assistant.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int code,
        String message,
        String path,
        String requestId
) {
}