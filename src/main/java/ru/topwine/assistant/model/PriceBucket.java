package ru.topwine.assistant.model;

public record PriceBucket(Integer minInclusive, Integer maxExclusive, int targetCount) {
}