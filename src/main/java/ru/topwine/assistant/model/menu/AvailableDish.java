package ru.topwine.assistant.model.menu;

import java.math.BigDecimal;

public record AvailableDish(
        Long dishId,
        Long sectionId,
        String sectionName,
        String dishName,
        String description,
        BigDecimal priceRub
) {
}