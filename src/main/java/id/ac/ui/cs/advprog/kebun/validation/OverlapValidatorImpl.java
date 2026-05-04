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
        Polygon polygon = GeometryMapper.toPolygon(points);
        boolean intersects = kebunRepository.existsIntersecting(polygon);

        if (intersects) {
            throw new IllegalArgumentException("Kebun coordinates overlap with an existing kebun");
        }
    }
}
