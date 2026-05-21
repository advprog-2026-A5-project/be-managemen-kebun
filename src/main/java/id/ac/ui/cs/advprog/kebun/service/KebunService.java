package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.dto.MandorKebunAssignmentResponse;
import id.ac.ui.cs.advprog.kebun.dto.KebunDetailResponse;
import id.ac.ui.cs.advprog.kebun.dto.SupirKebunAssignmentResponse;
import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.integration.client.PersonnelDirectory;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class KebunService {

    private static final String ERR_CODE_IMMUTABLE = "Kebun code is immutable and cannot be changed";
    private static final String ERR_CODE_ALREADY_EXISTS = "Kebun with code already exists: ";
    private static final String ERR_ACTIVE_MANDOR_DELETE = "Cannot delete kebun with active mandor";
    private static final String ERR_REPLACEMENT_REQUIRED = "Replacement mandor is required before unassignment";
    private static final String ERR_REPLACEMENT_SUPIR_REQUIRED = "Replacement kebun is required before unassigning supir";
    private static final String ERR_MANDOR_ID_REQUIRED = "Mandor ID is required";
    private static final String ERR_SUPIR_ID_REQUIRED = "Supir ID is required";

    private final KebunRepository kebunRepository;
    private final OverlapValidator overlapValidator;
    private final PersonnelDirectory personnelDirectory;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReentrantLock writeLock = new ReentrantLock(true);

    public KebunService(KebunRepository kebunRepository,
                        OverlapValidator overlapValidator,
                        PersonnelDirectory personnelDirectory,
                        ApplicationEventPublisher applicationEventPublisher) {
        this.kebunRepository = kebunRepository;
        this.overlapValidator = overlapValidator;
        this.personnelDirectory = personnelDirectory;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Kebun create(Kebun kebun) {
        return executeWithGlobalWriteLock(() -> {
            if (kebunRepository.existsByCode(kebun.getCode())) {
                throw new IllegalStateException(ERR_CODE_ALREADY_EXISTS + kebun.getCode());
            }
            overlapValidator.validateNoOverlap(kebun.getCoordinates());
            return kebunRepository.create(kebun);
        });
    }

    public Optional<Kebun> getByCode(String code) {
        return kebunRepository.findByCode(code);
    }

    public List<Kebun> findByName(String name) {
        return kebunRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Kebun> findByFilters(String name, String code) {
        String safeName = name == null ? "" : name.trim();
        String safeCode = code == null ? "" : code.trim();
        return kebunRepository.findByNameAndCodeContainingIgnoreCase(safeName, safeCode);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Kebun update(String code, Kebun updateRequest) {
        return executeWithGlobalWriteLock(() -> {
            Kebun existing = requireKebunByCode(code);

            if (!existing.getCode().equals(updateRequest.getCode())) {
                throw new IllegalArgumentException(ERR_CODE_IMMUTABLE);
            }

            overlapValidator.validateNoOverlap(updateRequest.getCoordinates(), existing.getCode());
            return kebunRepository.update(updateRequest);
        });
    }

    public void delete(String code) {
        requireKebunByCode(code);
        if (kebunRepository.existsActiveMandorByKebunCode(code)) {
            throw new IllegalStateException(ERR_ACTIVE_MANDOR_DELETE);
        }
        kebunRepository.deleteByCode(code);
    }

    @Transactional
    public void assignMandor(String kebunCode, String mandorId) {
        if (mandorId == null || mandorId.isBlank()) {
            throw new IllegalArgumentException(ERR_MANDOR_ID_REQUIRED);
        }
        String canonicalMandorId = personnelDirectory.requireMandorId(mandorId);
        requireKebunByCode(kebunCode);
        kebunRepository.unassignMandorFromAnyKebun(canonicalMandorId);
        kebunRepository.unassignAnyMandorFromKebun(kebunCode);
        kebunRepository.assignMandor(kebunCode, canonicalMandorId);
        applicationEventPublisher.publishEvent(new MandorAssignedEvent(kebunCode, canonicalMandorId));
    }

    @Transactional
    public void unassignMandor(String kebunCode, String oldMandorId, String replacementMandorId) {
        if (replacementMandorId == null || replacementMandorId.isBlank()) {
            throw new IllegalArgumentException(ERR_REPLACEMENT_REQUIRED);
        }
        String canonicalReplacementMandorId = personnelDirectory.requireMandorId(replacementMandorId);
        requireKebunByCode(kebunCode);

        kebunRepository.unassignMandor(kebunCode, oldMandorId);
        kebunRepository.unassignMandorFromAnyKebun(canonicalReplacementMandorId);
        kebunRepository.assignMandor(kebunCode, canonicalReplacementMandorId);
        applicationEventPublisher.publishEvent(new MandorAssignedEvent(kebunCode, canonicalReplacementMandorId));
    }

    @Transactional
    public void reassignMandorToAnotherKebun(String currentKebunCode, String mandorId, String replacementKebunCode) {
        if (replacementKebunCode == null || replacementKebunCode.isBlank()) {
            throw new IllegalArgumentException(ERR_REPLACEMENT_REQUIRED);
        }
        if (mandorId == null || mandorId.isBlank()) {
            throw new IllegalArgumentException(ERR_MANDOR_ID_REQUIRED);
        }
        String canonicalMandorId = personnelDirectory.requireMandorId(mandorId);

        requireKebunByCode(currentKebunCode);
        requireKebunByCode(replacementKebunCode);

        kebunRepository.unassignMandor(currentKebunCode, canonicalMandorId);
        kebunRepository.unassignMandorFromAnyKebun(canonicalMandorId);
        kebunRepository.unassignAnyMandorFromKebun(replacementKebunCode);
        kebunRepository.assignMandor(replacementKebunCode, canonicalMandorId);
        applicationEventPublisher.publishEvent(new MandorAssignedEvent(replacementKebunCode, canonicalMandorId));
    }

    @Transactional
    public void assignSupir(String kebunCode, String supirId) {
        if (supirId == null || supirId.isBlank()) {
            throw new IllegalArgumentException(ERR_SUPIR_ID_REQUIRED);
        }
        String canonicalSupirId = personnelDirectory.requireSupirId(supirId);
        requireKebunByCode(kebunCode);
        kebunRepository.unassignSupirFromAnyKebun(canonicalSupirId);
        kebunRepository.assignSupir(kebunCode, canonicalSupirId);
    }

    @Transactional
    public void reassignSupirToAnotherKebun(String currentKebunCode, String supirId, String replacementKebunCode) {
        if (replacementKebunCode == null || replacementKebunCode.isBlank()) {
            throw new IllegalArgumentException(ERR_REPLACEMENT_SUPIR_REQUIRED);
        }
        if (supirId == null || supirId.isBlank()) {
            throw new IllegalArgumentException(ERR_SUPIR_ID_REQUIRED);
        }
        String canonicalSupirId = personnelDirectory.requireSupirId(supirId);

        requireKebunByCode(currentKebunCode);
        requireKebunByCode(replacementKebunCode);

        kebunRepository.unassignSupir(currentKebunCode, canonicalSupirId);
        kebunRepository.unassignSupirFromAnyKebun(canonicalSupirId);
        kebunRepository.assignSupir(replacementKebunCode, canonicalSupirId);
    }

    public KebunDetailResponse getKebunDetailByCode(String code) {
        Kebun kebun = requireKebunByCode(code);
        String mandorId = kebunRepository.findMandorIdByKebunCode(code).orElse(null);
        List<String> supirIds = kebunRepository.findSupirIdsByKebunCode(code);
        return new KebunDetailResponse(
                kebun.getCode(),
                kebun.getName(),
                kebun.getLuas(),
                kebun.getCoordinates(),
                mandorId,
                supirIds
        );
    }

    public MandorKebunAssignmentResponse getMandorKebunAssignment(Long mandorId) {
        Optional<Kebun> kebun = kebunRepository.findAssignedKebunByMandorId(String.valueOf(mandorId));

        if (kebun.isEmpty()) {
            return new MandorKebunAssignmentResponse(mandorId, null, null, null, false);
        }

        Kebun assignedKebun = kebun.get();
        return new MandorKebunAssignmentResponse(
                mandorId,
                null,
                assignedKebun.getCode(),
                assignedKebun.getName(),
                true
        );
    }

    public SupirKebunAssignmentResponse getSupirKebunAssignment(Long supirId) {
        Optional<Kebun> kebun = kebunRepository.findAssignedKebunBySupirId(String.valueOf(supirId));

        if (kebun.isEmpty()) {
            return new SupirKebunAssignmentResponse(supirId, null, null, null, false);
        }

        Kebun assignedKebun = kebun.get();
        return new SupirKebunAssignmentResponse(
                supirId,
                null,
                assignedKebun.getCode(),
                assignedKebun.getName(),
                true
        );
    }

    private Kebun requireKebunByCode(String code) {
        return kebunRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Kebun not found with code: " + code));
    }

    private <T> T executeWithWriteLock(Supplier<T> action) {
        writeLock.lock();
        try {
            return action.get();
        } finally {
            writeLock.unlock();
        }
    }

    private <T> T executeWithGlobalWriteLock(Supplier<T> action) {
        return executeWithWriteLock(() -> {
            kebunRepository.acquireGlobalWriteLock();
            return action.get();
        });
    }
}
