package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.topwine.assistant.model.AvailableWine;
import ru.topwine.assistant.model.AvailableWineFilter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для чтения доступных вин из представления v_available_wines.
 * Без codegen: поля описаны вручную, имена строго соответствуют Liquibase.
 */
@Repository
@RequiredArgsConstructor
public class AvailableWinesRepository {

    private final DSLContext dslContext;

    private static final Table<Record> VIEW_AVAILABLE = DSL.table("v_available_wines");

    private static final Field<Long> FIELD_STOCK_ID = DSL.field("stock_id", Long.class);
    private static final Field<Long> FIELD_WINE_ID = DSL.field("wine_id", Long.class);
    private static final Field<String> FIELD_WINE_NAME = DSL.field("wine_name", String.class);
    private static final Field<String> FIELD_PRODUCER_NAME = DSL.field("producer_name", String.class);
    private static final Field<String> FIELD_COUNTRY_NAME = DSL.field("country_name", String.class);
    private static final Field<String> FIELD_REGION_NAME = DSL.field("region_name", String.class);
    private static final Field<String> FIELD_GRAPE_VARIETIES = DSL.field("grape_varieties", String.class);
    private static final Field<String> FIELD_WINE_COLOR = DSL.field("wine_color", String.class);
    private static final Field<Integer> FIELD_VINTAGE_YEAR = DSL.field("vintage_year", Integer.class);
    private static final Field<Integer> FIELD_BOTTLE_SIZE_ML = DSL.field("bottle_size_ml", Integer.class);
    private static final Field<BigDecimal> FIELD_ABV = DSL.field("alcohol_by_volume", BigDecimal.class);
    private static final Field<Integer> FIELD_QUANTITY = DSL.field("quantity_bottles", Integer.class);
    private static final Field<BigDecimal> FIELD_PRICE_RUB = DSL.field("price_rub", BigDecimal.class);
    private static final Field<String> FIELD_TAGS_JSON = DSL.field("tags_json", String.class);
    private static final Field<String> FIELD_DESCRIPTION = DSL.field("description_notes", String.class);
    private static final Field<OffsetDateTime> FIELD_WINE_CREATED_AT = DSL.field("wine_created_at", OffsetDateTime.class);
    private static final Field<OffsetDateTime> FIELD_WINE_UPDATED_AT = DSL.field("wine_updated_at", OffsetDateTime.class);
    private static final Field<OffsetDateTime> FIELD_STOCK_UPDATED_AT = DSL.field("stock_updated_at", OffsetDateTime.class);

    /**
     * Поиск доступных вин с применением фильтров.
     * Представление уже гарантирует остаток > 0.
     */
    public List<AvailableWine> search(AvailableWineFilter filter, int limit) {
        List<Condition> conditions = buildConditions(filter);
        int safeLimit = Math.max(1, Math.min(50, limit));

        return dslContext
                .select(
                        FIELD_STOCK_ID,
                        FIELD_WINE_ID,
                        FIELD_WINE_NAME,
                        FIELD_PRODUCER_NAME,
                        FIELD_COUNTRY_NAME,
                        FIELD_REGION_NAME,
                        FIELD_GRAPE_VARIETIES,
                        FIELD_WINE_COLOR,
                        FIELD_VINTAGE_YEAR,
                        FIELD_BOTTLE_SIZE_ML,
                        FIELD_ABV,
                        FIELD_QUANTITY,
                        FIELD_PRICE_RUB,
                        FIELD_TAGS_JSON,
                        FIELD_DESCRIPTION,
                        FIELD_WINE_CREATED_AT,
                        FIELD_WINE_UPDATED_AT,
                        FIELD_STOCK_UPDATED_AT
                )
                .from(VIEW_AVAILABLE)
                .where(conditions)
                .orderBy(
                        FIELD_PRICE_RUB.asc().nullsLast(),
                        FIELD_WINE_NAME.asc()
                )
                .limit(safeLimit)
                .fetch(record -> new AvailableWine(
                        record.get(FIELD_STOCK_ID),
                        record.get(FIELD_WINE_ID),
                        record.get(FIELD_WINE_NAME),
                        record.get(FIELD_PRODUCER_NAME),
                        record.get(FIELD_COUNTRY_NAME),
                        record.get(FIELD_REGION_NAME),
                        record.get(FIELD_GRAPE_VARIETIES),
                        record.get(FIELD_WINE_COLOR),
                        record.get(FIELD_VINTAGE_YEAR),
                        record.get(FIELD_BOTTLE_SIZE_ML),
                        record.get(FIELD_ABV),
                        record.get(FIELD_QUANTITY),
                        record.get(FIELD_PRICE_RUB),
                        record.get(FIELD_TAGS_JSON),
                        record.get(FIELD_DESCRIPTION),
                        record.get(FIELD_WINE_CREATED_AT),
                        record.get(FIELD_WINE_UPDATED_AT),
                        record.get(FIELD_STOCK_UPDATED_AT)
                ));
    }


    private List<Condition> buildConditions(AvailableWineFilter filter) {
        List<Condition> conditions = new ArrayList<>();

        addEqualsLowerIfPresent(conditions, FIELD_WINE_COLOR, normalizeOrNull(filter.color()));

        addLikeIgnoreCaseIfPresent(conditions, FIELD_COUNTRY_NAME, filter.country());
        addLikeIgnoreCaseIfPresent(conditions, FIELD_REGION_NAME, filter.region());
        addLikeIgnoreCaseIfPresent(conditions, FIELD_GRAPE_VARIETIES, filter.grape());

        if (filter.maxPriceRub() != null) {
            conditions.add(FIELD_PRICE_RUB.le(new BigDecimal(filter.maxPriceRub())));
        }
        if (filter.minVintageYear() != null) {
            conditions.add(FIELD_VINTAGE_YEAR.ge(filter.minVintageYear()));
        }
        if (filter.maxVintageYear() != null) {
            conditions.add(FIELD_VINTAGE_YEAR.le(filter.maxVintageYear()));
        }
        if (filter.freeQuery() != null && !filter.freeQuery().isBlank()) {
            String likePattern = "%" + filter.freeQuery().trim() + "%";
            conditions.add(
                    FIELD_WINE_NAME.likeIgnoreCase(likePattern)
                            .or(FIELD_PRODUCER_NAME.likeIgnoreCase(likePattern))
                            .or(FIELD_REGION_NAME.likeIgnoreCase(likePattern))
                            .or(FIELD_GRAPE_VARIETIES.likeIgnoreCase(likePattern))
            );
        }
        return conditions;
    }

    private void addLikeIgnoreCaseIfPresent(List<Condition> conditions, Field<String> field, String value) {
        if (value != null && !value.isBlank()) {
            String pattern = "%" + value.trim() + "%";
            conditions.add(field.likeIgnoreCase(pattern));
        }
    }

    private void addEqualsLowerIfPresent(List<Condition> conditions, Field<String> field, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(DSL.lower(field).eq(value.toLowerCase()));
        }
    }

    private String normalizeOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }
}
