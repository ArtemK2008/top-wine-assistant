package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.menu.MenuDish;

import java.util.List;
import java.util.Optional;

import static ru.topwine.assistant.jooq.tables.MenuDish.MENU_DISH;

@Repository
@RequiredArgsConstructor
public class MenuDishRepository {
    private final DSLContext dsl;

    public Optional<MenuDish> findById(Long id) {
        return dsl.select(
                        MENU_DISH.ID,
                        MENU_DISH.SECTION_ID,
                        MENU_DISH.NAME,
                        MENU_DISH.DESCRIPTION,
                        MENU_DISH.PRICE_RUB,
                        MENU_DISH.IS_ACTIVE
                )
                .from(MENU_DISH)
                .where(MENU_DISH.ID.eq(id))
                .fetchOptional(this::map);
    }

    public Optional<MenuDish> findByExactName(String name) {
        return dsl.select(
                        MENU_DISH.ID,
                        MENU_DISH.SECTION_ID,
                        MENU_DISH.NAME,
                        MENU_DISH.DESCRIPTION,
                        MENU_DISH.PRICE_RUB,
                        MENU_DISH.IS_ACTIVE
                )
                .from(MENU_DISH)
                .where(MENU_DISH.NAME.eq(name).and(MENU_DISH.IS_ACTIVE.isTrue()))
                .fetchOptional(this::map);
    }

    public List<MenuDish> searchByTextContains(String text, int limit) {
        final String like = "%" + (text == null ? "" : text.trim()) + "%";
        final int safeLimit = Math.max(1, limit);

        return dsl.select(
                        MENU_DISH.ID,
                        MENU_DISH.SECTION_ID,
                        MENU_DISH.NAME,
                        MENU_DISH.DESCRIPTION,
                        MENU_DISH.PRICE_RUB,
                        MENU_DISH.IS_ACTIVE
                )
                .from(MENU_DISH)
                .where(MENU_DISH.IS_ACTIVE.isTrue()
                        .and(
                                MENU_DISH.NAME.likeIgnoreCase(like)
                                        .or(DSL.coalesce(MENU_DISH.DESCRIPTION, "").likeIgnoreCase(like))
                        ))
                .orderBy(MENU_DISH.NAME.asc())
                .limit(safeLimit)
                .fetch(this::map);
    }

    public List<MenuDish> findBySectionId(Long sectionId) {
        return dsl.select(
                        MENU_DISH.ID,
                        MENU_DISH.SECTION_ID,
                        MENU_DISH.NAME,
                        MENU_DISH.DESCRIPTION,
                        MENU_DISH.PRICE_RUB,
                        MENU_DISH.IS_ACTIVE
                )
                .from(MENU_DISH)
                .where(MENU_DISH.SECTION_ID.eq(sectionId).and(MENU_DISH.IS_ACTIVE.isTrue()))
                .orderBy(MENU_DISH.NAME.asc())
                .fetch(this::map);
    }

    private MenuDish map(Record r) {
        return new MenuDish(
                r.get(MENU_DISH.ID),
                r.get(MENU_DISH.SECTION_ID),
                r.get(MENU_DISH.NAME),
                r.get(MENU_DISH.DESCRIPTION),
                r.get(MENU_DISH.PRICE_RUB),
                r.get(MENU_DISH.IS_ACTIVE)
        );
    }
}
