package ru.topwine.assistant.service;

public interface DebugService {
    String ping();

    String listTablesPretty();

    String describeTablePretty(String schema, String table);
}
