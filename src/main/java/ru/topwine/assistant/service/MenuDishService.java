package ru.topwine.assistant.service;

import ru.topwine.assistant.model.menu.MenuDish;

import java.util.List;
import java.util.Optional;

public interface MenuDishService {
    Optional<MenuDish> findByExactName(String name);

    List<MenuDish> searchByTextContains(String query, int limit);

    List<MenuDish> findBySectionId(Long sectionId);
}