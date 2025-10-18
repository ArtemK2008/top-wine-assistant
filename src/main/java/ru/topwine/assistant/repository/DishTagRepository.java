package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ru.topwine.assistant.jooq.tables.DishTag.DISH_TAG;

@Repository
@RequiredArgsConstructor
public class DishTagRepository {
    private final DSLContext dsl;

    @Transactional
    public void addTags(Long dishId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        dsl.batch(
                tagIds.stream()
                        .map(tid -> dsl.insertInto(DISH_TAG)
                                .columns(DISH_TAG.DISH_ID, DISH_TAG.TAG_ID)
                                .values(dishId, tid)
                                .onConflictDoNothing())
                        .toList()
        ).execute();
    }

    public void removeTags(Long dishId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        dsl.deleteFrom(DISH_TAG)
                .where(DISH_TAG.DISH_ID.eq(dishId)
                        .and(DISH_TAG.TAG_ID.in(tagIds)))
                .execute();
    }
}