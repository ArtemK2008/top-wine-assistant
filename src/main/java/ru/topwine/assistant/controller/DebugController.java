package ru.topwine.assistant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@RestController
@RequestMapping("/db")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final DataSource dataSource;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            rs.next();
            return ResponseEntity.ok("DB OK: " + rs.getInt(1));
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<String> listTablesWithCounts() throws Exception {
        String sql = """
                SELECT n.nspname  AS schema,
                       c.relname  AS table,
                       pg_total_relation_size(c.oid) AS bytes_total
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE c.relkind IN ('r','p')
                  AND n.nspname NOT IN ('pg_catalog','information_schema')
                ORDER BY n.nspname, c.relname
                """;

        StringBuilder out = new StringBuilder();
        int count = 0;

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String schema = rs.getString("schema");
                String table = rs.getString("table");
                long size = rs.getLong("bytes_total");

                long rows = countRows(conn, schema, table);

                String line = String.format("TABLE %s.%s — rows=%d, size≈%s",
                        schema, table, rows, humanBytes(size));

                log.info("[DB] {}", line);
                out.append(line).append('\n');
                count++;
            }
        }
        if (count == 0) out.append("(пусто — таблиц не найдено)");
        return ResponseEntity.ok("Найдено таблиц: " + count + "\n" + out);
    }

    @GetMapping("/describe/{schema}/{table}")
    public ResponseEntity<String> describeTable(@PathVariable String schema,
                                                @PathVariable String table) throws Exception {
        String sql = """
                SELECT column_name,
                       data_type,
                       is_nullable,
                       column_default
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ?
                ORDER BY ordinal_position
                """;
        StringBuilder out = new StringBuilder("Колонки " + schema + "." + table + ":\n");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                int i = 0;
                while (rs.next()) {
                    i++;
                    String name = rs.getString("column_name");
                    String type = rs.getString("data_type");
                    String nullable = rs.getString("is_nullable");
                    String def = rs.getString("column_default");
                    String line = String.format("- %s %s %s %s",
                            name,
                            type,
                            ("NO".equalsIgnoreCase(nullable) ? "NOT NULL" : "NULL"),
                            (def != null ? "DEFAULT " + def : ""));
                    log.info("[DB] {}", line);
                    out.append(line).append('\n');
                }
                if (i == 0) out.append("(колонки не найдены — проверь имя/schema)");
            }
        }
        return ResponseEntity.ok(out.toString());
    }


    private static long countRows(Connection conn, String schema, String table) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + quoteIdent(schema) + "." + quoteIdent(table);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String humanBytes(long b) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double val = b;
        int u = 0;
        while (val >= 1024 && u < units.length - 1) {
            val /= 1024.0;
            u++;
        }
        return String.format("%.1f %s", val, units[u]);
    }

    private static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }
}
