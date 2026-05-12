package id.ac.ui.cs.advprog.kebun.repository;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.Optional;

public interface KebunRepository {
    void acquireGlobalWriteLock();

    boolean existsIntersecting(Polygon polygon);

    Kebun save(Kebun kebun);

    Optional<Kebun> findByCode(String code);

    List<Kebun> findByNameContainingIgnoreCase(String name);

    boolean existsActiveMandorByKebunCode(String code);

    void assignMandor(String kebunCode, String mandorId);

    void unassignMandor(String kebunCode, String mandorId);

    void deleteByCode(String code);
}
