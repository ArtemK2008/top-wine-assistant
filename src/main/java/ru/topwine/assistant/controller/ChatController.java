package ru.topwine.assistant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.topwine.assistant.model.ChatRequest;
import ru.topwine.assistant.model.ChatResponse;
import ru.topwine.assistant.service.SommelierService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final SommelierService sommelier;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        String advice = sommelier.advise(req.message());
        return ResponseEntity.ok(new ChatResponse(advice));
    }
}