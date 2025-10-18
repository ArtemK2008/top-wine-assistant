package ru.topwine.assistant.service;

import ru.topwine.assistant.model.menu.AvailableDish;

import java.util.List;

public interface AvailableDishesService {
    List<AvailableDish> all();

    List<AvailableDish> bySection(String sectionName);
}