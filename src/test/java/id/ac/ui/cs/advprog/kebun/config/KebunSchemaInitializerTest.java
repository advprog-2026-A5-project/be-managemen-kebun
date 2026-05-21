package id.ac.ui.cs.advprog.kebun.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class KebunSchemaInitializerTest {

    @Test
    void constructorShouldCreateRequiredTables() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        new KebunSchemaInitializer(jdbcTemplate);

        verify(jdbcTemplate, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS kebun ("));
        verify(jdbcTemplate, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS kebun_mandor ("));
        verify(jdbcTemplate, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS kebun_supir ("));
        verify(jdbcTemplate, times(2)).execute(contains("REFERENCES kebun(code) ON DELETE CASCADE"));
    }
}
