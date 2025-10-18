package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.wine.AvailableWine;
import ru.topwine.assistant.model.wine.AvailableWineFilter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.lower;
import static ru.topwine.assistant.jooq.tables.VAvailableWines.V_AVAILABLE_WINES;

@Repository
@RequiredArgsConstructor
public class AvailableWinesRepository {

    private final DSLContext dsl;

    public List<AvailableWine> search(AvailableWineFilter filter, int limit) {
        List<Condition> conditions = buildConditions(filter);
        int safeLimit = Math.max(1, Math.min(50, limit));

        return dsl.select(
                        V_AVAILABLE_WINES.STOCK_ID,
                        V_AVAILABLE_WINES.WINE_ID,
                        V_AVAILABLE_WINES.WINE_NAME,
                        V_AVAILABLE_WINES.PRODUCER_NAME,
                        V_AVAILABLE_WINES.COUNTRY_NAME,
                        V_AVAILABLE_WINES.REGION_NAME,
                        V_AVAILABLE_WINES.GRAPE_VARIETIES,
                        V_AVAILABLE_WINES.WINE_COLOR,
                        V_AVAILABLE_WINES.VINTAGE_YEAR,
                        V_AVAILABLE_WINES.BOTTLE_SIZE_ML,
                        V_AVAILABLE_WINES.ALCOHOL_BY_VOLUME,
                        V_AVAILABLE_WINES.QUANTITY_BOTTLES,
                        V_AVAILABLE_WINES.PRICE_RUB,
                        V_AVAILABLE_WINES.TAGS_JSON,
                        V_AVAILABLE_WINES.DESCRIPTION_NOTES,
                        V_AVAILABLE_WINES.WINE_CREATED_AT,
                        V_AVAILABLE_WINES.WINE_UPDATED_AT,
                        V_AVAILABLE_WINES.STOCK_UPDATED_AT
                )
                .from(V_AVAILABLE_WINES)
                .where(conditions)
                .orderBy(
                        V_AVAILABLE_WINES.PRICE_RUB.asc().nullsLast(),
                        V_AVAILABLE_WINES.WINE_NAME.asc()
                )
                .limit(safeLimit)
                .fetch(this::map);
    }

    private List<Condition> buildConditions(AvailableWineFilter filter) {
        List<Condition> conditions = new ArrayList<>();

        addEqualsLowerIfPresent(conditions, V_AVAILABLE_WINES.WINE_COLOR, normalizeOrNull(filter.color()));

        addLikeIgnoreCaseIfPresent(conditions, V_AVAILABLE_WINES.COUNTRY_NAME, filter.country());
        addLikeIgnoreCaseIfPresent(conditions, V_AVAILABLE_WINES.REGION_NAME, filter.region());
        addLikeIgnoreCaseIfPresent(conditions, V_AVAILABLE_WINES.GRAPE_VARIETIES, filter.grape());

        if (filter.maxPriceRub() != null) {
            conditions.add(V_AVAILABLE_WINES.PRICE_RUB.le(new BigDecimal(filter.maxPriceRub())));
        }
        if (filter.minVintageYear() != null) {
            conditions.add(V_AVAILABLE_WINES.VINTAGE_YEAR.ge(filter.minVintageYear()));
        }
        if (filter.maxVintageYear() != null) {
            conditions.add(V_AVAILABLE_WINES.VINTAGE_YEAR.le(filter.maxVintageYear()));
        }
        if (filter.freeQuery() != null && !filter.freeQuery().isBlank()) {
            String likePattern = "%" + filter.freeQuery().trim() + "%";
            conditions.add(
                    V_AVAILABLE_WINES.WINE_NAME.likeIgnoreCase(likePattern)
                            .or(V_AVAILABLE_WINES.PRODUCER_NAME.likeIgnoreCase(likePattern))
                            .or(V_AVAILABLE_WINES.REGION_NAME.likeIgnoreCase(likePattern))
                            .or(V_AVAILABLE_WINES.GRAPE_VARIETIES.likeIgnoreCase(likePattern))
            );
        }

        return conditions;
    }

    private void addLikeIgnoreCaseIfPresent(List<Condition> conditions,
                                            org.jooq.Field<String> field,
                                            String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(field.likeIgnoreCase("%" + value.trim() + "%"));
        }
    }

    private void addEqualsLowerIfPresent(List<Condition> conditions,
                                         org.jooq.Field<String> field,
                                         String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(lower(field).eq(value.toLowerCase()));
        }
    }

    private String normalizeOrNull(String value) {
        if (value == null) return null;
        String s = value.trim();
        return s.isEmpty() ? null : s.toLowerCase();
    }

    private AvailableWine map(Record record) {
        String tagsJson =
                record.get(V_AVAILABLE_WINES.TAGS_JSON) == null
                        ? null
                        : record.get(V_AVAILABLE_WINES.TAGS_JSON).data();

        return new AvailableWine(
                record.get(V_AVAILABLE_WINES.STOCK_ID),
                record.get(V_AVAILABLE_WINES.WINE_ID),
                record.get(V_AVAILABLE_WINES.WINE_NAME),
                record.get(V_AVAILABLE_WINES.PRODUCER_NAME),
                record.get(V_AVAILABLE_WINES.COUNTRY_NAME),
                record.get(V_AVAILABLE_WINES.REGION_NAME),
                record.get(V_AVAILABLE_WINES.GRAPE_VARIETIES),
                record.get(V_AVAILABLE_WINES.WINE_COLOR),
                record.get(V_AVAILABLE_WINES.VINTAGE_YEAR),
                record.get(V_AVAILABLE_WINES.BOTTLE_SIZE_ML),
                record.get(V_AVAILABLE_WINES.ALCOHOL_BY_VOLUME),
                record.get(V_AVAILABLE_WINES.QUANTITY_BOTTLES),
                record.get(V_AVAILABLE_WINES.PRICE_RUB),
                tagsJson,
                record.get(V_AVAILABLE_WINES.DESCRIPTION_NOTES),
                record.get(V_AVAILABLE_WINES.WINE_CREATED_AT),
                record.get(V_AVAILABLE_WINES.WINE_UPDATED_AT),
                record.get(V_AVAILABLE_WINES.STOCK_UPDATED_AT)
        );
    }
}
