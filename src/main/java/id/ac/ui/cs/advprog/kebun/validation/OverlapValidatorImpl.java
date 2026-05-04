package id.ac.ui.cs.advprog.kebun.validation;

import id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OverlapValidatorImpl implements OverlapValidator {

    private final KebunRepository kebunRepository;

    public OverlapValidatorImpl(KebunRepository kebunRepository) {
        this.kebunRepository = kebunRepository;
    }

    @Override
    public void validateNoOverlap(List<Kebun.Point> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("Kebun coordinates are required for overlap validation");
        }

        Polygon polygon = GeometryMapper.toPolygon(points);
        if (kebunRepository.existsIntersecting(polygon)) {
            throw new IllegalArgumentException("Kebun coordinates overlap with an existing kebun");
        }
    }
}
