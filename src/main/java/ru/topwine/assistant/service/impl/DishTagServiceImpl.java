package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.repository.DishTagRepository;
import ru.topwine.assistant.service.DishTagService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DishTagServiceImpl implements DishTagService {
    private final DishTagRepository repo;

    @Override
    public void addTags(Long dishId, List<Long> tagIds) {
        repo.addTags(dishId, tagIds);
    }

    @Override
    public void removeTags(Long dishId, List<Long> tagIds) {
        repo.removeTags(dishId, tagIds);
    }
}