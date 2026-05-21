package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        Kebun request = kebun("Kebun Sawit Alpha", "KBNA01", 100.0, squarePoints());
        when(kebunRepository.existsByCode("KBNA01")).thenReturn(false);
        when(kebunRepository.create(request)).thenReturn(request);

        Kebun created = kebunService.create(request);

        verify(kebunRepository).acquireGlobalWriteLock();
        verify(kebunRepository).existsByCode("KBNA01");
        verify(overlapValidator).validateNoOverlap(request.getCoordinates());
        verify(kebunRepository).create(request);
        assertEquals("KBNA01", created.getCode());
    }

    @Test
    void createShouldRejectDuplicateCodeBeforePersisting() {
        Kebun request = kebun("Kebun Sawit Alpha", "KBNA01", 100.0, squarePoints());
        when(kebunRepository.existsByCode("KBNA01")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> kebunService.create(request));

        assertEquals("Kebun with code already exists: KBNA01", ex.getMessage());
        verify(kebunRepository).acquireGlobalWriteLock();
        verify(kebunRepository).existsByCode("KBNA01");
        verify(overlapValidator, never()).validateNoOverlap(any());
        verify(kebunRepository, never()).create(any(Kebun.class));
    }

    @Test
    void createShouldNotOverwriteExistingKebunWhenCodeAlreadyExists() {
        Kebun existing = kebun("Old Kebun", "KBNA01", 88.0, squarePoints());
        Kebun duplicateRequest = kebun("New Kebun", "KBNA01", 120.0, offsetSquarePoints());

        when(kebunRepository.existsByCode("KBNA01")).thenReturn(true);
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> kebunService.create(duplicateRequest));

        Optional<Kebun> stored = kebunRepository.findByCode("KBNA01");
        assertTrue(stored.isPresent());
        assertEquals("Old Kebun", stored.get().getName());
        assertEquals(88.0, stored.get().getLuas());
        assertEquals(4, stored.get().getCoordinates().size());
        assertEquals(0.0, stored.get().getCoordinates().get(0).getX());
        assertEquals(0.0, stored.get().getCoordinates().get(0).getY());
        verify(kebunRepository, never()).create(any(Kebun.class));
    }

    @Test
    void createShouldSurfaceOverlapValidationFailure() {
        Kebun request = kebun("Overlap Candidate", "KBNA01", 100.0, squarePoints());
        when(kebunRepository.existsByCode("KBNA01")).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Kebun coordinates overlap with an existing kebun"))
                .when(overlapValidator).validateNoOverlap(request.getCoordinates());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> kebunService.create(request));

        assertEquals("Kebun coordinates overlap with an existing kebun", ex.getMessage());
        verify(kebunRepository, never()).create(any(Kebun.class));
    }

    @Test
    void getByCodeShouldReturnKebunWhenFound() {
        Kebun kebun = kebun("Kebun Sawit Beta", "KBNB02", 120.0, squarePoints());
        when(kebunRepository.findByCode("KBNB02")).thenReturn(Optional.of(kebun));

        Optional<Kebun> result = kebunService.getByCode("KBNB02");

        assertTrue(result.isPresent());
        assertEquals("KBNB02", result.get().getCode());
        verify(kebunRepository).findByCode("KBNB02");
    }

    @Test
    void findByNameShouldReturnFilteredKebunList() {
        Kebun kebun1 = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        Kebun kebun2 = kebun("Kebun Sawit B", "KBNB02", 200.0, offsetSquarePoints());
        when(kebunRepository.findByNameContainingIgnoreCase("Sawit")).thenReturn(List.of(kebun1, kebun2));

        List<Kebun> results = kebunService.findByName("Sawit");

        assertEquals(2, results.size());
        verify(kebunRepository).findByNameContainingIgnoreCase("Sawit");
    }

    @Test
    void findByFiltersShouldDelegateToRepository() {
        Kebun kebun = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        when(kebunRepository.findByNameAndCodeContainingIgnoreCase("Sawit", "A01")).thenReturn(List.of(kebun));

        List<Kebun> result = kebunService.findByFilters("Sawit", "A01");

        assertEquals(1, result.size());
        verify(kebunRepository).findByNameAndCodeContainingIgnoreCase("Sawit", "A01");
    }

    @Test
    void updateShouldThrowWhenTargetKebunDoesNotExist() {
        Kebun updateRequest = kebun("Kebun Sawit A Updated", "KBNA01", 150.0, squarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> kebunService.update("KBNA01", updateRequest));

        assertEquals("Kebun not found with code: KBNA01", ex.getMessage());
        verify(kebunRepository).acquireGlobalWriteLock();
        verify(overlapValidator, never()).validateNoOverlap(any(), any());
        verify(kebunRepository, never()).update(any(Kebun.class));
    }

    @Test
    void updateShouldThrowWhenCodeIsChanged() {
        Kebun existing = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        Kebun updateRequest = kebun("Kebun Sawit A Updated", "DIFFERENT", 150.0, squarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kebunService.update("KBNA01", updateRequest));

        assertEquals("Kebun code is immutable and cannot be changed", ex.getMessage());
        verify(overlapValidator, never()).validateNoOverlap(any(), any());
        verify(kebunRepository, never()).update(any(Kebun.class));
    }

    @Test
    void updateShouldPersistWhenDataIsValidAndNonOverlapping() {
        Kebun existing = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        Kebun updateRequest = kebun("Kebun Sawit A Updated", "KBNA01", 150.0, offsetSquarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));
        when(kebunRepository.update(updateRequest)).thenReturn(updateRequest);

        Kebun updated = kebunService.update("KBNA01", updateRequest);

        verify(kebunRepository).acquireGlobalWriteLock();
        verify(overlapValidator).validateNoOverlap(updateRequest.getCoordinates(), "KBNA01");
        verify(kebunRepository).update(updateRequest);
        assertEquals("KBNA01", updated.getCode());
        assertEquals("Kebun Sawit A Updated", updated.getName());
        assertEquals(150.0, updated.getLuas());
    }

    @Test
    void updateShouldRejectOverlapWithAnotherKebun() {
        Kebun existing = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        Kebun updateRequest = kebun("Kebun Sawit A Updated", "KBNA01", 150.0, offsetSquarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Kebun coordinates overlap with an existing kebun"))
                .when(overlapValidator).validateNoOverlap(updateRequest.getCoordinates(), "KBNA01");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kebunService.update("KBNA01", updateRequest));

        assertEquals("Kebun coordinates overlap with an existing kebun", ex.getMessage());
        verify(kebunRepository, never()).update(any(Kebun.class));
    }

    @Test
    void updateShouldIgnoreSelfOverlapWhenGeometryDoesNotMeaningfullyChange() {
        Kebun existing = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        Kebun updateRequest = kebun("Kebun Sawit A Updated", "KBNA01", 110.0, squarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));
        when(kebunRepository.update(updateRequest)).thenReturn(updateRequest);

        Kebun updated = kebunService.update("KBNA01", updateRequest);

        assertEquals("Kebun Sawit A Updated", updated.getName());
        verify(overlapValidator).validateNoOverlap(updateRequest.getCoordinates(), "KBNA01");
        verify(kebunRepository).update(updateRequest);
    }

    @Test
    void updateShouldSurfaceInvalidCoordinateValidationFromDomainModel() {
        assertThrows(IllegalArgumentException.class, () -> kebun(
                "Invalid Geometry",
                "KBNA01",
                100.0,
                List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 2),
                        new Kebun.Point(0, 4),
                        new Kebun.Point(2, 0)
                )
        ));
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

        verify(kebunRepository).deleteByCode("KBNA01");
    }

    @Test
    void assignMandorShouldPersistAssignmentWhenKebunExists() {
        Kebun existing = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        kebunService.assignMandor("KBNA01", "mandor-123");

        verify(kebunRepository).unassignMandorFromAnyKebun("mandor-123");
        verify(kebunRepository).unassignAnyMandorFromKebun("KBNA01");
        verify(kebunRepository).assignMandor("KBNA01", "mandor-123");
    }

    @Test
    void assignMandorShouldThrowWhenKebunNotFound() {
        when(kebunRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
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
        Kebun existing = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        kebunService.unassignMandor("KBNA01", "mandor-123", "mandor-456");

        verify(kebunRepository).unassignMandor("KBNA01", "mandor-123");
        verify(kebunRepository).unassignMandorFromAnyKebun("mandor-456");
        verify(kebunRepository).assignMandor("KBNA01", "mandor-456");
    }

    @Test
    void reassignMandorShouldMoveMandorToReplacementKebun() {
        Kebun current = kebun("Kebun A", "KB001", 100.0, squarePoints());
        Kebun replacement = kebun("Kebun B", "KB002", 120.0, offsetSquarePoints());
        when(kebunRepository.findByCode("KB001")).thenReturn(Optional.of(current));
        when(kebunRepository.findByCode("KB002")).thenReturn(Optional.of(replacement));

        kebunService.reassignMandorToAnotherKebun("KB001", "3", "KB002");

        verify(kebunRepository).unassignMandor("KB001", "3");
        verify(kebunRepository).unassignMandorFromAnyKebun("3");
        verify(kebunRepository).unassignAnyMandorFromKebun("KB002");
        verify(kebunRepository).assignMandor("KB002", "3");
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof MandorAssignedEvent mandorEvent
                        && "KB002".equals(mandorEvent.getKebunCode())
                        && "3".equals(mandorEvent.getMandorId())
        ));
    }

    @Test
    void reassignMandorShouldThrowWhenReplacementMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> kebunService.reassignMandorToAnotherKebun("KB001", "3", " "));
    }

    @Test
    void assignSupirShouldPersistWhenInputsValid() {
        Kebun kebun = kebun("Kebun A", "KB001", 100.0, squarePoints());
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
        Kebun current = kebun("Kebun A", "KB001", 100.0, squarePoints());
        Kebun replacement = kebun("Kebun B", "KB002", 120.0, offsetSquarePoints());
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
        Kebun kebun = kebun("Kebun A", "KB001", 100.0, squarePoints());
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
        Kebun request = kebun("Concurrent Kebun", "KBNC99", 88.0, squarePoints());
        CountDownLatch firstEnteredCreate = new CountDownLatch(1);
        CountDownLatch releaseFirstCreate = new CountDownLatch(1);
        AtomicInteger inCreate = new AtomicInteger(0);
        AtomicInteger maxConcurrentInCreate = new AtomicInteger(0);

        when(kebunRepository.existsByCode("KBNC99")).thenReturn(false);
        when(kebunRepository.create(any(Kebun.class))).thenAnswer(invocation -> {
            int now = inCreate.incrementAndGet();
            maxConcurrentInCreate.updateAndGet(prev -> Math.max(prev, now));
            firstEnteredCreate.countDown();
            releaseFirstCreate.await(2, TimeUnit.SECONDS);
            inCreate.decrementAndGet();
            return invocation.getArgument(0);
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Kebun> f1 = executor.submit(() -> kebunService.create(request));

        firstEnteredCreate.await(2, TimeUnit.SECONDS);

        Future<Kebun> f2 = executor.submit(() -> kebunService.create(request));
        Thread.sleep(150);

        releaseFirstCreate.countDown();

        f1.get(2, TimeUnit.SECONDS);
        f2.get(2, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(1, maxConcurrentInCreate.get());
    }

    @Test
    void updateShouldBeSerializedToPreventConcurrentWriteRace() throws Exception {
        Kebun existing = kebun("Kebun Sawit A", "KBNA01", 100.0, squarePoints());
        Kebun updateRequest = kebun("Kebun Sawit A Updated", "KBNA01", 150.0, offsetSquarePoints());
        when(kebunRepository.findByCode("KBNA01")).thenReturn(Optional.of(existing));

        CountDownLatch firstEnteredUpdate = new CountDownLatch(1);
        CountDownLatch releaseFirstUpdate = new CountDownLatch(1);
        AtomicInteger inUpdate = new AtomicInteger(0);
        AtomicInteger maxConcurrentInUpdate = new AtomicInteger(0);

        when(kebunRepository.update(any(Kebun.class))).thenAnswer(invocation -> {
            int now = inUpdate.incrementAndGet();
            maxConcurrentInUpdate.updateAndGet(prev -> Math.max(prev, now));
            firstEnteredUpdate.countDown();
            releaseFirstUpdate.await(2, TimeUnit.SECONDS);
            inUpdate.decrementAndGet();
            return invocation.getArgument(0);
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Kebun> f1 = executor.submit(() -> kebunService.update("KBNA01", updateRequest));

        firstEnteredUpdate.await(2, TimeUnit.SECONDS);

        Future<Kebun> f2 = executor.submit(() -> kebunService.update("KBNA01", updateRequest));
        Thread.sleep(150);

        releaseFirstUpdate.countDown();

        f1.get(2, TimeUnit.SECONDS);
        f2.get(2, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(1, maxConcurrentInUpdate.get());
    }

    @Test
    void createShouldUseSerializableTransactionIsolation() throws NoSuchMethodException {
        Transactional transactional = KebunService.class
                .getMethod("create", Kebun.class)
                .getAnnotation(Transactional.class);

        assertDoesNotThrow(() -> assertEquals(Isolation.SERIALIZABLE, transactional.isolation()));
    }

    @Test
    void updateShouldUseSerializableTransactionIsolation() throws NoSuchMethodException {
        Transactional transactional = KebunService.class
                .getMethod("update", String.class, Kebun.class)
                .getAnnotation(Transactional.class);

        assertDoesNotThrow(() -> assertEquals(Isolation.SERIALIZABLE, transactional.isolation()));
    }

    @Test
    void getMandorKebunAssignmentShouldReturnActiveAssignment() {
        Kebun kebun = kebun("Kebun A", "KB001", 100.0, squarePoints());
        when(kebunRepository.findAssignedKebunByMandorId("3")).thenReturn(Optional.of(kebun));

        var result = kebunService.getMandorKebunAssignment(3L);

        assertEquals(3L, result.mandorId());
        assertEquals("KB001", result.kebunCode());
        assertTrue(result.active());
    }

    @Test
    void getMandorKebunAssignmentShouldReturnInactiveWhenMandorNotAssigned() {
        when(kebunRepository.findAssignedKebunByMandorId("99")).thenReturn(Optional.empty());

        var result = kebunService.getMandorKebunAssignment(99L);

        assertEquals(99L, result.mandorId());
        assertEquals(false, result.active());
    }

    private Kebun kebun(String name, String code, double luas, List<Kebun.Point> coordinates) {
        return Kebun.builder()
                .name(name)
                .code(code)
                .luas(luas)
                .coordinates(coordinates)
                .build();
    }

    private List<Kebun.Point> squarePoints() {
        return List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );
    }

    private List<Kebun.Point> offsetSquarePoints() {
        return List.of(
                new Kebun.Point(3, 0),
                new Kebun.Point(3, 2),
                new Kebun.Point(5, 2),
                new Kebun.Point(5, 0)
        );
    }
}
