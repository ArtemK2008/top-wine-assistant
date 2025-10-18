package ru.topwine.assistant.model.menu;

public record MenuSection(
        Long id,
        String name,
        Integer sortOrder,
        Boolean isActive
) {
}