package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.configuration.AiProps;
import ru.topwine.assistant.guard.AdviceContext;
import ru.topwine.assistant.guard.AdviceFilterChain;
import ru.topwine.assistant.http.client.ChatClient;
import ru.topwine.assistant.http.request.OpenAiChatCompletionsRequest;
import ru.topwine.assistant.http.request.OpenAiRequestFactory;
import ru.topwine.assistant.model.ChatMessage;
import ru.topwine.assistant.model.wine.AvailableWine;
import ru.topwine.assistant.model.wine.AvailableWineFilter;
import ru.topwine.assistant.service.AvailableWinesService;
import ru.topwine.assistant.service.SommelierService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Сомелье, который рекомендует ТОЛЬКО позиции из БД (v_available_wines).
 * Все комментарии и сообщения — на русском. Без var и однобуквенных имён.
 */
@Service
@RequiredArgsConstructor
public class SommelierServiceImpl implements SommelierService {

    private static final String SYSTEM_PROMPT = """
            Ты — дружелюбный сомелье в винном ресторане. Отвечай ТОЛЬКО НА РУССКОМ.
            Правила:
            - Используй ТОЛЬКО позиции из списка доступных вин, который я передам ниже (ничего не выдумывай).
            - Допустимо называть производителя/бренд ТОЛЬКО так, как указано в позиции.
            - Формат ответа:
              1) 1–3 строки «Название — кратко почему подходит (одно предложение)».
              2) «Пара: …» — 1–2 блюда.
              3) «Уточнение: …» — один короткий вопрос, только при недостатке данных.
            - Терминология: «умеренные танины», «среднее тело», «кислотность», «дубовые ноты» и т.п.
            - Температура подачи всегда: красные 16–18°C (лёгкие — 14–16°C), белые 8–12°C, игристые 6–8°C.
            - Если запрос образовательный (например, «расскажи про рислинг»), сначала короткая справка (2–3 строки) без брендов,
              затем 1–2 примера из списка доступных позиций, который я передам.
            - Не предлагай небезопасные блюда (никаких блюд из сырой свинины/птицы).
            Выводи только финальный ответ БЕЗ рассуждений.
            """;

    private static final int MAIN_GROUNDING_LIMIT = 12;
    private static final int RETRY_GROUNDING_LIMIT = 6;
    private static final int FALLBACK_SUGGESTIONS = 3;
    private static final int FALLBACK_CANDIDATES_LIMIT = 50;
    private static final int NEAR_BUDGET_THRESHOLD_RUB = 400;
    private static final Locale RU_LOCALE = Locale.forLanguageTag("ru-RU");

    private static final double RETRY_TEMPERATURE = 0.2;

    private final ChatClient chatClient;
    private final AiProps aiProps;
    private final AdviceFilterChain filterChain;
    private final AvailableWinesService availableWinesService;

    @Override
    public String advise(String userMessage) {
        AdviceContext adviceContext = new AdviceContext(userMessage);

        Optional<String> earlyReply = filterChain.run(adviceContext);
        if (earlyReply.isPresent()) {
            return earlyReply.get();
        }

        AvailableWineFilter derivedFilter = availableWinesService.deriveFilterFromUserText(userMessage);
        List<AvailableWine> availableWines = availableWinesService.search(derivedFilter, MAIN_GROUNDING_LIMIT);

        if (availableWines.isEmpty()) {
            return handleNoMatches(derivedFilter);
        }

        String reply = askModelOnce(userMessage, availableWines, isEducationalIntent(userMessage));
        reply = sanitizeReply(reply);

        if (isBlankOrPlaceholder(reply)) {
            return retryOrFallback(userMessage, availableWines, derivedFilter);
        }

        return reply;
    }

    private String handleNoMatches(AvailableWineFilter derivedFilter) {
        List<AvailableWine> closest = pickClosestByColorAndBudget(derivedFilter);
        if (closest.isEmpty()) {
            closest = availableWinesService.search(AvailableWineFilter.empty(), FALLBACK_SUGGESTIONS);
        }
        return buildPoliteFallbackMessage(closest, derivedFilter);
    }

    private String askModelOnce(String userMessage, List<AvailableWine> groundingList, boolean educational) {
        String groundingMessage = buildGroundingMessage(groundingList);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(
                "Доступные позиции (основанные на нашей библиотеке — пожалуйста, подбери лучшие из них для гостя):\n"
                + groundingMessage));
        if (educational) {
            messages.add(ChatMessage.system(
                    "Запрос образовательный: сначала краткая справка без брендов, затем 1–2 примера именно из списка выше."));
        }
        messages.add(ChatMessage.user(userMessage));

        OpenAiChatCompletionsRequest request = OpenAiRequestFactory.build(
                aiProps.model(),
                SYSTEM_PROMPT,
                messages,
                aiProps.temperature(),
                aiProps.maxTokens()
        );

        return chatClient.chat(request);
    }

    private String retryOrFallback(String userMessage, List<AvailableWine> fullList, AvailableWineFilter derivedFilter) {
        List<AvailableWine> shortened = fullList.subList(0, Math.min(RETRY_GROUNDING_LIMIT, fullList.size()));
        String shortGrounding = buildGroundingMessage(shortened);

        List<ChatMessage> retryMessages = new ArrayList<>();
        retryMessages.add(ChatMessage.system("Доступные позиции (короткий список):\n" + shortGrounding));
        if (isEducationalIntent(userMessage)) {
            retryMessages.add(ChatMessage.system("Запрос образовательный: краткая справка → 1–2 примера из списка."));
        }
        retryMessages.add(ChatMessage.user(userMessage));

        OpenAiChatCompletionsRequest retryRequest = OpenAiRequestFactory.build(
                aiProps.model(),
                SYSTEM_PROMPT,
                retryMessages,
                RETRY_TEMPERATURE,
                aiProps.maxTokens()
        );

        String retryReply = chatClient.chat(retryRequest);
        retryReply = sanitizeReply(retryReply);

        if (!isBlankOrPlaceholder(retryReply)) {
            return retryReply;
        }

        List<AvailableWine> closest = pickClosestByColorAndBudget(derivedFilter);
        if (closest.isEmpty()) {
            closest = availableWinesService.search(AvailableWineFilter.empty(), FALLBACK_SUGGESTIONS);
        }
        return buildPoliteFallbackMessage(closest, derivedFilter);
    }

    private String buildGroundingMessage(List<AvailableWine> availableWines) {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 1;
        for (AvailableWine availableWine : availableWines) {
            stringBuilder
                    .append(index).append(") ")
                    .append(buildWineLabel(availableWine))
                    .append(" — ")
                    .append(buildWineMeta(availableWine))
                    .append("; цена: ")
                    .append(formatPriceRub(availableWine.priceRub()))
                    .append(", остаток: ")
                    .append(availableWine.quantityBottles())
                    .append(" шт.")
                    .append('\n');
            index++;
        }
        stringBuilder.append("Рекомендации должны опираться только на перечисленные позиции из нашей библиотеки.\n");
        return stringBuilder.toString();
    }

    private String buildWineLabel(AvailableWine availableWine) {
        StringBuilder labelBuilder = new StringBuilder();
        if (availableWine.producerName() != null && !availableWine.producerName().isBlank()) {
            labelBuilder.append(availableWine.producerName()).append(' ');
        }
        labelBuilder.append(availableWine.wineName());
        if (availableWine.vintageYear() != null) {
            labelBuilder.append(' ').append(availableWine.vintageYear());
        }
        return labelBuilder.toString();
    }

    private String buildWineMeta(AvailableWine availableWine) {
        return String.join(", ",
                valueOrDash(availableWine.countryName()),
                valueOrDash(availableWine.regionName()),
                valueOrDash(availableWine.wineColor()),
                valueOrDash(availableWine.grapeVarieties()),
                availableWine.bottleSizeMl() + " мл"
        );
    }

    /**
     * Выбирает ближайшие позиции по указанному цвету и бюджету (если заданы).
     * Если цвет не указан — сортируем только по близости к бюджету; если бюджета нет — по самой цене.
     */
    private List<AvailableWine> pickClosestByColorAndBudget(AvailableWineFilter derivedFilter) {
        AvailableWineFilter colorOnlyFilter = new AvailableWineFilter(
                null,
                derivedFilter.color(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<AvailableWine> candidates = availableWinesService.search(colorOnlyFilter, FALLBACK_CANDIDATES_LIMIT);
        Integer budgetRub = derivedFilter.maxPriceRub();

        candidates.sort((left, right) -> {
            int leftPrice = priceOrMax(left.priceRub());
            int rightPrice = priceOrMax(right.priceRub());

            int leftDiff = (budgetRub == null) ? Integer.MAX_VALUE : Math.abs(leftPrice - budgetRub);
            int rightDiff = (budgetRub == null) ? Integer.MAX_VALUE : Math.abs(rightPrice - budgetRub);

            if (leftDiff != rightDiff) {
                return Integer.compare(leftDiff, rightDiff);
            }
            return Integer.compare(leftPrice, rightPrice);
        });

        List<AvailableWine> result = new ArrayList<>();
        int safeLimit = Math.min(5, SommelierServiceImpl.FALLBACK_SUGGESTIONS);
        for (AvailableWine availableWine : candidates) {
            if (result.size() >= safeLimit) {
                break;
            }
            result.add(availableWine);
        }
        return result;
    }

    private int priceOrMax(BigDecimal priceRub) {
        return priceRub == null ? Integer.MAX_VALUE : priceRub.intValue();
    }

    private String buildPoliteFallbackMessage(List<AvailableWine> fallback, AvailableWineFilter derivedFilter) {
        StringBuilder message = new StringBuilder();
        message.append("Пока нет точных совпадений, но есть несколько близких по стилю и наличию вариантов:\n\n");

        for (AvailableWine availableWine : fallback) {
            String budgetNote = buildBudgetNote(derivedFilter.maxPriceRub(), availableWine.priceRub());
            message.append("• ")
                    .append(buildWineLabel(availableWine))
                    .append(" — ")
                    .append(buildWineMeta(availableWine))
                    .append("; цена: ")
                    .append(formatPriceRub(availableWine.priceRub()))
                    .append(budgetNote)
                    .append('\n');
        }

        message.append("\n").append(buildAdaptiveQuestion(derivedFilter));
        return message.toString();
    }

    private String buildBudgetNote(Integer budgetRub, BigDecimal priceRub) {
        if (budgetRub == null || priceRub == null) {
            return "";
        }
        int diff = priceRub.intValue() - budgetRub;
        if (diff > 0 && diff <= NEAR_BUDGET_THRESHOLD_RUB) {
            return " (чуть выше бюджета)";
        }
        if (diff > NEAR_BUDGET_THRESHOLD_RUB) {
            return " (выше указанного бюджета)";
        }
        return "";
    }

    private String buildAdaptiveQuestion(AvailableWineFilter filter) {
        boolean hasColor = filter.color() != null && !filter.color().isBlank();
        boolean hasBudget = filter.maxPriceRub() != null;

        if (hasColor && hasBudget) {
            return "Уточнение: предпочитаете более минеральный профиль или яркие тропические/фруктовые ноты?";
        }
        if (hasColor) {
            return "Уточнение: подскажите желаемый бюджет, чтобы сузить выбор?";
        }
        if (hasBudget) {
            return "Уточнение: подскажите желаемый цвет (красное/белое/розе), чтобы подобрать точнее?";
        }
        return "Уточнение: подскажете желаемый цвет или бюджет, чтобы подобрать точнее?";
    }

    private boolean isEducationalIntent(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String normalized = userMessage.toLowerCase(Locale.ROOT);
        return normalized.contains("расскажи")
               || normalized.contains("что можешь рассказать")
               || normalized.contains("что это за")
               || normalized.contains("про сорт")
               || normalized.contains("про рислинг")
               || normalized.startsWith("что такое ");
    }

    private String sanitizeReply(String reply) {
        if (reply == null) {
            return "";
        }
        String fixed = reply;
        fixed = fixed.replace("жаренное", "жареное");
        fixed = fixed.replace("тартар из свинины", "тартар из говядины");
        return fixed;
    }

    private boolean isBlankOrPlaceholder(String text) {
        if (text == null) {
            return true;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("пустой ответ от модели");
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }

    private String formatPriceRub(BigDecimal priceRub) {
        if (priceRub == null) {
            return "—";
        }
        return String.format(RU_LOCALE, "%,.2f ₽", priceRub);
    }
}
