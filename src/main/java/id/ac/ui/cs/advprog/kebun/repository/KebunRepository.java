package id.ac.ui.cs.advprog.kebun.repository;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.Optional;

public interface KebunRepository {
    boolean existsIntersecting(Polygon polygon);

    Kebun save(Kebun kebun);

    Optional<Kebun> findByCode(String code);

    List<Kebun> findByNameContainingIgnoreCase(String name);
}
