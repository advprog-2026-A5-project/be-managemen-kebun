package id.ac.ui.cs.advprog.kebun.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class KebunSchemaInitializer {

    public KebunSchemaInitializer(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS kebun (
                    code VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    luas DOUBLE PRECISION NOT NULL,
                    coordinates_json TEXT NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS kebun_mandor (
                    kebun_code VARCHAR(64) NOT NULL REFERENCES kebun(code) ON DELETE CASCADE,
                    mandor_id VARCHAR(128) NOT NULL,
                    PRIMARY KEY (kebun_code, mandor_id)
                )
                """);
    }
}
