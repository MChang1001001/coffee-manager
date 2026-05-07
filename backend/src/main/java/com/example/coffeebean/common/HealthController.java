package com.example.coffeebean.common;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Integer dbResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return ApiResponse.success(Map.of(
                "status", "ok",
                "database", dbResult != null && dbResult == 1 ? "ok" : "unknown"
        ));
    }
}
