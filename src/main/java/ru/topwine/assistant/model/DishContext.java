package ru.topwine.assistant.model;

import ru.topwine.assistant.model.menu.DishProfile;
import ru.topwine.assistant.model.menu.MenuDish;
import ru.topwine.assistant.model.wine.AvailableWineFilter;

import java.util.List;

public record DishContext(
        MenuDish dish,
        DishProfile profile,
        List<String> tagNames,
        AvailableWineFilter dishFilter
) {
}