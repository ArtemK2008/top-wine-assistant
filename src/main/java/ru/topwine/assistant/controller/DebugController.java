package ru.topwine.assistant.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@RestController
@RequiredArgsConstructor
public class DebugController {
    private final DataSource dataSource;

    @GetMapping("/db/ping")
    public String ping() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            rs.next();
            return "DB OK: " + rs.getInt(1);
        }
    }
}
