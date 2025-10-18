package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.model.menu.MenuDish;
import ru.topwine.assistant.repository.MenuDishRepository;
import ru.topwine.assistant.service.MenuDishService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MenuDishServiceImpl implements MenuDishService {
    private final MenuDishRepository repo;

    @Override
    public Optional<MenuDish> findByExactName(String name) {
        return repo.findByExactName(name);
    }

    @Override
    public List<MenuDish> searchByTextContains(String q, int limit) {
        return repo.searchByTextContains(q, limit);
    }

    @Override
    public List<MenuDish> findBySectionId(Long sectionId) {
        return repo.findBySectionId(sectionId);
    }
}