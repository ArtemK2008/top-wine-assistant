package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.menu.DishProfile;

import java.util.Optional;

import static ru.topwine.assistant.jooq.tables.DishProfile.DISH_PROFILE;

@Repository
@RequiredArgsConstructor
public class DishProfileRepository {
    private final DSLContext dsl;

    public Optional<DishProfile> findByDishId(Long dishId) {
        return dsl.select(
                        DISH_PROFILE.DISH_ID,
                        DISH_PROFILE.HEAVY_LEVEL,
                        DISH_PROFILE.FAT_LEVEL,
                        DISH_PROFILE.SPICE_LEVEL,
                        DISH_PROFILE.SWEET_LEVEL,
                        DISH_PROFILE.ACID_LEVEL,
                        DISH_PROFILE.PROTEIN_TYPE,
                        DISH_PROFILE.COOK_WAY,
                        DISH_PROFILE.SAUCE_NOTE
                )
                .from(DISH_PROFILE)
                .where(DISH_PROFILE.DISH_ID.eq(dishId))
                .fetchOptional(r -> new DishProfile(
                        r.get(DISH_PROFILE.DISH_ID),
                        r.get(DISH_PROFILE.HEAVY_LEVEL, Integer.class),
                        r.get(DISH_PROFILE.FAT_LEVEL, Integer.class),
                        r.get(DISH_PROFILE.SPICE_LEVEL, Integer.class),
                        r.get(DISH_PROFILE.SWEET_LEVEL, Integer.class),
                        r.get(DISH_PROFILE.ACID_LEVEL, Integer.class),
                        r.get(DISH_PROFILE.PROTEIN_TYPE),
                        r.get(DISH_PROFILE.COOK_WAY),
                        r.get(DISH_PROFILE.SAUCE_NOTE)
                ));
    }
}