package ru.topwine.assistant.service;

import ru.topwine.assistant.model.menu.DishWineFilter;

import java.util.Optional;

public interface DishWineFilterService {
    Optional<DishWineFilter> findByDishId(Long dishId);
}