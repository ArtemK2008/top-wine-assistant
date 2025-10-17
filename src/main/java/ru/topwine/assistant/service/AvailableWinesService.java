package ru.topwine.assistant.service;

import ru.topwine.assistant.model.AvailableWine;
import ru.topwine.assistant.model.AvailableWineFilter;

import java.util.List;

public interface AvailableWinesService {
    List<AvailableWine> search(AvailableWineFilter filter, int limit);

    AvailableWineFilter deriveFilterFromUserText(String userText);
}
