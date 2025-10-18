package ru.topwine.assistant.service;

import ru.topwine.assistant.model.menu.DishProfile;

import java.util.Optional;

public interface DishProfileService {
    Optional<DishProfile> findByDishId(Long dishId);
}