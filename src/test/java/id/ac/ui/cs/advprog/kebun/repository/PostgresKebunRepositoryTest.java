package id.ac.ui.cs.advprog.kebun.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Polygon;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresKebunRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private PostgresKebunRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PostgresKebunRepository(jdbcTemplate, objectMapper);
    }

    @Test
    void acquireGlobalWriteLockShouldInvokeAdvisoryLockQuery() {
        repository.acquireGlobalWriteLock();
        verify(jdbcTemplate).query(anyString(), any(org.springframework.jdbc.core.RowCallbackHandler.class), eq(0x4B4542554EL));
    }

    @Test
    void existsByCodeShouldReturnTrueWhenCountPositive() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("KBNA01"))).thenReturn(1);

        assertTrue(repository.existsByCode("KBNA01"));
    }

    @Test
    void existsByCodeShouldReturnFalseWhenCountZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("KBNA01"))).thenReturn(0);

        assertFalse(repository.existsByCode("KBNA01"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsIntersectingShouldReturnTrueWhenPolygonIntersects() {
        Kebun existing = kebun("Existing", "KBNA01", 100.0, squarePoints());
        doReturn(List.of(existing)).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(), any());

        Polygon candidate = id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper.toPolygon(squarePoints());

        assertTrue(repository.existsIntersecting(candidate));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsIntersectingShouldReturnFalseWhenNoRows() {
        doReturn(List.of()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(), any());

        Polygon candidate = id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper.toPolygon(offsetSquarePoints());

        assertFalse(repository.existsIntersecting(candidate));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsIntersectingExcludingCodeShouldIgnoreExcludedKebun() {
        doReturn(List.of()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("KBNA01"), eq("KBNA01"));

        Polygon candidate = id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper.toPolygon(squarePoints());

        assertFalse(repository.existsIntersectingExcludingCode(candidate, "KBNA01"));
    }

    @Test
    void createShouldInsertKebunWithoutUpsert() throws Exception {
        Kebun kebun = kebun("Alpha", "KBNA01", 123.45, squarePoints());
        when(objectMapper.writeValueAsString(kebun.getCoordinates())).thenReturn("[{\"x\":0.0,\"y\":0.0}]");

        Kebun created = repository.create(kebun);

        assertEquals(kebun, created);
        verify(jdbcTemplate).update(
                eq("INSERT INTO kebun (code, name, luas, coordinates_json) VALUES (?, ?, ?, ?)"),
                eq("KBNA01"),
                eq("Alpha"),
                eq(123.45),
                anyString()
        );
    }

    @Test
    void createShouldThrowWhenSerializationFails() throws Exception {
        Kebun kebun = kebun("Alpha", "KBNA01", 123.45, squarePoints());
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });

        assertThrows(IllegalStateException.class, () -> repository.create(kebun));
    }

    @Test
    void updateShouldPersistByCode() throws Exception {
        Kebun kebun = kebun("Alpha Updated", "KBNA01", 200.0, offsetSquarePoints());
        when(objectMapper.writeValueAsString(kebun.getCoordinates())).thenReturn("[{\"x\":3.0,\"y\":0.0}]");

        Kebun updated = repository.update(kebun);

        assertEquals(kebun, updated);
        verify(jdbcTemplate).update(
                eq("UPDATE kebun SET name = ?, luas = ?, coordinates_json = ? WHERE code = ?"),
                eq("Alpha Updated"),
                eq(200.0),
                anyString(),
                eq("KBNA01")
        );
    }

    @Test
    void updateShouldThrowWhenSerializationFails() throws Exception {
        Kebun kebun = kebun("Alpha Updated", "KBNA01", 200.0, offsetSquarePoints());
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });

        assertThrows(IllegalStateException.class, () -> repository.update(kebun));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCodeShouldReturnKebunWhenExists() {
        Kebun kebun = kebun("A", "KBNA01", 1.0, squarePoints());
        doReturn(kebun).when(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq("KBNA01"));

        Optional<Kebun> result = repository.findByCode("KBNA01");

        assertTrue(result.isPresent());
        assertEquals("KBNA01", result.get().getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCodeShouldReturnEmptyWhenNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("MISSING")))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<Kebun> result = repository.findByCode("MISSING");

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByNameShouldDelegateLikeQuery() {
        Kebun kebun = kebun("Sawit", "KBNA01", 1.0, squarePoints());
        doReturn(List.of(kebun)).when(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("%saw%"));

        List<Kebun> result = repository.findByNameContainingIgnoreCase("saw");

        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByNameShouldHandleNullKeyword() {
        doReturn(List.of()).when(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("%%"));

        List<Kebun> result = repository.findByNameContainingIgnoreCase(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByNameAndCodeShouldDelegateCombinedLikeQuery() {
        Kebun kebun = kebun("Sawit", "KBNA01", 1.0, squarePoints());
        doReturn(List.of(kebun))
                .when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), eq("%saw%"), eq("%na0%"));

        List<Kebun> result = repository.findByNameAndCodeContainingIgnoreCase("saw", "na0");

        assertEquals(1, result.size());
    }

    @Test
    void existsActiveMandorShouldReturnTrueWhenCountPositive() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("KBNA01"))).thenReturn(1);

        assertTrue(repository.existsActiveMandorByKebunCode("KBNA01"));
    }

    @Test
    void existsActiveMandorShouldReturnFalseWhenCountNullOrZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("KBNA01"))).thenReturn(0);
        assertFalse(repository.existsActiveMandorByKebunCode("KBNA01"));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("KBNA02"))).thenReturn(null);
        assertFalse(repository.existsActiveMandorByKebunCode("KBNA02"));
    }

    @Test
    void assignMandorShouldInsertRelationship() {
        repository.assignMandor("KBNA01", "M1");
        verify(jdbcTemplate).update(anyString(), eq("KBNA01"), eq("M1"));
    }

    @Test
    void unassignMandorShouldDeleteRelationship() {
        repository.unassignMandor("KBNA01", "M1");
        verify(jdbcTemplate).update(anyString(), eq("KBNA01"), eq("M1"));
    }

    @Test
    void unassignMandorFromAnyKebunShouldDeleteByMandorId() {
        repository.unassignMandorFromAnyKebun("M1");
        verify(jdbcTemplate).update(anyString(), eq("M1"));
    }

    @Test
    void unassignAnyMandorFromKebunShouldDeleteByKebunCode() {
        repository.unassignAnyMandorFromKebun("KBNA01");
        verify(jdbcTemplate).update(anyString(), eq("KBNA01"));
    }

    @Test
    void findMandorIdByKebunCodeShouldReturnValueWhenExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq("KBNA01"))).thenReturn("M1");

        Optional<String> result = repository.findMandorIdByKebunCode("KBNA01");

        assertTrue(result.isPresent());
        assertEquals("M1", result.get());
    }

    @Test
    void findMandorIdByKebunCodeShouldReturnEmptyWhenNoResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq("KBNA01")))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<String> result = repository.findMandorIdByKebunCode("KBNA01");

        assertTrue(result.isEmpty());
    }

    @Test
    void findSupirIdsByKebunCodeShouldReturnRows() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("KBNA01")))
                .thenReturn(List.of("11", "12"));

        List<String> result = repository.findSupirIdsByKebunCode("KBNA01");

        assertEquals(List.of("11", "12"), result);
    }

    @Test
    void assignSupirShouldInsertRelationship() {
        repository.assignSupir("KBNA01", "11");
        verify(jdbcTemplate).update(anyString(), eq("KBNA01"), eq("11"));
    }

    @Test
    void unassignSupirShouldDeleteRelationship() {
        repository.unassignSupir("KBNA01", "11");
        verify(jdbcTemplate).update(anyString(), eq("KBNA01"), eq("11"));
    }

    @Test
    void unassignSupirFromAnyKebunShouldDeleteBySupirId() {
        repository.unassignSupirFromAnyKebun("11");
        verify(jdbcTemplate).update(anyString(), eq("11"));
    }

    @Test
    void deleteByCodeShouldDeleteKebun() {
        repository.deleteByCode("KBNA01");
        verify(jdbcTemplate).update(anyString(), eq("KBNA01"));
    }

    @Test
    void findByCodeShouldMapCoordinatesFromJson() throws Exception {
        List<Kebun.Point> points = squarePoints();
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(points);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("KBNA01")))
                .thenAnswer(invocation -> {
                    RowMapper<Kebun> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rs.getString("name")).thenReturn("Alpha");
                    when(rs.getString("code")).thenReturn("KBNA01");
                    when(rs.getDouble("luas")).thenReturn(50.0);
                    when(rs.getString("coordinates_json")).thenReturn("[{}]");
                    return mapper.mapRow(rs, 0);
                });

        Optional<Kebun> result = repository.findByCode("KBNA01");

        assertTrue(result.isPresent());
        assertEquals(4, result.get().getCoordinates().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCodeShouldThrowWhenDeserializationFails() throws Exception {
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new JsonProcessingException("bad json") { });

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("KBNA01")))
                .thenAnswer(invocation -> {
                    RowMapper<Kebun> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rs.getString("name")).thenReturn("Alpha");
                    when(rs.getString("code")).thenReturn("KBNA01");
                    when(rs.getDouble("luas")).thenReturn(50.0);
                    when(rs.getString("coordinates_json")).thenReturn("[{}]");
                    return mapper.mapRow(rs, 0);
                });

        assertThrows(IllegalStateException.class, () -> repository.findByCode("KBNA01"));
    }

    private Kebun kebun(String name, String code, double luas, List<Kebun.Point> coordinates) {
        return Kebun.builder()
                .name(name)
                .code(code)
                .luas(luas)
                .coordinates(coordinates)
                .build();
    }

    private List<Kebun.Point> squarePoints() {
        return List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );
    }

    private List<Kebun.Point> offsetSquarePoints() {
        return List.of(
                new Kebun.Point(3, 0),
                new Kebun.Point(3, 2),
                new Kebun.Point(5, 2),
                new Kebun.Point(5, 0)
        );
    }
}
