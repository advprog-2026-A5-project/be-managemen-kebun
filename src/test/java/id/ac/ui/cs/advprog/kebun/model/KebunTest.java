package id.ac.ui.cs.advprog.kebun.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KebunTest {

    @Test
    void builderShouldCreateKebunWithBasicProperties() {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(12.5)
                .build();

        org.junit.jupiter.api.Assertions.assertEquals("Kebun Sawit A", kebun.getName());
        org.junit.jupiter.api.Assertions.assertEquals("KBNA01", kebun.getCode());
        org.junit.jupiter.api.Assertions.assertEquals(12.5, kebun.getLuas());
    }

    @Test
    void builderShouldSupportDifferentBasicValues() {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit B")
                .code("KBNB02")
                .luas(24.0)
                .build();

        org.junit.jupiter.api.Assertions.assertEquals("Kebun Sawit B", kebun.getName());
        org.junit.jupiter.api.Assertions.assertEquals("KBNB02", kebun.getCode());
        org.junit.jupiter.api.Assertions.assertEquals(24.0, kebun.getLuas());
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
    void builderShouldRejectWhenCoordinatesDoNotFormSquare() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(1, 2),
                new Kebun.Point(1, 0)
        );

        assertThrows(IllegalArgumentException.class, () -> Kebun.builder()
                .name("Kebun Sawit F")
                .code("KBNF06")
                .luas(60.0)
                .coordinates(points)
                .build());
    }

    @Test
    void builderShouldAllowWhenCoordinatesFormSquare() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );

        assertDoesNotThrow(() -> Kebun.builder()
                .name("Kebun Sawit G")
                .code("KBNG07")
                .luas(70.0)
                .coordinates(points)
                .build());
    }
}
