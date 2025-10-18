package ru.topwine.assistant.model.menu;

public record DishProfile(
        Long dishId,
        Integer heavyLevel,
        Integer fatLevel,
        Integer spiceLevel,
        Integer sweetLevel,
        Integer acidLevel,
        String proteinType,
        String cookWay,
        String sauceNote
) {
}