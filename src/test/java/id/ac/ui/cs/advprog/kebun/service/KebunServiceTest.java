package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KebunServiceTest {

    @Mock
    private KebunRepository kebunRepository;

    @Mock
    private OverlapValidator overlapValidator;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KebunService kebunService;

    @BeforeEach
    void setUp() {
        kebunService = new KebunService(kebunRepository, overlapValidator, kafkaTemplate, "mandor-assigned");
    }

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

        verify(kebunRepository, times(1)).acquireGlobalWriteLock();
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

    @Test
    void updateShouldThrowWhenCodeIsChanged() {
        Kebun existing = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(100.0)
                .build();

        Kebun updateRequest = Kebun.builder()
                .name("Kebun Sawit A Updated")
                .code("DIFFERENT")
                .luas(150.0)
                .build();

        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> kebunService.update("KBNA01", updateRequest));
    }

    @Test
    void updateShouldPersistWhenCodeRemainsUnchanged() {
        Kebun existing = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(100.0)
                .build();

        Kebun updateRequest = Kebun.builder()
                .name("Kebun Sawit A Updated")
                .code("KBNA01")
                .luas(150.0)
                .build();

        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));
        when(kebunRepository.save(updateRequest)).thenReturn(updateRequest);

        Kebun updated = kebunService.update("KBNA01", updateRequest);

        verify(kebunRepository, times(1)).acquireGlobalWriteLock();
        assertEquals("KBNA01", updated.getCode());
        assertEquals("Kebun Sawit A Updated", updated.getName());
        verify(kebunRepository, times(1)).save(updateRequest);
    }

    @Test
    void deleteShouldThrowWhenMandorIsStillActive() {
        when(kebunRepository.existsActiveMandorByKebunCode("KBNA01")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> kebunService.delete("KBNA01"));
        verify(kebunRepository, never()).deleteByCode("KBNA01");
    }

    @Test
    void deleteShouldProceedWhenNoActiveMandor() {
        when(kebunRepository.existsActiveMandorByKebunCode("KBNA01")).thenReturn(false);

        kebunService.delete("KBNA01");

        verify(kebunRepository, times(1)).deleteByCode("KBNA01");
    }

    @Test
    void assignMandorShouldPersistAssignmentWhenKebunExists() {
        Kebun existing = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(100.0)
                .build();

        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        kebunService.assignMandor("KBNA01", "mandor-123");

        verify(kebunRepository, times(1)).assignMandor("KBNA01", "mandor-123");
    }

    @Test
    void assignMandorShouldThrowWhenKebunNotFound() {
        when(kebunRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> kebunService.assignMandor("UNKNOWN", "mandor-123"));

        verify(kebunRepository, never()).assignMandor(any(), any());
    }

    @Test
    void unassignMandorShouldThrowWhenNoReplacementProvided() {
        assertThrows(IllegalArgumentException.class,
                () -> kebunService.unassignMandor("KBNA01", "mandor-123", null));

        verify(kebunRepository, never()).unassignMandor(any(), any());
    }

    @Test
    void unassignMandorShouldPersistWhenReplacementProvided() {
        kebunService.unassignMandor("KBNA01", "mandor-123", "mandor-456");

        verify(kebunRepository, times(1)).unassignMandor("KBNA01", "mandor-123");
        verify(kebunRepository, times(1)).assignMandor("KBNA01", "mandor-456");
    }

    @Test
    void createShouldBeSerializedToPreventConcurrentOverlapRace() throws Exception {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );

        Kebun request = Kebun.builder()
                .name("Concurrent Kebun")
                .code("KBNC99")
                .luas(88.0)
                .coordinates(points)
                .build();

        java.util.concurrent.CountDownLatch firstEnteredSave = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch releaseFirstSave = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger inSave = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger maxConcurrentInSave = new java.util.concurrent.atomic.AtomicInteger(0);

        when(kebunRepository.save(any(Kebun.class))).thenAnswer(invocation -> {
            int now = inSave.incrementAndGet();
            maxConcurrentInSave.updateAndGet(prev -> Math.max(prev, now));
            firstEnteredSave.countDown();
            releaseFirstSave.await(2, java.util.concurrent.TimeUnit.SECONDS);
            inSave.decrementAndGet();
            return invocation.getArgument(0);
        });

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Future<Kebun> f1 = executor.submit(() -> kebunService.create(request));

        firstEnteredSave.await(2, java.util.concurrent.TimeUnit.SECONDS);

        java.util.concurrent.Future<Kebun> f2 = executor.submit(() -> kebunService.create(request));
        Thread.sleep(150);

        releaseFirstSave.countDown();

        f1.get(2, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(2, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdownNow();

        org.junit.jupiter.api.Assertions.assertEquals(1, maxConcurrentInSave.get());
    }

    @Test
    void updateShouldBeSerializedToPreventConcurrentWriteRace() throws Exception {
        Kebun existing = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(100.0)
                .build();

        Kebun updateRequest = Kebun.builder()
                .name("Kebun Sawit A Updated")
                .code("KBNA01")
                .luas(150.0)
                .build();

        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        java.util.concurrent.CountDownLatch firstEnteredSave = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch releaseFirstSave = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger inSave = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger maxConcurrentInSave = new java.util.concurrent.atomic.AtomicInteger(0);

        when(kebunRepository.save(any(Kebun.class))).thenAnswer(invocation -> {
            int now = inSave.incrementAndGet();
            maxConcurrentInSave.updateAndGet(prev -> Math.max(prev, now));
            firstEnteredSave.countDown();
            releaseFirstSave.await(2, java.util.concurrent.TimeUnit.SECONDS);
            inSave.decrementAndGet();
            return invocation.getArgument(0);
        });

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Future<Kebun> f1 = executor.submit(() -> kebunService.update("KBNA01", updateRequest));

        firstEnteredSave.await(2, java.util.concurrent.TimeUnit.SECONDS);

        java.util.concurrent.Future<Kebun> f2 = executor.submit(() -> kebunService.update("KBNA01", updateRequest));
        Thread.sleep(150);

        releaseFirstSave.countDown();

        f1.get(2, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(2, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdownNow();

        org.junit.jupiter.api.Assertions.assertEquals(1, maxConcurrentInSave.get());
    }
}

