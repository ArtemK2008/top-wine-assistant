package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.model.menu.DishProfile;
import ru.topwine.assistant.repository.DishProfileRepository;
import ru.topwine.assistant.service.DishProfileService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DishProfileServiceImpl implements DishProfileService {
    private final DishProfileRepository repo;

    @Override
    public Optional<DishProfile> findByDishId(Long dishId) {
        return repo.findByDishId(dishId);
    }
}