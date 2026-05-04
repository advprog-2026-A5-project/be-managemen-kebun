package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KebunServiceTest {

    @Mock
    private KebunRepository kebunRepository;

    @Mock
    private OverlapValidator overlapValidator;

    @InjectMocks
    private KebunService kebunService;

    @Test
    void createShouldValidateOverlapAndPersistKebun() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );

        Kebun request = Kebun.builder()
                .name("Kebun Sawit Alpha")
                .code("KBNA01")
                .luas(100.0)
                .coordinates(points)
                .build();

        when(kebunRepository.save(any(Kebun.class))).thenReturn(request);

        Kebun created = kebunService.create(request);

        verify(overlapValidator, times(1)).validateNoOverlap(points);
        verify(kebunRepository, times(1)).save(request);
        assertEquals("KBNA01", created.getCode());
    }

    @Test
    void getByCodeShouldReturnKebunWhenFound() {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit Beta")
                .code("KBNB02")
                .luas(120.0)
                .build();

        when(kebunRepository.findByCode("KBNB02")).thenReturn(Optional.of(kebun));

        Optional<Kebun> result = kebunService.getByCode("KBNB02");

        assertTrue(result.isPresent());
        assertEquals("KBNB02", result.get().getCode());
        verify(kebunRepository, times(1)).findByCode("KBNB02");
    }

    @Test
    void findByNameShouldReturnFilteredKebunList() {
        Kebun kebun1 = Kebun.builder().name("Kebun Sawit A").code("KBNA01").luas(100.0).build();
        Kebun kebun2 = Kebun.builder().name("Kebun Sawit B").code("KBNB02").luas(200.0).build();

        when(kebunRepository.findByNameContainingIgnoreCase("Sawit")).thenReturn(List.of(kebun1, kebun2));

        List<Kebun> results = kebunService.findByName("Sawit");

        assertEquals(2, results.size());
        verify(kebunRepository, times(1)).findByNameContainingIgnoreCase("Sawit");
    }
}
