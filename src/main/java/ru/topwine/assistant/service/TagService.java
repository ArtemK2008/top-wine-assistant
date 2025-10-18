package ru.topwine.assistant.service;

import ru.topwine.assistant.model.menu.Tag;

import java.util.List;
import java.util.Optional;

public interface TagService {
    Optional<Tag> findByName(String name);

    List<Tag> findAllByNames(List<String> names);

    List<String> findNamesByDishId(Long dishId);
}