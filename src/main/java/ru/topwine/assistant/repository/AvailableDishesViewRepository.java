package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.menu.AvailableDish;

import java.util.List;

import static ru.topwine.assistant.jooq.tables.VAvailableDishes.V_AVAILABLE_DISHES;

@Repository
@RequiredArgsConstructor
public class AvailableDishesViewRepository {
    private final DSLContext dsl;

    public List<AvailableDish> findAll() {
        return dsl.select(
                        V_AVAILABLE_DISHES.DISH_ID,
                        V_AVAILABLE_DISHES.SECTION_ID,
                        V_AVAILABLE_DISHES.SECTION_NAME,
                        V_AVAILABLE_DISHES.DISH_NAME,
                        V_AVAILABLE_DISHES.DESCRIPTION,
                        V_AVAILABLE_DISHES.PRICE_RUB
                )
                .from(V_AVAILABLE_DISHES)
                .orderBy(V_AVAILABLE_DISHES.SECTION_NAME.asc(), V_AVAILABLE_DISHES.DISH_NAME.asc())
                .fetch(r -> new AvailableDish(
                        r.get(V_AVAILABLE_DISHES.DISH_ID),
                        r.get(V_AVAILABLE_DISHES.SECTION_ID),
                        r.get(V_AVAILABLE_DISHES.SECTION_NAME),
                        r.get(V_AVAILABLE_DISHES.DISH_NAME),
                        r.get(V_AVAILABLE_DISHES.DESCRIPTION),
                        r.get(V_AVAILABLE_DISHES.PRICE_RUB)
                ));
    }

    public List<AvailableDish> findBySectionName(String sectionName) {
        return dsl.select(
                        V_AVAILABLE_DISHES.DISH_ID,
                        V_AVAILABLE_DISHES.SECTION_ID,
                        V_AVAILABLE_DISHES.SECTION_NAME,
                        V_AVAILABLE_DISHES.DISH_NAME,
                        V_AVAILABLE_DISHES.DESCRIPTION,
                        V_AVAILABLE_DISHES.PRICE_RUB
                )
                .from(V_AVAILABLE_DISHES)
                .where(V_AVAILABLE_DISHES.SECTION_NAME.eq(sectionName))
                .orderBy(V_AVAILABLE_DISHES.DISH_NAME.asc())
                .fetch(r -> new AvailableDish(
                        r.get(V_AVAILABLE_DISHES.DISH_ID),
                        r.get(V_AVAILABLE_DISHES.SECTION_ID),
                        r.get(V_AVAILABLE_DISHES.SECTION_NAME),
                        r.get(V_AVAILABLE_DISHES.DISH_NAME),
                        r.get(V_AVAILABLE_DISHES.DESCRIPTION),
                        r.get(V_AVAILABLE_DISHES.PRICE_RUB)
                ));
    }
}