package ru.topwine.assistant.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.OffsetDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TopWineException.class)
    public ResponseEntity<ErrorResponse> handleTopWine(TopWineException exception, ServerWebExchange exchange) {
        HttpStatus httpStatus = mapStatus(exception.getKind());

        log.error("TopWineException: kind={}, code={}, message={}",
                exception.getKind(), exception.getCode(), exception.getMessage(), exception);

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                exception.getCode(),
                exception.getMessage(),
                exchange.getRequest().getPath().value(),
                exchange.getRequest().getId()
        );
        return ResponseEntity.status(httpStatus).body(body);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleAny(Throwable throwable, ServerWebExchange exchange) {
        TopWineException wrapped = TopWineException.fromCause(TopWineException.Kind.INTERNAL_ERROR, throwable);
        HttpStatus httpStatus = mapStatus(wrapped.getKind());

        log.error("Unhandled error wrapped: kind={}, code={}, message={}",
                wrapped.getKind(), wrapped.getCode(), wrapped.getMessage(), throwable);

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                wrapped.getCode(),
                wrapped.getMessage(),
                exchange.getRequest().getPath().value(),
                exchange.getRequest().getId()
        );
        return ResponseEntity.status(httpStatus).body(body);
    }

    private HttpStatus mapStatus(TopWineException.Kind kind) {
        return switch (kind) {
            case PROVIDER_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case PROVIDER_ERROR -> HttpStatus.BAD_GATEWAY;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
