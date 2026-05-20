package id.ac.ui.cs.advprog.kebun.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@JsonDeserialize(builder = Kebun.Builder.class)
public class Kebun {
    private final String name;
    private final String code;
    private final double luas;
    private final List<Point> coordinates;

    private Kebun(Builder builder) {
        this.name = builder.name;
        this.code = builder.code;
        this.luas = builder.luas;
        this.coordinates = builder.coordinates;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public double getLuas() {
        return luas;
    }

    public List<Point> getCoordinates() {
        return coordinates;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String name;
        private String code;
        private double luas;
        private List<Point> coordinates;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder luas(double luas) {
            this.luas = luas;
            return this;
        }

        public Builder coordinates(List<Point> coordinates) {
            this.coordinates = coordinates;
            return this;
        }

        public Kebun build() {
            if (coordinates != null) {
                if (coordinates.size() != 4) {
                    throw new IllegalArgumentException("Kebun must have exactly 4 coordinate points");
                }
                if (!formsValidQuadrilateral(coordinates)) {
                    throw new IllegalArgumentException("Kebun coordinates must form a valid 4-sided polygon");
                }
            }
            return new Kebun(this);
        }

        private boolean formsValidQuadrilateral(List<Point> points) {
            Set<String> uniquePoints = points.stream()
                    .map(p -> String.format(Locale.US, "%.9f,%.9f", p.getX(), p.getY()))
                    .collect(Collectors.toSet());
            if (uniquePoints.size() != 4) {
                return false;
            }

            List<Point> ordered = orderByAngle(points);

            for (int i = 0; i < ordered.size(); i++) {
                Point a = ordered.get(i);
                Point b = ordered.get((i + 1) % ordered.size());
                Point c = ordered.get((i + 2) % ordered.size());
                if (Math.abs(cross(a, b, c)) < 1e-9) {
                    return false;
                }
            }

            return polygonArea(ordered) > 1e-9;
        }

        private List<Point> orderByAngle(List<Point> points) {
            double centerX = points.stream().mapToDouble(Point::getX).average().orElse(0.0);
            double centerY = points.stream().mapToDouble(Point::getY).average().orElse(0.0);

            List<Point> ordered = new ArrayList<>(points);
            ordered.sort(Comparator.comparingDouble(p -> Math.atan2(p.getY() - centerY, p.getX() - centerX)));
            return ordered;
        }

        private double cross(Point a, Point b, Point c) {
            return (b.getX() - a.getX()) * (c.getY() - a.getY())
                    - (b.getY() - a.getY()) * (c.getX() - a.getX());
        }

        private double polygonArea(List<Point> points) {
            double sum = 0.0;
            for (int i = 0; i < points.size(); i++) {
                Point current = points.get(i);
                Point next = points.get((i + 1) % points.size());
                sum += current.getX() * next.getY() - next.getX() * current.getY();
            }
            return Math.abs(sum) / 2.0;
        }
    }

    public static class Point {
        private final double x;
        private final double y;

        @JsonCreator
        public Point(@JsonProperty("x") double x, @JsonProperty("y") double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }
}
