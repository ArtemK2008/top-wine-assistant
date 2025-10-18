package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.model.menu.AvailableDish;
import ru.topwine.assistant.repository.AvailableDishesViewRepository;
import ru.topwine.assistant.service.AvailableDishesService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailableDishesServiceImpl implements AvailableDishesService {
    private final AvailableDishesViewRepository repo;

    @Override
    public List<AvailableDish> all() {
        return repo.findAll();
    }

    @Override
    public List<AvailableDish> bySection(String sectionName) {
        return repo.findBySectionName(sectionName);
    }
}