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
import ru.topwine.assistant.model.PriceBucket;
import ru.topwine.assistant.model.enums.UserRequestType;
import ru.topwine.assistant.model.menu.AvailableDish;
import ru.topwine.assistant.model.menu.DishProfile;
import ru.topwine.assistant.model.menu.DishWineFilter;
import ru.topwine.assistant.model.menu.MenuDish;
import ru.topwine.assistant.model.menu.MenuSection;
import ru.topwine.assistant.model.util.UserRequest;
import ru.topwine.assistant.model.wine.AvailableWine;
import ru.topwine.assistant.model.wine.AvailableWineFilter;
import ru.topwine.assistant.service.AvailableDishesService;
import ru.topwine.assistant.service.AvailableWinesService;
import ru.topwine.assistant.service.DishProfileService;
import ru.topwine.assistant.service.DishWineFilterService;
import ru.topwine.assistant.service.MenuDishService;
import ru.topwine.assistant.service.MenuSectionService;
import ru.topwine.assistant.service.SommelierService;
import ru.topwine.assistant.service.TagService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SommelierServiceImpl implements SommelierService {

    private static final String SYSTEM_PROMPT = """
            Ты — дружелюбный сомелье в винном ресторане. Отвечай ТОЛЬКО НА РУССКОМ.
            Правила:
            - Рекомендуй ТОЛЬКО позиции из списка доступных вин и (если дано) из списка блюд меню ниже.
            - Формат:
              1) 1–3 строки «Название вина — коротко почему подходит».
              2) «Пара: …» — 1–2 блюда из меню (если уместно).
              3) «Уточнение: …» — один короткий вопрос, если данных мало.
            - Термины: «умеренные танины», «среднее тело», «кислотность», «дубовые ноты» и т.п.
            - Подача: красные 16–18°C (лёгкие 14–16°C), белые 8–12°C, игристые 6–8°C.
            - Если запрос образовательный — краткая справка (2–3 строки) без брендов, затем 1–2 примера из списка.
            Выводи только финальный ответ БЕЗ рассуждений.
            """;

    private static final int GROUNDING_LIMIT = 12;
    private static final int OVERFETCH_PER_BUCKET = 50;
    private static final int MENU_DISHES_CONTEXT_LIMIT = 12;
    private static final Locale RU = Locale.forLanguageTag("ru-RU");

    private final ChatClient chatClient;
    private final AiProps aiProps;
    private final AdviceFilterChain adviceFilterChain;

    private final AvailableWinesService availableWinesService;
    private final AvailableDishesService availableDishesService;
    private final MenuDishService menuDishService;
    private final MenuSectionService menuSectionService;
    private final DishWineFilterService dishWineFilterService;
    private final DishProfileService dishProfileService;
    private final TagService tagService;

    @Override
    public String advise(String userMessage) {
        AdviceContext adviceContext = new AdviceContext(userMessage);
        Optional<String> early = adviceFilterChain.run(adviceContext);
        if (early.isPresent()) return early.get();

        UserRequest userRequest = detectUserRequest(userMessage);

        AvailableWineFilter userFilter = availableWinesService.deriveFilterFromUserText(userMessage);
        AvailableWineFilter effectiveFilter = userFilter;

        String sectionContextLine = null;
        String dishProfileBlock = null;
        List<AvailableDish> menuForContext = List.of();

        if (userRequest.type() == UserRequestType.DISH) {
            MenuDish dish = menuDishService.findByExactName(userRequest.text())
                    .orElseGet(() -> menuDishService.searchByTextContains(userRequest.text(), 3).stream().findFirst().orElse(null));
            if (dish != null) {
                AvailableWineFilter fromDishJson = readDishWineFilter(dish.id());
                Optional<DishProfile> profileOpt = dishProfileService.findByDishId(dish.id());
                List<String> tagNames = tagService.findNamesByDishId(dish.id());

                AvailableWineFilter enrichedFromDish = deriveFilterFromProfileAndTags(profileOpt.orElse(null), tagNames, fromDishJson);
                effectiveFilter = mergeFilters(enrichedFromDish, userFilter);

                dishProfileBlock = buildDishContextBlock(dish, profileOpt.orElse(null), tagNames);
                menuForContext = pickMenuForContext();
            }
        } else if (userRequest.type() == UserRequestType.SECTION) {
            String sectionName = resolveSectionName(userRequest.text());
            if (sectionName != null) {
                sectionContextLine = "Раздел меню: " + sectionName;
                List<AvailableDish> inSection = availableDishesService.bySection(sectionName);
                menuForContext = limit(mergeDistinct(inSection, pickMenuForContext()), MENU_DISHES_CONTEXT_LIMIT);
            }
        } else if (userRequest.type() == UserRequestType.WINE_NAME) {
            menuForContext = pickMenuForContext();
        }

        List<AvailableWine> groundingWines = (effectiveFilter.maxPriceRub() != null)
                ? availableWinesService.search(effectiveFilter, GROUNDING_LIMIT)
                : fetchBucketedWithoutBudget(effectiveFilter);

        if (groundingWines.isEmpty()) {
            return buildNoMatchesResponse(effectiveFilter);
        }

        StringBuilder systemBlock = new StringBuilder();
        if (sectionContextLine != null) systemBlock.append(sectionContextLine).append("\n");
        if (dishProfileBlock != null && !dishProfileBlock.isBlank()) systemBlock.append(dishProfileBlock).append("\n");
        if (!menuForContext.isEmpty()) {
            systemBlock.append("Доступные блюда меню (используй только их для пар):\n")
                    .append(renderMenu(menuForContext))
                    .append("\n");
        }
        systemBlock.append("Доступные позиции вина (используй только их):\n")
                .append(renderWines(groundingWines));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemBlock.toString()));
        messages.add(ChatMessage.user(userMessage));

        OpenAiChatCompletionsRequest request = OpenAiRequestFactory.build(
                aiProps.model(), SYSTEM_PROMPT, messages, aiProps.temperature(), aiProps.maxTokens()
        );

        String reply = chatClient.chat(request);
        return reply == null ? "" : reply;
    }

    private UserRequest detectUserRequest(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new UserRequest(UserRequestType.GENERAL, null);
        }
        String text = userMessage.trim();
        String lowered = text.toLowerCase(RU);

        if (lowered.contains("закуск") || lowered.contains("стартер")
            || lowered.contains("суп") || lowered.contains("горяч")
            || lowered.contains("основ") || lowered.contains("десерт")) {
            return new UserRequest(UserRequestType.SECTION, text);
        }

        if (menuDishService.findByExactName(text).isPresent()) {
            return new UserRequest(UserRequestType.DISH, text);
        }

        if (mentionsWineConcept(lowered)) {
            return new UserRequest(UserRequestType.WINE_NAME, text);
        }

        return new UserRequest(UserRequestType.GENERAL, text);
    }

    private boolean mentionsWineConcept(String lowered) {
        return lowered.contains("каберне") || lowered.contains("мерло") || lowered.contains("пино")
               || lowered.contains("рислинг") || lowered.contains("шардоне")
               || lowered.contains("красн") || lowered.contains("бел") || lowered.contains("розе") || lowered.contains("игрист");
    }

    private String resolveSectionName(String userText) {
        String lower = userText.toLowerCase(RU);
        return menuSectionService.activeSections().stream()
                .map(MenuSection::name)
                .filter(Objects::nonNull)
                .filter(name -> lower.contains(name.toLowerCase(RU)) || synonymsMatch(lower, name))
                .findFirst()
                .orElseGet(() -> {
                    if (lower.contains("закуск")) return "Закуски";
                    if (lower.contains("суп")) return "Супы";
                    if (lower.contains("горяч") || lower.contains("основ")) return "Горячее";
                    if (lower.contains("десерт")) return "Десерты";
                    return null;
                });
    }

    private boolean synonymsMatch(String loweredUser, String sectionName) {
        String lowerName = sectionName.toLowerCase(RU);
        if (lowerName.contains("закуск")) return loweredUser.contains("стартер");
        if (lowerName.contains("горяч") || lowerName.contains("основ")) return loweredUser.contains("мэйн");
        return false;
    }

    private AvailableWineFilter readDishWineFilter(Long dishId) {
        Optional<DishWineFilter> raw = dishWineFilterService.findByDishId(dishId);
        Map<String, Object> map = raw.map(DishWineFilter::filter).orElse(Map.of());
        return new AvailableWineFilter(
                readString(map.get("freeQuery")),
                readString(map.get("color")),
                readString(map.get("country")),
                readString(map.get("region")),
                readString(map.get("grape")),
                readInt(map.get("maxPriceRub")),
                readInt(map.get("minVintageYear")),
                readInt(map.get("maxVintageYear"))
        );
    }

    private String buildDishContextBlock(MenuDish dish, DishProfile profile, List<String> tagNames) {
        StringBuilder b = new StringBuilder();
        b.append("Профиль блюда для ориентиров:\n")
                .append("- Блюдо: ").append(dish.name());
        if (dish.description() != null && !dish.description().isBlank()) {
            b.append(" — ").append(dish.description());
        }
        b.append("\n- Цена: ").append(formatPrice(dish.priceRub())).append("\n");

        if (profile != null) {
            b.append("- Параметры: ")
                    .append("белок=").append(nullSafe(profile.proteinType()))
                    .append(", способ=").append(nullSafe(profile.cookWay()))
                    .append(", тяж=").append(nullSafe(profile.heavyLevel()))
                    .append(", жир=").append(nullSafe(profile.fatLevel()))
                    .append(", остр=").append(nullSafe(profile.spiceLevel()))
                    .append(", слад=").append(nullSafe(profile.sweetLevel()))
                    .append(", кис=").append(nullSafe(profile.acidLevel()))
                    .append("\n");
        }
        if (tagNames != null && !tagNames.isEmpty()) {
            b.append("- Теги: ").append(String.join(", ", tagNames)).append("\n");
        }
        return b.toString();
    }

    private AvailableWineFilter deriveFilterFromProfileAndTags(
            DishProfile profile,
            List<String> tagNames,
            AvailableWineFilter base
    ) {
        if (profile == null && (tagNames == null || tagNames.isEmpty())) {
            return base;
        }

        String chosenColor = base.color();
        if (chosenColor == null && profile != null) {
            String protein = safeLower(profile.proteinType());
            if (protein != null) {
                switch (protein) {
                    case "beef", "lamb", "duck" -> chosenColor = "red";
                    case "fish_white", "seafood", "chicken", "veg", "cheese", "dessert" -> chosenColor = "white";
                    default -> {
                    }
                }
            }
        }

        String grapeHint = base.grape();
        if (grapeHint == null) {
            grapeHint = inferGrapeFromProfileAndTags(profile, tagNames, chosenColor);
        }

        return new AvailableWineFilter(
                base.freeQuery(),
                chosenColor,
                base.country(),
                base.region(),
                grapeHint,
                base.maxPriceRub(),
                base.minVintageYear(),
                base.maxVintageYear()
        );
    }

    private String inferGrapeFromProfileAndTags(
            DishProfile profile,
            List<String> tagNames,
            String chosenColor
    ) {
        if (profile == null && (tagNames == null || tagNames.isEmpty())) {
            return null;
        }

        Integer fatLevel = profile == null ? null : profile.fatLevel();
        Integer spiceLevel = profile == null ? null : profile.spiceLevel();
        String cookWay = safeLower(profile == null ? null : profile.cookWay());
        String protein = safeLower(profile == null ? null : profile.proteinType());

        if (tagContains(tagNames, "анчоус")) return "albariño";
        if (tagContains(tagNames, "камамбер")) return "chardonnay";
        if (tagContains(tagNames, "голландский соус")) return "chablis";

        if (high(fatLevel)) {
            return (chosenColor == null || "white".equals(chosenColor))
                    ? "riesling" : "pinot noir";
        }
        if (high(spiceLevel)) return "gewürztraminer";
        if ("grilled".equals(cookWay)) return "syrah";
        if ("raw".equals(cookWay) && "fish_fatty".equals(protein)) return "sauvignon blanc";

        return null;
    }

    private String safeLower(String s) {
        return (s == null) ? null : s.toLowerCase(RU).trim();
    }

    private boolean tagContains(List<String> tags, String needle) {
        if (tags == null || tags.isEmpty()) return false;
        String n = needle.toLowerCase(RU);
        return tags.stream().anyMatch(t -> t != null && t.toLowerCase(RU).contains(n));
    }

    private Boolean high(Integer level) {
        return level != null && level >= 3;
    }

    private List<AvailableWine> fetchBucketedWithoutBudget(AvailableWineFilter filterWithoutPrice) {
        List<PriceBucket> plan = List.of(
                new PriceBucket(1500, 3000, 4),
                new PriceBucket(3000, 5000, 4),
                new PriceBucket(5000, 10000, 2),
                new PriceBucket(null, 1500, 1),
                new PriceBucket(10000, 30000, 1),
                new PriceBucket(30000, null, 1)
        );

        LinkedHashMap<Long, AvailableWine> pickedByStock = new LinkedHashMap<>();

        for (PriceBucket bucket : plan) {
            if (pickedByStock.size() >= GROUNDING_LIMIT) break;

            AvailableWineFilter repoFilter = withMaxOnly(filterWithoutPrice, bucket.maxExclusive());
            List<AvailableWine> prefiltered = availableWinesService.search(repoFilter, OVERFETCH_PER_BUCKET);

            List<AvailableWine> inBucket = prefiltered.stream()
                    .filter(w -> fitsBucket(w.priceRub(), bucket.minInclusive(), bucket.maxExclusive()))
                    .filter(w -> !pickedByStock.containsKey(w.stockId()))
                    .limit(bucket.targetCount())
                    .toList();

            inBucket.forEach(w -> pickedByStock.put(w.stockId(), w));
        }

        if (pickedByStock.size() < GROUNDING_LIMIT) {
            List<AvailableWine> topUp = availableWinesService.search(removePrice(filterWithoutPrice), 100);
            for (AvailableWine w : topUp) {
                if (pickedByStock.size() >= GROUNDING_LIMIT) break;
                pickedByStock.putIfAbsent(w.stockId(), w);
            }
        }

        return new ArrayList<>(pickedByStock.values());
    }

    private String buildNoMatchesResponse(AvailableWineFilter filter) {
        String[] preambles = {
                "Прошу прощения — сейчас не нашёл точных совпадений.",
                "Извините, точных совпадений не нашлось.",
                "Похоже, прямо сейчас нет позиций, которые строго попадают в запрос."
        };
        String[] closings = {
                "Уточните, пожалуйста, стиль (например, «минеральнее» или «фруктовее») — сузим выбор.",
                "Если подскажете желаемый стиль, я подберу точнее.",
                "Готов уточнить по стилю и показать альтернативы ближе к вкусу."
        };

        int preIdx = (int) (System.nanoTime() % preambles.length);
        int closeIdx = (int) ((System.nanoTime() / 7) % closings.length);
        String preamble = preambles[preIdx];
        String closing = closings[closeIdx];

        if (filter != null && filter.maxPriceRub() != null) {
            int budgetRub = filter.maxPriceRub();
            List<AvailableWine> closest = pickClosestByBudget(filter, budgetRub, 16);
            if (!closest.isEmpty()) {
                List<AvailableWine> shortlist = randomTrim(closest, 3, 6);
                return composeNoMatchesWithLLM(
                        preamble,
                        "Если устроит посмотреть варианты рядом с бюджетом ~" + String.format(RU, "%,d ₽", budgetRub) + ", могу предложить:",
                        shortlist,
                        closing
                );
            }
        }

        List<AvailableWine> balanced = fetchBucketedWithoutBudget(
                new AvailableWineFilter(
                        filter == null ? null : filter.freeQuery(),
                        filter == null ? null : filter.color(),
                        filter == null ? null : filter.country(),
                        filter == null ? null : filter.region(),
                        filter == null ? null : filter.grape(),
                        null,
                        filter == null ? null : filter.minVintageYear(),
                        filter == null ? null : filter.maxVintageYear()
                )
        );
        if (!balanced.isEmpty()) {
            List<AvailableWine> shortlist = randomTrim(balanced, 3, 6);
            return composeNoMatchesWithLLM(
                    preamble,
                    "Покажу несколько близких по профилю вариантов в популярных ценовых диапазонах:",
                    shortlist,
                    closing
            );
        }

        return preamble + " Похоже, сейчас почти ничего нет в наличии. Могу записать ваши предпочтения (цвет, стиль, бюджет) и предложить, когда появится обновление.";
    }

    private List<AvailableWine> randomTrim(List<AvailableWine> source, int min, int max) {
        if (source == null || source.isEmpty()) return List.of();
        List<AvailableWine> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        int span = Math.max(1, max - min + 1);
        int take = Math.min(shuffled.size(), min + (int) (Math.abs(System.nanoTime()) % span));
        take = Math.max(min, Math.min(max, take));
        return shuffled.subList(0, take);
    }

    private String composeNoMatchesWithLLM(String preamble,
                                           String middleLine,
                                           List<AvailableWine> shortlist,
                                           String closing) {
        String grounding = renderWines(shortlist);

        String fallbackSystem = """
                Сформулируй вежливый ответ на русском.
                Используй ТОЛЬКО список позиций, который я дам ниже (ничего не придумывай).
                Формат:
                1) Короткое извинение + пояснение (1 строка).
                2) Одна фраза-переход к альтернативам.
                3) Список 3–6 вариантов: «Название — коротко почему подходит; цена: …».
                4) Один уточняющий вопрос (стиль/бюджет).
                Избегай повторов формулировок, вариативность приветствуется.
                """;

        List<ChatMessage> msgs = new ArrayList<>();
        msgs.add(ChatMessage.system(fallbackSystem));
        msgs.add(ChatMessage.system("Доступные позиции (используй только их):\n" + grounding));
        String userFrame = preamble + "\n" + middleLine + "\n\n" + "Заверши вопросом: " + closing;
        msgs.add(ChatMessage.user(userFrame));

        OpenAiChatCompletionsRequest req = OpenAiRequestFactory.build(
                aiProps.model(),
                SYSTEM_PROMPT,
                msgs,
                aiProps.temperature(),
                aiProps.maxTokens()
        );
        String reply = chatClient.chat(req);
        return reply == null ? (preamble + "\n" + middleLine + "\n\n" + renderWines(shortlist) + "\n\n" + closing) : reply;
    }

    private List<AvailableWine> pickClosestByBudget(AvailableWineFilter baseFilter, int budgetRub, int limit) {
        AvailableWineFilter withoutPrice = new AvailableWineFilter(
                baseFilter.freeQuery(),
                baseFilter.color(),
                baseFilter.country(),
                baseFilter.region(),
                baseFilter.grape(),
                null,
                baseFilter.minVintageYear(),
                baseFilter.maxVintageYear()
        );

        List<AvailableWine> candidates = availableWinesService.search(withoutPrice, 200);
        if (candidates.isEmpty()) return List.of();

        candidates.sort((left, right) -> {
            int leftP = priceOrMax(left.priceRub());
            int rightP = priceOrMax(right.priceRub());
            int leftDiff = Math.abs(leftP - budgetRub);
            int rightDiff = Math.abs(rightP - budgetRub);
            if (leftDiff != rightDiff) return Integer.compare(leftDiff, rightDiff);
            return Integer.compare(leftP, rightP);
        });

        List<AvailableWine> result = new ArrayList<>();
        for (AvailableWine wine : candidates) {
            if (result.size() >= Math.max(1, limit)) break;
            result.add(wine);
        }
        return result;
    }

    private int priceOrMax(BigDecimal priceRub) {
        return priceRub == null ? Integer.MAX_VALUE / 4 : priceRub.intValue();
    }

    private boolean fitsBucket(BigDecimal price, Integer min, Integer max) {
        if (price == null) return false;
        int value = price.intValue();
        if (min != null && value < min) return false;
        return max == null || value < max;
    }

    private AvailableWineFilter withMaxOnly(AvailableWineFilter source, Integer newMaxExclusive) {
        return new AvailableWineFilter(
                source.freeQuery(), source.color(), source.country(), source.region(),
                source.grape(), newMaxExclusive, source.minVintageYear(), source.maxVintageYear()
        );
    }

    private AvailableWineFilter removePrice(AvailableWineFilter source) {
        return new AvailableWineFilter(
                source.freeQuery(), source.color(), source.country(), source.region(),
                source.grape(), null, source.minVintageYear(), source.maxVintageYear()
        );
    }

    private AvailableWineFilter mergeFilters(AvailableWineFilter fromDish, AvailableWineFilter fromUser) {
        return new AvailableWineFilter(
                firstNonNull(fromUser.freeQuery(), fromDish.freeQuery()),
                firstNonNull(fromUser.color(), fromDish.color()),
                firstNonNull(fromUser.country(), fromDish.country()),
                firstNonNull(fromUser.region(), fromDish.region()),
                firstNonNull(fromUser.grape(), fromDish.grape()),
                firstNonNull(fromUser.maxPriceRub(), fromDish.maxPriceRub()),
                firstNonNull(fromUser.minVintageYear(), fromDish.minVintageYear()),
                firstNonNull(fromUser.maxVintageYear(), fromDish.maxVintageYear())
        );
    }

    private <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private String renderWines(List<AvailableWine> wines) {
        StringBuilder out = new StringBuilder();
        int idx = 1;
        for (AvailableWine wine : wines) {
            out.append(idx++).append(") ")
                    .append(buildLabel(wine)).append(" — ")
                    .append(buildMeta(wine))
                    .append("; цена: ").append(formatPrice(wine.priceRub()))
                    .append(", остаток: ").append(wine.quantityBottles()).append(" шт.\n");
        }
        return out.toString();
    }

    private String buildLabel(AvailableWine wine) {
        StringBuilder b = new StringBuilder();
        if (wine.producerName() != null && !wine.producerName().isBlank()) b.append(wine.producerName()).append(' ');
        b.append(wine.wineName());
        if (wine.vintageYear() != null) b.append(' ').append(wine.vintageYear());
        return b.toString();
    }

    private String buildMeta(AvailableWine wine) {
        return String.join(", ",
                dash(wine.countryName()),
                dash(wine.regionName()),
                dash(wine.wineColor()),
                dash(wine.grapeVarieties()),
                wine.bottleSizeMl() == null ? "—" : (wine.bottleSizeMl() + " мл")
        );
    }

    private String renderMenu(List<AvailableDish> dishes) {
        return dishes.stream()
                .limit(MENU_DISHES_CONTEXT_LIMIT)
                .map(d -> "- " + d.sectionName() + ": " + d.dishName()
                          + (d.description() == null || d.description().isBlank() ? "" : " — " + d.description())
                          + "; " + formatPrice(d.priceRub()))
                .collect(Collectors.joining("\n"));
    }

    private List<AvailableDish> pickMenuForContext() {
        List<AvailableDish> all = availableDishesService.all();
        return limit(all, MENU_DISHES_CONTEXT_LIMIT);
    }

    private <T> List<T> limit(List<T> source, int cap) {
        if (source == null) return List.of();
        return source.size() <= cap ? source : source.subList(0, cap);
    }

    private <T> List<T> mergeDistinct(List<T> a, List<T> b) {
        LinkedHashSet<T> set = new LinkedHashSet<>();
        if (a != null) set.addAll(a);
        if (b != null) set.addAll(b);
        return new ArrayList<>(set);
    }

    private String dash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }

    private String readString(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }

    private Integer readInt(Object v) {
        switch (v) {
            case null -> {
                return null;
            }
            case Integer i -> {
                return i;
            }
            case Long l -> {
                return Math.toIntExact(l);
            }
            case Double d -> {
                return (int) Math.round(d);
            }
            case BigDecimal bd -> {
                return bd.intValue();
            }
            default -> {
            }
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String nullSafe(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "—" : String.format(RU, "%,.0f ₽", price);
    }
}
