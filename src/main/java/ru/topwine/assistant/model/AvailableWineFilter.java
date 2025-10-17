package ru.topwine.assistant.model;

public record AvailableWineFilter(
        String freeQuery,
        String color,
        String country,
        String region,
        String grape,
        Integer maxPriceRub,
        Integer minVintageYear,
        Integer maxVintageYear
) {
    public static AvailableWineFilter empty() {
        return new AvailableWineFilter(null, null, null, null, null, null, null, null);
    }
}