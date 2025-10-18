package ru.topwine.assistant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.topwine.assistant.model.ChatRequest;
import ru.topwine.assistant.model.ChatResponse;
import ru.topwine.assistant.service.SommelierService;
import ru.topwine.assistant.service.session.ConversationStore;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final SommelierService sommelier;
    private final ConversationStore conversationStore;

    @PostMapping(value = "/chat", produces = "application/json; charset=UTF-8")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader(value = "X-Client-Id", required = false) String clientId,
            @Valid @RequestBody ChatRequest req
    ) {
        String effectiveClientId = (clientId == null || clientId.isBlank())
                ? "anon-" + UUID.randomUUID()
                : clientId;

        String reply = sommelier.advise(effectiveClientId, req.message());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    @PostMapping("/conversation/reset")
    public ResponseEntity<Void> reset(@RequestHeader("X-Client-Id") String clientId) {
        conversationStore.clear(clientId);
        return ResponseEntity.noContent().build();
    }
}
