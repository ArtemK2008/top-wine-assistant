package ru.topwine.assistant.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AvailableWine(
        long stockId,
        long wineId,
        String wineName,
        String producerName,
        String countryName,
        String regionName,
        String grapeVarieties,
        String wineColor,
        Integer vintageYear,
        int bottleSizeMl,
        BigDecimal alcoholByVolume,
        int quantityBottles,
        BigDecimal priceRub,
        String tagsJson,
        String descriptionNotes,
        OffsetDateTime wineCreatedAt,
        OffsetDateTime wineUpdatedAt,
        OffsetDateTime stockUpdatedAt
) {
}