package ru.topwine.assistant.service;

import ru.topwine.assistant.model.menu.MenuSection;

import java.util.List;
import java.util.Optional;

public interface MenuSectionService {
    List<MenuSection> activeSections();

    Optional<MenuSection> findByName(String name);
}