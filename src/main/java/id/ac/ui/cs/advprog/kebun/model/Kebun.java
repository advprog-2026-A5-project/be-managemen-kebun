package id.ac.ui.cs.advprog.kebun.model;

import java.util.List;

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
            if (coordinates != null && coordinates.size() != 4) {
                throw new IllegalArgumentException("Kebun must have exactly 4 coordinate points");
            }
            return new Kebun(this);
        }
    }

    public static class Point {
        private final double x;
        private final double y;

        public Point(double x, double y) {
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
