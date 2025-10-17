package ru.topwine.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@Repository
@RequiredArgsConstructor
public class DebugRepository {

    private final DSLContext dsl;

    public int ping() {
        return dsl.fetchOne("select 1 as v").get("v", Integer.class);
    }

    /**
     * Таблицы + размер + кол-во строк
     */
    public List<Map<String, Object>> listTablesWithCounts() {
        Result<Record> tableRecords = dsl.fetch("""
                    select t.table_schema as schema,
                           t.table_name   as table
                    from information_schema.tables t
                    where t.table_type='BASE TABLE'
                      and t.table_schema not in ('pg_catalog','information_schema')
                    order by t.table_schema, t.table_name
                """);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Record record : tableRecords) {
            String schemaName = record.get("schema", String.class);
            String tableName = record.get("table", String.class);

            Long sizeBytes = dsl.fetchOne(
                    "select pg_total_relation_size(format('%I.%I', ?, ?)::regclass) as bytes_total",
                    schemaName, tableName
            ).get("bytes_total", Long.class);
            if (sizeBytes == null) sizeBytes = 0L;

            Long rowCount = dsl.selectCount()
                    .from(table(name(schemaName, tableName)))
                    .fetchOne(0, Long.class);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("schema", schemaName);
            row.put("table", tableName);
            row.put("rows", rowCount == null ? 0L : rowCount);
            row.put("bytes_total", sizeBytes);

            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> describeColumns(String schemaName, String tableName) {
        Result<Record> columnRecords = dsl.fetch("""
                    select column_name,
                           data_type,
                           is_nullable,
                           column_default
                    from information_schema.columns
                    where table_schema = ? and table_name = ?
                    order by ordinal_position
                """, schemaName, tableName);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Record record : columnRecords) {
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("column_name", record.get("column_name", String.class));
            column.put("data_type", record.get("data_type", String.class));
            column.put("is_nullable", record.get("is_nullable", String.class));
            column.put("column_default", record.get("column_default", String.class));
            result.add(column);
        }
        return result;
    }
}
