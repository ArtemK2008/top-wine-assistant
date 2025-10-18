package ru.topwine.assistant.model.menu;

import java.util.Map;

public record DishWineFilter(
        Long dishId,
        Map<String, Object> filter
) {
}