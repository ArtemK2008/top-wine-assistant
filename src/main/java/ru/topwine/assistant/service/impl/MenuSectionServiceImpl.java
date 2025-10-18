package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.model.menu.MenuSection;
import ru.topwine.assistant.repository.MenuSectionRepository;
import ru.topwine.assistant.service.MenuSectionService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MenuSectionServiceImpl implements MenuSectionService {
    private final MenuSectionRepository repo;

    @Override
    public List<MenuSection> activeSections() {
        return repo.findAllActiveOrdered();
    }

    @Override
    public Optional<MenuSection> findByName(String name) {
        return repo.findByName(name);
    }
}