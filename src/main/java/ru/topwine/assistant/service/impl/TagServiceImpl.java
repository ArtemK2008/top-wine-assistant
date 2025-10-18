package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.model.menu.Tag;
import ru.topwine.assistant.repository.TagRepository;
import ru.topwine.assistant.service.TagService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository repo;

    @Override
    public Optional<Tag> findByName(String name) {
        return repo.findByName(name);
    }

    @Override
    public List<Tag> findAllByNames(List<String> names) {
        return repo.findAllByNames(names);
    }

    @Override
    public List<String> findNamesByDishId(Long dishId) {
        return repo.findNamesByDishId(dishId);
    }
}