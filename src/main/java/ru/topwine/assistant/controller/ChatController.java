package ru.topwine.assistant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.topwine.assistant.model.ChatDto;
import ru.topwine.assistant.stab.SommelierService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final SommelierService sommelier;

    @PostMapping("/chat")
    public Mono<ChatDto.ChatResponse> chat(@Valid @RequestBody ChatDto.ChatRequest req) {
        return sommelier.advise(req.message()).map(ChatDto.ChatResponse::new);
    }
}