package ru.topwine.assistant.model.menu;

import java.math.BigDecimal;

public record MenuDish(
        Long id,
        Long sectionId,
        String name,
        String description,
        BigDecimal priceRub,
        Boolean isActive
) {
}