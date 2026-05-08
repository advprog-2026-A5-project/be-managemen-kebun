package id.ac.ui.cs.advprog.kebun.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

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
                if (!formsSquare(coordinates)) {
                    throw new IllegalArgumentException("Kebun coordinates must form a square");
                }
            }
            return new Kebun(this);
        }

        private boolean formsSquare(List<Point> points) {
            double[] distances = new double[6];
            int idx = 0;

            for (int i = 0; i < points.size(); i++) {
                for (int j = i + 1; j < points.size(); j++) {
                    distances[idx++] = squaredDistance(points.get(i), points.get(j));
                }
            }

            java.util.Arrays.sort(distances);

            double side = distances[0];
            double diag = distances[4];

            if (side <= 0) {
                return false;
            }

            return almostEqual(distances[0], side)
                    && almostEqual(distances[1], side)
                    && almostEqual(distances[2], side)
                    && almostEqual(distances[3], side)
                    && almostEqual(distances[4], diag)
                    && almostEqual(distances[5], diag)
                    && almostEqual(diag, 2 * side);
        }

        private double squaredDistance(Point a, Point b) {
            double dx = a.getX() - b.getX();
            double dy = a.getY() - b.getY();
            return dx * dx + dy * dy;
        }

        private boolean almostEqual(double a, double b) {
            return Math.abs(a - b) < 1e-9;
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
