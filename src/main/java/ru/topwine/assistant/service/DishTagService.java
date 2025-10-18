package ru.topwine.assistant.service;

import java.util.List;

public interface DishTagService {
    void addTags(Long dishId, List<Long> tagIds);

    void removeTags(Long dishId, List<Long> tagIds);
}