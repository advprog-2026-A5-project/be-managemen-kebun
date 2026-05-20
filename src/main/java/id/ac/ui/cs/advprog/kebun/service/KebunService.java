package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.dto.MandorKebunAssignmentResponse;
import id.ac.ui.cs.advprog.kebun.dto.KebunDetailResponse;
import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class KebunService {

    private static final String ERR_CODE_IMMUTABLE = "Kebun code is immutable and cannot be changed";
    private static final String ERR_ACTIVE_MANDOR_DELETE = "Cannot delete kebun with active mandor";
    private static final String ERR_REPLACEMENT_REQUIRED = "Replacement mandor is required before unassignment";
    private static final String ERR_REPLACEMENT_SUPIR_REQUIRED = "Replacement kebun is required before unassigning supir";
    private static final String ERR_MANDOR_ID_REQUIRED = "Mandor ID is required";
    private static final String ERR_SUPIR_ID_REQUIRED = "Supir ID is required";

    private final KebunRepository kebunRepository;
    private final OverlapValidator overlapValidator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReentrantLock writeLock = new ReentrantLock(true);

    public KebunService(KebunRepository kebunRepository,
                        OverlapValidator overlapValidator,
                        ApplicationEventPublisher applicationEventPublisher) {
        this.kebunRepository = kebunRepository;
        this.overlapValidator = overlapValidator;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Kebun create(Kebun kebun) {
        return executeWithGlobalWriteLock(() -> {
            overlapValidator.validateNoOverlap(kebun.getCoordinates());
            return kebunRepository.save(kebun);
        });
    }

    public Optional<Kebun> getByCode(String code) {
        return kebunRepository.findByCode(code);
    }

    public List<Kebun> findByName(String name) {
        return kebunRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Kebun> findByFilters(String name, String code) {
        return kebunRepository.findByNameAndCodeContainingIgnoreCase(name, code);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Kebun update(String code, Kebun updateRequest) {
        return executeWithGlobalWriteLock(() -> {
            Kebun existing = requireKebunByCode(code);

            if (!existing.getCode().equals(updateRequest.getCode())) {
                throw new IllegalArgumentException(ERR_CODE_IMMUTABLE);
            }

            return kebunRepository.save(updateRequest);
        });
    }

    public void delete(String code) {
        if (kebunRepository.existsActiveMandorByKebunCode(code)) {
            throw new IllegalStateException(ERR_ACTIVE_MANDOR_DELETE);
        }
        kebunRepository.deleteByCode(code);
    }

    public void assignMandor(String kebunCode, String mandorId) {
        if (mandorId == null || mandorId.isBlank()) {
            throw new IllegalArgumentException(ERR_MANDOR_ID_REQUIRED);
        }
        requireKebunByCode(kebunCode);
        kebunRepository.unassignMandorFromAnyKebun(mandorId);
        kebunRepository.unassignAnyMandorFromKebun(kebunCode);
        kebunRepository.assignMandor(kebunCode, mandorId);
        applicationEventPublisher.publishEvent(new MandorAssignedEvent(kebunCode, mandorId));
    }

    @Transactional
    public void unassignMandor(String kebunCode, String oldMandorId, String replacementMandorId) {
        if (replacementMandorId == null || replacementMandorId.isBlank()) {
            throw new IllegalArgumentException(ERR_REPLACEMENT_REQUIRED);
        }
        requireKebunByCode(kebunCode);

        kebunRepository.unassignMandor(kebunCode, oldMandorId);
        kebunRepository.unassignMandorFromAnyKebun(replacementMandorId);
        kebunRepository.assignMandor(kebunCode, replacementMandorId);
        applicationEventPublisher.publishEvent(new MandorAssignedEvent(kebunCode, replacementMandorId));
    }

    @Transactional
    public void reassignMandorToAnotherKebun(String currentKebunCode, String mandorId, String replacementKebunCode) {
        if (replacementKebunCode == null || replacementKebunCode.isBlank()) {
            throw new IllegalArgumentException(ERR_REPLACEMENT_REQUIRED);
        }
        if (mandorId == null || mandorId.isBlank()) {
            throw new IllegalArgumentException(ERR_MANDOR_ID_REQUIRED);
        }

        requireKebunByCode(currentKebunCode);
        requireKebunByCode(replacementKebunCode);

        kebunRepository.unassignMandor(currentKebunCode, mandorId);
        kebunRepository.unassignMandorFromAnyKebun(mandorId);
        kebunRepository.unassignAnyMandorFromKebun(replacementKebunCode);
        kebunRepository.assignMandor(replacementKebunCode, mandorId);
        applicationEventPublisher.publishEvent(new MandorAssignedEvent(replacementKebunCode, mandorId));
    }

    @Transactional
    public void assignSupir(String kebunCode, String supirId) {
        if (supirId == null || supirId.isBlank()) {
            throw new IllegalArgumentException(ERR_SUPIR_ID_REQUIRED);
        }
        requireKebunByCode(kebunCode);
        kebunRepository.unassignSupirFromAnyKebun(supirId);
        kebunRepository.assignSupir(kebunCode, supirId);
    }

    @Transactional
    public void reassignSupirToAnotherKebun(String currentKebunCode, String supirId, String replacementKebunCode) {
        if (replacementKebunCode == null || replacementKebunCode.isBlank()) {
            throw new IllegalArgumentException(ERR_REPLACEMENT_SUPIR_REQUIRED);
        }
        if (supirId == null || supirId.isBlank()) {
            throw new IllegalArgumentException(ERR_SUPIR_ID_REQUIRED);
        }

        requireKebunByCode(currentKebunCode);
        requireKebunByCode(replacementKebunCode);

        kebunRepository.unassignSupir(currentKebunCode, supirId);
        kebunRepository.unassignSupirFromAnyKebun(supirId);
        kebunRepository.assignSupir(replacementKebunCode, supirId);
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

    private Kebun requireKebunByCode(String code) {
        return kebunRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Kebun not found with code: " + code));
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
