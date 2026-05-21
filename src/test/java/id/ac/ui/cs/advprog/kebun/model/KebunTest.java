package id.ac.ui.cs.advprog.kebun.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KebunTest {

    @Test
    void builderShouldCreateKebunWithBasicProperties() {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(12.5)
                .coordinates(squarePoints())
                .build();

        assertEquals("Kebun Sawit A", kebun.getName());
        assertEquals("KBNA01", kebun.getCode());
        assertEquals(12.5, kebun.getLuas());
    }

    @Test
    void builderShouldSupportDifferentBasicValues() {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit B")
                .code("KBNB02")
                .luas(24.0)
                .coordinates(squarePoints())
                .build();

        assertEquals("Kebun Sawit B", kebun.getName());
        assertEquals("KBNB02", kebun.getCode());
        assertEquals(24.0, kebun.getLuas());
    }

    @Test
    void builderShouldRejectWhenCoordinatePointsLessThanFour() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 1),
                new Kebun.Point(1, 1)
        );

        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit C")
                .code("KBNC03")
                .luas(30.0)
                .coordinates(points)
                .build());
    }

    @Test
    void builderShouldRejectWhenCoordinatePointsMoreThanFour() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 1),
                new Kebun.Point(1, 1),
                new Kebun.Point(1, 0),
                new Kebun.Point(2, 0)
        );

        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit D")
                .code("KBND04")
                .luas(40.0)
                .coordinates(points)
                .build());
    }

    @Test
    void builderShouldAllowExactlyFourCoordinatePoints() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 1),
                new Kebun.Point(1, 1),
                new Kebun.Point(1, 0)
        );

        assertDoesNotThrow(() -> Kebun.builder()
                .name("Kebun Sawit E")
                .code("KBNE05")
                .luas(50.0)
                .coordinates(points)
                .build());
    }

    @Test
    void builderShouldAllowWhenCoordinatesFormValidNonSquareQuadrilateral() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 3),
                new Kebun.Point(2, 2),
                new Kebun.Point(3, 0)
        );

        assertDoesNotThrow(() -> Kebun.builder()
                .name("Kebun Sawit F")
                .code("KBNF06")
                .luas(60.0)
                .coordinates(points)
                .build());
    }

    @Test
    void builderShouldRejectWhenCoordinatesDoNotFormValidQuadrilateral() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(0, 4),
                new Kebun.Point(2, 0)
        );

        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit G")
                .code("KBNG07")
                .luas(70.0)
                .coordinates(points)
                .build());
    }

    @Test
    void builderShouldRejectWhenCoordinatesContainDuplicatePoint() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 0),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );

        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit H")
                .code("KBNH08")
                .luas(70.0)
                .coordinates(points)
                .build());
    }

    @Test
    void builderShouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name(" ")
                .code("KBNI09")
                .luas(10.0)
                .coordinates(squarePoints())
                .build());
    }

    @Test
    void builderShouldRejectNullName() {
        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name(null)
                .code("KBNI09")
                .luas(10.0)
                .coordinates(squarePoints())
                .build());
    }

    @Test
    void builderShouldRejectNullCode() {
        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit I")
                .code(null)
                .luas(10.0)
                .coordinates(squarePoints())
                .build());
    }

    @Test
    void builderShouldRejectBlankCode() {
        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit I")
                .code(" ")
                .luas(10.0)
                .coordinates(squarePoints())
                .build());
    }

    @Test
    void builderShouldRejectNonPositiveLuas() {
        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit J")
                .code("KBNJ10")
                .luas(0.0)
                .coordinates(squarePoints())
                .build());
    }

    @Test
    void builderShouldRejectNullCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit K")
                .code("KBNK11")
                .luas(10.0)
                .coordinates(null)
                .build());
    }

    @Test
    void pointShouldRejectNonFiniteCoordinateValues() {
        assertThrows(IllegalArgumentException.class, () -> new Kebun.Point(Double.NaN, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Kebun.Point(1.0, Double.POSITIVE_INFINITY));
    }

    @Test
    void builderShouldRejectNullPointInCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit L")
                .code("KBNL12")
                .luas(10.0)
                .coordinates(Arrays.asList(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 1),
                        null,
                        new Kebun.Point(1, 0)
                ))
                .build());
    }

    private List<Kebun.Point> squarePoints() {
        return List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 1),
                new Kebun.Point(1, 1),
                new Kebun.Point(1, 0)
        );
    }
}
