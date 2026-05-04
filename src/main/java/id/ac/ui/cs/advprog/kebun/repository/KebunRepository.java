package id.ac.ui.cs.advprog.kebun.repository;

import org.locationtech.jts.geom.Polygon;

public interface KebunRepository {
    boolean existsIntersecting(Polygon polygon);
}
