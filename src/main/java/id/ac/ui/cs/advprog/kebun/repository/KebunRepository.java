package id.ac.ui.cs.advprog.kebun.repository;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.locationtech.jts.geom.Polygon;

public interface KebunRepository {
    boolean existsIntersecting(Polygon polygon);

    Kebun save(Kebun kebun);
}
