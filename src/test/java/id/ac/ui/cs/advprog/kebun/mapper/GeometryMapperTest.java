package id.ac.ui.cs.advprog.kebun.mapper;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryMapperTest {

    @Test
    void toPolygonShouldConvertFourPointsIntoClosedSquarePolygon() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );

        Polygon polygon = GeometryMapper.toPolygon(points);

        Coordinate[] coordinates = polygon.getCoordinates();

        assertEquals(5, coordinates.length);
        assertEquals(coordinates[0], coordinates[coordinates.length - 1]);
        assertEquals(0.0, coordinates[0].x);
        assertEquals(0.0, coordinates[0].y);
        assertEquals(2.0, coordinates[2].x);
        assertEquals(2.0, coordinates[2].y);
    }
}
