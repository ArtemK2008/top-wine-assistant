package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.model.menu.DishWineFilter;
import ru.topwine.assistant.repository.DishWineFilterRepository;
import ru.topwine.assistant.service.DishWineFilterService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DishWineFilterServiceImpl implements DishWineFilterService {
    private final DishWineFilterRepository repo;

    @Override
    public Optional<DishWineFilter> findByDishId(Long dishId) {
        return repo.findByDishId(dishId);
    }
}