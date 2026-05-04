package id.ac.ui.cs.advprog.kebun.model;

public class Kebun {
    private final String name;
    private final String code;
    private final double luas;

    private Kebun(Builder builder) {
        this.name = builder.name;
        this.code = builder.code;
        this.luas = builder.luas;
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

    public static class Builder {
        private String name;
        private String code;
        private double luas;

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

        public Kebun build() {
            return new Kebun(this);
        }
    }
}
