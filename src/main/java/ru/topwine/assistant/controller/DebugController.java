package ru.topwine.assistant.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.topwine.assistant.service.DebugService;

@RestController
@RequestMapping("/db")
@RequiredArgsConstructor
public class DebugController {

    private final DebugService service;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok(service.ping());
    }

    @GetMapping("/tables")
    public ResponseEntity<String> listTables() {
        return ResponseEntity.ok(service.listTablesPretty());
    }

    @GetMapping("/describe/{schema}/{table}")
    public ResponseEntity<String> describe(@PathVariable String schema,
                                           @PathVariable String table) {
        return ResponseEntity.ok(service.describeTablePretty(schema, table));
    }
}