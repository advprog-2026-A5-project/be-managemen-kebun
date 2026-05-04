package id.ac.ui.cs.advprog.kebun.validation;

import id.ac.ui.cs.advprog.kebun.mapper.GeometryMapper;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Polygon;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverlapValidatorImplTest {

    @Mock
    private KebunRepository kebunRepository;

    @InjectMocks
    private OverlapValidatorImpl overlapValidator;

    @Test
    void validateShouldPassWhenNoExistingKebunOverlaps() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );

        Polygon newPolygon = GeometryMapper.toPolygon(points);

        when(kebunRepository.existsIntersecting(newPolygon)).thenReturn(false);

        assertDoesNotThrow(() -> overlapValidator.validateNoOverlap(points));
    }
}
