package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.menu.MenuSection;

import java.util.List;
import java.util.Optional;

import static ru.topwine.assistant.jooq.tables.MenuSection.MENU_SECTION;

@Repository
@RequiredArgsConstructor
public class MenuSectionRepository {
    private final DSLContext dsl;

    public List<MenuSection> findAllActiveOrdered() {
        return dsl.select(
                        MENU_SECTION.ID,
                        MENU_SECTION.NAME,
                        MENU_SECTION.SORT_ORDER,
                        MENU_SECTION.IS_ACTIVE
                )
                .from(MENU_SECTION)
                .where(MENU_SECTION.IS_ACTIVE.isTrue())
                .orderBy(MENU_SECTION.SORT_ORDER.asc(), MENU_SECTION.NAME.asc())
                .fetch(r -> new MenuSection(
                        r.get(MENU_SECTION.ID),
                        r.get(MENU_SECTION.NAME),
                        r.get(MENU_SECTION.SORT_ORDER),
                        r.get(MENU_SECTION.IS_ACTIVE)
                ));
    }

    public Optional<MenuSection> findByName(String name) {
        return dsl.select(
                        MENU_SECTION.ID,
                        MENU_SECTION.NAME,
                        MENU_SECTION.SORT_ORDER,
                        MENU_SECTION.IS_ACTIVE
                )
                .from(MENU_SECTION)
                .where(MENU_SECTION.NAME.eq(name))
                .fetchOptional(r -> new MenuSection(
                        r.get(MENU_SECTION.ID),
                        r.get(MENU_SECTION.NAME),
                        r.get(MENU_SECTION.SORT_ORDER),
                        r.get(MENU_SECTION.IS_ACTIVE)
                ));
    }
}