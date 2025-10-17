package ru.topwine.assistant.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.topwine.assistant.repository.DebugRepository;
import ru.topwine.assistant.service.DebugService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DebugServiceImpl implements DebugService {
    private final DebugRepository repository;
    private static final String NO_TABLES_MSG = "(пусто — таблиц не найдено)";
    private static final String NO_COLUMNS_MSG = "(колонки не найдены — проверь имя/schema)";
    private static final Locale RU = new Locale("ru", "RU");


    @Override
    public String ping() {
        return "DB OK: " + repository.ping();
    }

    @Override
    public String listTablesPretty() {
        List<Map<String, Object>> rows = repository.listTablesWithCounts();
        if (rows.isEmpty()) return NO_TABLES_MSG;

        List<TableInfo> tables = toTableInfo(rows);

        StringBuilder sb = new StringBuilder();
        sb.append("Найдено таблиц: ").append(tables.size()).append('\n');
        for (TableInfo info : tables) {
            sb.append(String.format(
                    RU,
                    "TABLE %s.%s — rows=%d, size≈%s%n",
                    info.schema, info.table, info.rows, humanBytes(info.bytesTotal)
            ));
        }
        return sb.toString();
    }

    @Override
    public String describeTablePretty(String schema, String table) {
        List<Map<String, Object>> rows = repository.describeColumns(schema, table);
        if (rows.isEmpty()) return NO_COLUMNS_MSG;

        List<ColumnInfo> columns = toColumnInfo(rows);

        StringBuilder sb = new StringBuilder();
        sb.append("Колонки ").append(schema).append('.').append(table).append(":\n");
        for (ColumnInfo column : columns) {
            sb.append("- ")
                    .append(column.name).append(' ')
                    .append(column.dataType).append(' ')
                    .append(column.notNull ? "NOT NULL" : "NULL");

            if (column.defaultValue != null && !column.defaultValue.isBlank()) {
                sb.append(' ').append("DEFAULT ").append(column.defaultValue);
            }
            sb.append('\n');
        }
        return sb.toString();
    }


    private static List<TableInfo> toTableInfo(List<Map<String, Object>> rows) {
        List<TableInfo> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            String schema = asString(r.get("schema"));
            String table = asString(r.get("table"));
            long rowsCount = asLong(r.get("rows"));
            long bytesTotal = asLong(r.getOrDefault("bytes_total", 0L));
            out.add(new TableInfo(schema, table, rowsCount, bytesTotal));
        }
        return out;
    }

    private static List<ColumnInfo> toColumnInfo(List<Map<String, Object>> rows) {
        List<ColumnInfo> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            String name = asString(r.get("column_name"));
            String dataType = asString(r.get("data_type"));
            boolean notNull = isNotNullFlag(r.get("is_nullable"));
            String defaultVal = valueToStringOrNull(r.get("column_default"));
            out.add(new ColumnInfo(name, dataType, notNull, defaultVal));
        }
        return out;
    }


    private static String humanBytes(long bytes) {
        if (bytes < 0) bytes = 0;
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = bytes;
        int idx = 0;
        while (value >= 1024 && idx < units.length - 1) {
            value /= 1024.0;
            idx++;
        }
        return String.format(RU, "%.1f %s", value, units[idx]);
    }

    private static boolean isNotNullFlag(Object isNullableRaw) {
        if (isNullableRaw == null) return false;
        String s = String.valueOf(isNullableRaw).trim();
        if ("NO".equalsIgnoreCase(s)) return true;
        if ("YES".equalsIgnoreCase(s)) return false;
        if ("true".equalsIgnoreCase(s)) return false;
        return "false".equalsIgnoreCase(s);
    }

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static long asLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String valueToStringOrNull(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private record TableInfo(String schema, String table, long rows, long bytesTotal) {
    }

    private record ColumnInfo(String name, String dataType, boolean notNull, String defaultValue) {
    }
}
