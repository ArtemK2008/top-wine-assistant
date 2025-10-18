package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.menu.Tag;

import java.util.List;
import java.util.Optional;

import static ru.topwine.assistant.jooq.tables.DishTag.DISH_TAG;
import static ru.topwine.assistant.jooq.tables.Tag.TAG;


@Repository
@RequiredArgsConstructor
public class TagRepository {
    private final DSLContext dsl;

    public Optional<Tag> findByName(String name) {
        return dsl.select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.eq(name))
                .fetchOptional(r -> new Tag(r.get(TAG.ID), r.get(TAG.NAME)));
    }

    public List<Tag> findAllByNames(List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        return dsl.select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.in(names))
                .fetch(r -> new Tag(r.get(TAG.ID), r.get(TAG.NAME)));
    }

    public List<String> findNamesByDishId(Long dishId) {
        return dsl.select(TAG.NAME)
                .from(DISH_TAG)
                .join(TAG).on(TAG.ID.eq(DISH_TAG.TAG_ID))
                .where(DISH_TAG.DISH_ID.eq(dishId))
                .orderBy(TAG.NAME.asc())
                .fetch(TAG.NAME);
    }
}