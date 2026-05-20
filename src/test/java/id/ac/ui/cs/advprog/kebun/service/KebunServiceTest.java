package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private ApplicationEventPublisher applicationEventPublisher;

    private KebunService kebunService;

    @BeforeEach
    void setUp() {
        kebunService = new KebunService(
                kebunRepository,
                overlapValidator,
                applicationEventPublisher
        );
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
    void findByFiltersShouldDelegateToRepository() {
        Kebun kebun = Kebun.builder().name("Kebun Sawit A").code("KBNA01").luas(100.0).build();
        when(kebunRepository.findByNameAndCodeContainingIgnoreCase("Sawit", "A01")).thenReturn(List.of(kebun));

        List<Kebun> result = kebunService.findByFilters("Sawit", "A01");

        assertEquals(1, result.size());
        verify(kebunRepository, times(1)).findByNameAndCodeContainingIgnoreCase("Sawit", "A01");
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

        verify(kebunRepository, times(1)).unassignMandorFromAnyKebun("mandor-123");
        verify(kebunRepository, times(1)).unassignAnyMandorFromKebun("KBNA01");
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
    void assignMandorShouldThrowWhenMandorIdBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> kebunService.assignMandor("KBNA01", " "));
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
        Kebun existing = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(100.0)
                .build();
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        kebunService.unassignMandor("KBNA01", "mandor-123", "mandor-456");

        verify(kebunRepository, times(1)).unassignMandor("KBNA01", "mandor-123");
        verify(kebunRepository, times(1)).unassignMandorFromAnyKebun("mandor-456");
        verify(kebunRepository, times(1)).assignMandor("KBNA01", "mandor-456");
    }

    @Test
    void reassignMandorShouldMoveMandorToReplacementKebun() {
        Kebun current = Kebun.builder().name("Kebun A").code("KB001").luas(100.0).build();
        Kebun replacement = Kebun.builder().name("Kebun B").code("KB002").luas(120.0).build();
        when(kebunRepository.findByCode("KB001")).thenReturn(Optional.of(current));
        when(kebunRepository.findByCode("KB002")).thenReturn(Optional.of(replacement));

        kebunService.reassignMandorToAnotherKebun("KB001", "3", "KB002");

        verify(kebunRepository).unassignMandor("KB001", "3");
        verify(kebunRepository).unassignMandorFromAnyKebun("3");
        verify(kebunRepository).unassignAnyMandorFromKebun("KB002");
        verify(kebunRepository).assignMandor("KB002", "3");
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent
                        && "KB002".equals(((id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent) event).getKebunCode())
                        && "3".equals(((id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent) event).getMandorId())
        ));
    }

    @Test
    void reassignMandorShouldThrowWhenReplacementMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> kebunService.reassignMandorToAnotherKebun("KB001", "3", " "));
    }

    @Test
    void assignSupirShouldPersistWhenInputsValid() {
        Kebun kebun = Kebun.builder().name("Kebun A").code("KB001").luas(100.0).build();
        when(kebunRepository.findByCode("KB001")).thenReturn(Optional.of(kebun));

        kebunService.assignSupir("KB001", "11");

        verify(kebunRepository).unassignSupirFromAnyKebun("11");
        verify(kebunRepository).assignSupir("KB001", "11");
    }

    @Test
    void assignSupirShouldThrowWhenSupirIdMissing() {
        assertThrows(IllegalArgumentException.class, () -> kebunService.assignSupir("KB001", ""));
        verify(kebunRepository, never()).assignSupir(any(), any());
    }

    @Test
    void reassignSupirShouldMoveSupirToReplacementKebun() {
        Kebun current = Kebun.builder().name("Kebun A").code("KB001").luas(100.0).build();
        Kebun replacement = Kebun.builder().name("Kebun B").code("KB002").luas(120.0).build();
        when(kebunRepository.findByCode("KB001")).thenReturn(Optional.of(current));
        when(kebunRepository.findByCode("KB002")).thenReturn(Optional.of(replacement));

        kebunService.reassignSupirToAnotherKebun("KB001", "11", "KB002");

        verify(kebunRepository).unassignSupir("KB001", "11");
        verify(kebunRepository).unassignSupirFromAnyKebun("11");
        verify(kebunRepository).assignSupir("KB002", "11");
    }

    @Test
    void reassignSupirShouldThrowWhenReplacementMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> kebunService.reassignSupirToAnotherKebun("KB001", "11", " "));
    }

    @Test
    void getKebunDetailByCodeShouldIncludeMandorAndSupirs() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );
        Kebun kebun = Kebun.builder()
                .name("Kebun A")
                .code("KB001")
                .luas(100.0)
                .coordinates(points)
                .build();
        when(kebunRepository.findByCode("KB001")).thenReturn(Optional.of(kebun));
        when(kebunRepository.findMandorIdByKebunCode("KB001")).thenReturn(Optional.of("3"));
        when(kebunRepository.findSupirIdsByKebunCode("KB001")).thenReturn(List.of("11", "12"));

        var detail = kebunService.getKebunDetailByCode("KB001");

        assertEquals("KB001", detail.code());
        assertEquals("3", detail.mandorId());
        assertEquals(List.of("11", "12"), detail.supirIds());
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

    @Test
    void createShouldUseSerializableTransactionIsolation() throws NoSuchMethodException {
        Transactional transactional = KebunService.class
                .getMethod("create", Kebun.class)
                .getAnnotation(Transactional.class);

        org.junit.jupiter.api.Assertions.assertNotNull(transactional);
        assertEquals(Isolation.SERIALIZABLE, transactional.isolation());
    }

    @Test
    void updateShouldUseSerializableTransactionIsolation() throws NoSuchMethodException {
        Transactional transactional = KebunService.class
                .getMethod("update", String.class, Kebun.class)
                .getAnnotation(Transactional.class);

        org.junit.jupiter.api.Assertions.assertNotNull(transactional);
        assertEquals(Isolation.SERIALIZABLE, transactional.isolation());
    }

    @Test
    void getMandorKebunAssignmentShouldReturnActiveAssignment() {
        Kebun kebun = Kebun.builder()
                .name("Kebun A")
                .code("KB001")
                .luas(100.0)
                .build();

        when(kebunRepository.findAssignedKebunByMandorId("3")).thenReturn(Optional.of(kebun));

        var result = kebunService.getMandorKebunAssignment(3L);

        assertEquals(3L, result.mandorId());
        assertEquals("KB001", result.kebunCode());
        assertEquals(true, result.active());
    }

    @Test
    void getMandorKebunAssignmentShouldReturnInactiveWhenMandorNotAssigned() {
        when(kebunRepository.findAssignedKebunByMandorId("99")).thenReturn(Optional.empty());

        var result = kebunService.getMandorKebunAssignment(99L);

        assertEquals(99L, result.mandorId());
        assertEquals(false, result.active());
    }
}

