package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class KebunService {

    private static final String ERR_CODE_IMMUTABLE = "Kebun code is immutable and cannot be changed";
    private static final String ERR_ACTIVE_MANDOR_DELETE = "Cannot delete kebun with active mandor";
    private static final String ERR_REPLACEMENT_REQUIRED = "Replacement mandor is required before unassignment";

    private final KebunRepository kebunRepository;
    private final OverlapValidator overlapValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String mandorAssignedTopic;
    private final ReentrantLock writeLock = new ReentrantLock(true);

    public KebunService(KebunRepository kebunRepository,
                        OverlapValidator overlapValidator,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        @Value("${app.kafka.topic.mandor-assigned}") String mandorAssignedTopic) {
        this.kebunRepository = kebunRepository;
        this.overlapValidator = overlapValidator;
        this.kafkaTemplate = kafkaTemplate;
        this.mandorAssignedTopic = mandorAssignedTopic;
    }

    public Kebun create(Kebun kebun) {
        return executeWithWriteLock(() -> {
            kebunRepository.acquireGlobalWriteLock();
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

    public Kebun update(String code, Kebun updateRequest) {
        return executeWithWriteLock(() -> {
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
        requireKebunByCode(kebunCode);
        kebunRepository.assignMandor(kebunCode, mandorId);
        kafkaTemplate.send(mandorAssignedTopic, kebunCode, new MandorAssignedEvent(kebunCode, mandorId));
    }

    public void unassignMandor(String kebunCode, String oldMandorId, String replacementMandorId) {
        if (replacementMandorId == null || replacementMandorId.isBlank()) {
            throw new IllegalArgumentException(ERR_REPLACEMENT_REQUIRED);
        }

        kebunRepository.unassignMandor(kebunCode, oldMandorId);
        kebunRepository.assignMandor(kebunCode, replacementMandorId);
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
}
