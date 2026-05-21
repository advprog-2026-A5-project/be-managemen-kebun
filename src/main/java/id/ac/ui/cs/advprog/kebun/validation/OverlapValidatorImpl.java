package id.ac.ui.cs.advprog.kebun.validation;

import id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OverlapValidatorImpl implements OverlapValidator {
    private static final String ERR_COORDINATES_REQUIRED = "Kebun coordinates are required for overlap validation";
    private static final String ERR_OVERLAP = "Kebun coordinates overlap with an existing kebun";

    private final KebunRepository kebunRepository;

    public OverlapValidatorImpl(KebunRepository kebunRepository) {
        this.kebunRepository = kebunRepository;
    }

    @Override
    public void validateNoOverlap(List<Kebun.Point> points) {
        validateNoOverlap(points, null);
    }

    @Override
    public void validateNoOverlap(List<Kebun.Point> points, String excludedCode) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException(ERR_COORDINATES_REQUIRED);
        }

        Polygon polygon = GeometryMapper.toPolygon(points);
        boolean intersects = excludedCode == null
                ? kebunRepository.existsIntersecting(polygon)
                : kebunRepository.existsIntersectingExcludingCode(polygon, excludedCode);
        if (intersects) {
            throw new IllegalArgumentException(ERR_OVERLAP);
        }
    }
}
