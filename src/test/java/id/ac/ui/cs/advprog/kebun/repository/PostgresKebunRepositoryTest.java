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
import static org.mockito.Mockito.times;
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
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsIntersectingShouldReturnTrueWhenPolygonIntersects() {
        List<Kebun.Point> existingPoints = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );

        Kebun existing = Kebun.builder()
                .name("Existing")
                .code("KBNA01")
                .luas(100.0)
                .coordinates(existingPoints)
                .build();

        doReturn(List.of(existing)).when(jdbcTemplate).query(anyString(), any(RowMapper.class));

        Polygon candidate = id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper.toPolygon(existingPoints);

        assertTrue(repository.existsIntersecting(candidate));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsIntersectingShouldReturnFalseWhenNoRows() {
        doReturn(List.of()).when(jdbcTemplate).query(anyString(), any(RowMapper.class));

        List<Kebun.Point> points = List.of(
                new Kebun.Point(10, 10),
                new Kebun.Point(10, 12),
                new Kebun.Point(12, 12),
                new Kebun.Point(12, 10)
        );
        Polygon candidate = id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper.toPolygon(points);

        assertFalse(repository.existsIntersecting(candidate));
    }

    @Test
    void saveShouldInsertOrUpdateKebun() throws Exception {
        Kebun kebun = Kebun.builder()
                .name("Alpha")
                .code("KBNA01")
                .luas(123.45)
                .coordinates(List.of(
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 2),
                        new Kebun.Point(2, 2),
                        new Kebun.Point(2, 1)
                ))
                .build();

        when(objectMapper.writeValueAsString(kebun.getCoordinates())).thenReturn("[{\"x\":1.0,\"y\":1.0}]");

        Kebun saved = repository.save(kebun);

        assertEquals(kebun, saved);
        verify(jdbcTemplate).update(anyString(), eq("KBNA01"), eq("Alpha"), eq(123.45), anyString());
    }

    @Test
    void saveShouldThrowWhenSerializationFails() throws Exception {
        Kebun kebun = Kebun.builder()
                .name("Alpha")
                .code("KBNA01")
                .luas(123.45)
                .coordinates(List.of(
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 2),
                        new Kebun.Point(2, 2),
                        new Kebun.Point(2, 1)
                ))
                .build();

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        assertThrows(IllegalStateException.class, () -> repository.save(kebun));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCodeShouldReturnKebunWhenExists() {
        Kebun kebun = Kebun.builder()
                .name("A")
                .code("KBNA01")
                .luas(1.0)
                .coordinates(List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 1),
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 0)
                ))
                .build();
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
        Kebun kebun = Kebun.builder()
                .name("Sawit")
                .code("KBNA01")
                .luas(1.0)
                .coordinates(List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 1),
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 0)
                ))
                .build();
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
        Kebun kebun = Kebun.builder()
                .name("Sawit")
                .code("KBNA01")
                .luas(1.0)
                .coordinates(List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 1),
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 0)
                ))
                .build();
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
        List<Kebun.Point> points = List.of(
                new Kebun.Point(1, 1),
                new Kebun.Point(1, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 1)
        );
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
                .thenThrow(new JsonProcessingException("bad json") {});

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
}
