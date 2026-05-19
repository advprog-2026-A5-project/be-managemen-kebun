package id.ac.ui.cs.advprog.kebun.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.locationtech.jts.geom.Polygon;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgresKebunRepository implements KebunRepository {

    private static final TypeReference<List<Kebun.Point>> POINTS_TYPE = new TypeReference<>() {};
    private static final long KEBUN_WRITE_LOCK_KEY = 0x4B4542554EL;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<Kebun> kebunRowMapper = this::mapKebun;

    public PostgresKebunRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        initializeSchema();
    }

    @Override
    public void acquireGlobalWriteLock() {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", rs -> { }, KEBUN_WRITE_LOCK_KEY);
    }

    @Override
    public boolean existsIntersecting(Polygon polygon) {
        List<Kebun> allKebun = jdbcTemplate.query(
                "SELECT name, code, luas, coordinates_json FROM kebun",
                kebunRowMapper
        );

        return allKebun.stream()
                .map(Kebun::getCoordinates)
                .filter(points -> points != null && !points.isEmpty())
                .map(GeometryMapper::toPolygon)
                .anyMatch(existing -> existing.intersects(polygon));
    }

    @Override
    public Kebun save(Kebun kebun) {
        String sql = """
                INSERT INTO kebun (code, name, luas, coordinates_json)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (code) DO UPDATE SET
                    name = EXCLUDED.name,
                    luas = EXCLUDED.luas,
                    coordinates_json = EXCLUDED.coordinates_json
                """;

        jdbcTemplate.update(
                sql,
                kebun.getCode(),
                kebun.getName(),
                kebun.getLuas(),
                toJson(kebun.getCoordinates())
        );
        return kebun;
    }

    @Override
    public Optional<Kebun> findByCode(String code) {
        try {
            Kebun kebun = jdbcTemplate.queryForObject(
                    "SELECT name, code, luas, coordinates_json FROM kebun WHERE code = ?",
                    kebunRowMapper,
                    code
            );
            return Optional.ofNullable(kebun);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Kebun> findByNameContainingIgnoreCase(String name) {
        String keyword = name == null ? "" : name;
        return jdbcTemplate.query(
                "SELECT name, code, luas, coordinates_json FROM kebun WHERE LOWER(name) LIKE LOWER(?)",
                kebunRowMapper,
                "%" + keyword + "%"
        );
    }

    @Override
    public boolean existsActiveMandorByKebunCode(String code) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kebun_mandor WHERE kebun_code = ?",
                Integer.class,
                code
        );
        return count != null && count > 0;
    }

    @Override
    public void assignMandor(String kebunCode, String mandorId) {
        jdbcTemplate.update(
                "INSERT INTO kebun_mandor (kebun_code, mandor_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                kebunCode,
                mandorId
        );
    }

    @Override
    public void unassignMandor(String kebunCode, String mandorId) {
        jdbcTemplate.update(
                "DELETE FROM kebun_mandor WHERE kebun_code = ? AND mandor_id = ?",
                kebunCode,
                mandorId
        );
    }

    @Override
    public void deleteByCode(String code) {
        jdbcTemplate.update("DELETE FROM kebun WHERE code = ?", code);
    }

    @Override
    public Optional<Kebun> findAssignedKebunByMandorId(String mandorId) {
        try {
            Kebun kebun = jdbcTemplate.queryForObject(
                    """
                    SELECT k.name, k.code, k.luas, k.coordinates_json
                    FROM kebun k
                    JOIN kebun_mandor km ON km.kebun_code = k.code
                    WHERE km.mandor_id = ?
                    LIMIT 1
                    """,
                    kebunRowMapper,
                    mandorId
            );
            return Optional.ofNullable(kebun);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Kebun mapKebun(ResultSet rs, int rowNum) throws SQLException {
        return Kebun.builder()
                .name(rs.getString("name"))
                .code(rs.getString("code"))
                .luas(rs.getDouble("luas"))
                .coordinates(fromJson(rs.getString("coordinates_json")))
                .build();
    }

    private void initializeSchema() {
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

    private String toJson(List<Kebun.Point> points) {
        try {
            return objectMapper.writeValueAsString(points == null ? Collections.emptyList() : points);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize kebun coordinates", ex);
        }
    }

    private List<Kebun.Point> fromJson(String json) {
        try {
            return objectMapper.readValue(json, POINTS_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize kebun coordinates", ex);
        }
    }
}
