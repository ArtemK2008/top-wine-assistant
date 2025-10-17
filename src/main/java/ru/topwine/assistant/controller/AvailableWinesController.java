package ru.topwine.assistant.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.topwine.assistant.model.AvailableWine;
import ru.topwine.assistant.model.AvailableWineFilter;
import ru.topwine.assistant.service.AvailableWinesService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AvailableWinesController {

    private final AvailableWinesService availableWinesService;

    @GetMapping(value = "/available-wines", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<AvailableWine>> list(
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String grape,
            @RequestParam(required = false) Integer maxPriceRub,
            @RequestParam(required = false, defaultValue = "200") Integer limit
    ) {
        AvailableWineFilter filter = new AvailableWineFilter(
                null,
                color,
                country,
                region,
                grape,
                maxPriceRub,
                null,
                null
        );
        int safeLimit = Math.max(1, Math.min(500, limit));
        List<AvailableWine> items = availableWinesService.search(filter, safeLimit);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(items);
    }
}
