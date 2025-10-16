package ru.topwine.assistant.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.topwine.assistant.exception.GlobalExceptionHandler;
import ru.topwine.assistant.exception.TopWineException;
import ru.topwine.assistant.model.ChatRequest;
import ru.topwine.assistant.service.SommelierService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

@WebFluxTest(controllers = ChatController.class)
@Import(GlobalExceptionHandler.class)
class ChatControllerExceptionTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SommelierService sommelierService;

    @Test
    @DisplayName("given непредвиденная ошибка when POST /api/chat then 500 и JSON с code=1999")
    void given_unexpected_error_when_chat_then_500_and_json_body() {
        Mockito.when(sommelierService.advise(anyString()))
                .thenReturn(Mono.error(new TopWineException(
                        TopWineException.Kind.PROVIDER_TIMEOUT, 30)));

        ChatRequest request = new ChatRequest("steak");

        webTestClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(1001)
                .jsonPath("$.message").value(msg -> assertThat(msg.toString())
                        .contains("Таймаут. Провайдер не ответил за 30 секунд"))
                .jsonPath("$.path").isEqualTo("/api/chat")
                .jsonPath("$.requestId").isNotEmpty();
    }
}