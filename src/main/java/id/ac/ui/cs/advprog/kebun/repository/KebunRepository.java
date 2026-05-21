package id.ac.ui.cs.advprog.kebun.repository;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.Optional;

public interface KebunRepository {
    void acquireGlobalWriteLock();

    boolean existsByCode(String code);

    boolean existsIntersecting(Polygon polygon);

    boolean existsIntersectingExcludingCode(Polygon polygon, String excludedCode);

    Kebun create(Kebun kebun);

    Kebun update(Kebun kebun);

    Optional<Kebun> findByCode(String code);

    List<Kebun> findByNameContainingIgnoreCase(String name);

    List<Kebun> findByNameAndCodeContainingIgnoreCase(String name, String code);

    boolean existsActiveMandorByKebunCode(String code);

    void assignMandor(String kebunCode, String mandorId);

    void unassignMandor(String kebunCode, String mandorId);

    void unassignMandorFromAnyKebun(String mandorId);

    void unassignAnyMandorFromKebun(String kebunCode);

    Optional<String> findMandorIdByKebunCode(String kebunCode);

    List<String> findSupirIdsByKebunCode(String kebunCode);

    void assignSupir(String kebunCode, String supirId);

    void unassignSupir(String kebunCode, String supirId);

    void unassignSupirFromAnyKebun(String supirId);

    void deleteByCode(String code);

    Optional<Kebun> findAssignedKebunByMandorId(String mandorId);
}
