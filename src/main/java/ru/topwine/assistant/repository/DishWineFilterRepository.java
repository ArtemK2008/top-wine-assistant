package ru.topwine.assistant.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.menu.DishWineFilter;

import java.util.Map;
import java.util.Optional;

import static ru.topwine.assistant.jooq.tables.DishWineFilter.DISH_WINE_FILTER;

@Repository
@RequiredArgsConstructor
public class DishWineFilterRepository {
    private final DSLContext dsl;
    private final ObjectMapper mapper;

    public Optional<DishWineFilter> findByDishId(Long dishId) {
        return dsl.select(
                        DISH_WINE_FILTER.DISH_ID,
                        DISH_WINE_FILTER.FILTER_JSON
                )
                .from(DISH_WINE_FILTER)
                .where(DISH_WINE_FILTER.DISH_ID.eq(dishId))
                .fetchOptional(r -> new DishWineFilter(
                        r.get(DISH_WINE_FILTER.DISH_ID),
                        jsonToMap(r.get(DISH_WINE_FILTER.FILTER_JSON))
                ));
    }

    private Map<String, Object> jsonToMap(JSONB jsonb) {
        if (jsonb == null || jsonb.data() == null) return Map.of();
        try {
            return mapper.readValue(jsonb.data(), new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }
}