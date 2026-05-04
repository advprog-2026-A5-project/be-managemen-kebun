package id.ac.ui.cs.advprog.kebun.mapper;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

public final class GeometryMapper {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private GeometryMapper() {
    }

    public static Polygon toPolygon(List<Kebun.Point> points) {
        Coordinate[] coordinates = new Coordinate[points.size() + 1];

        for (int i = 0; i < points.size(); i++) {
            coordinates[i] = new Coordinate(points.get(i).getX(), points.get(i).getY());
        }

        coordinates[points.size()] = new Coordinate(points.get(0).getX(), points.get(0).getY());

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coordinates);
        return GEOMETRY_FACTORY.createPolygon(shell);
    }
}
