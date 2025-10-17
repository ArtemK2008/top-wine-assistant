package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.model.AvailableWine;
import ru.topwine.assistant.model.AvailableWineFilter;
import ru.topwine.assistant.repository.AvailableWinesRepository;
import ru.topwine.assistant.service.AvailableWinesService;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AvailableWinesServiceImpl implements AvailableWinesService {
    private final AvailableWinesRepository availableWinesRepository;

    @Override
    public List<AvailableWine> search(AvailableWineFilter filter, int limit) {
        return availableWinesRepository.search(filter, limit);
    }

    /**
     * Очень лёгкий парсер намерений пользователя из свободного текста.
     * Извлекает цвет и примерный бюджет (в рублях), если встречаются.
     * Это только «помощь» фильтрам, а не строгая логика.
     */
    @Override
    public AvailableWineFilter deriveFilterFromUserText(String userText) {
        if (userText == null) {
            return AvailableWineFilter.empty();
        }

        String normalized = userText.toLowerCase(Locale.ROOT);

        String detectedColor = null;
        if (normalized.contains("красн")) detectedColor = "red";
        else if (normalized.contains("бел")) detectedColor = "white";
        else if (normalized.contains("розе") || normalized.contains("розов")) detectedColor = "rose";
        else if (normalized.contains("оранж")) detectedColor = "orange";

        Integer detectedBudgetRub = null;
        // Ищем числа «до 1500», «1500 руб», «1500р», «<= 1500» и т.п.
        Pattern pricePattern = Pattern.compile("(до|<=)?\\s*(\\d{3,6})\\s*(руб|р|₽)?");
        Matcher priceMatcher = pricePattern.matcher(normalized);
        if (priceMatcher.find()) {
            try {
                detectedBudgetRub = Integer.parseInt(priceMatcher.group(2));
            } catch (NumberFormatException ignored) {
                // Игнорируем ошибки парсинга — фильтр просто не получит бюджет
            }
        }

        return new AvailableWineFilter(
                null,
                detectedColor,
                null,
                null,
                null,
                detectedBudgetRub,
                null,
                null
        );
    }
}