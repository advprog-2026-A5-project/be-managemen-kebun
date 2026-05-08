package id.ac.ui.cs.advprog.kebun.validation;

import id.ac.ui.cs.advprog.kebun.model.Kebun;

import java.util.List;

public interface OverlapValidator {
    void validateNoOverlap(List<Kebun.Point> points);
}
