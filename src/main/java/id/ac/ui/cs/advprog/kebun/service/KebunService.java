package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KebunService {

    private final KebunRepository kebunRepository;
    private final OverlapValidator overlapValidator;

    public KebunService(KebunRepository kebunRepository, OverlapValidator overlapValidator) {
        this.kebunRepository = kebunRepository;
        this.overlapValidator = overlapValidator;
    }

    public Kebun create(Kebun kebun) {
        overlapValidator.validateNoOverlap(kebun.getCoordinates());
        return kebunRepository.save(kebun);
    }

    public Optional<Kebun> getByCode(String code) {
        return kebunRepository.findByCode(code);
    }

    public List<Kebun> findByName(String name) {
        return kebunRepository.findByNameContainingIgnoreCase(name);
    }

    public Kebun update(String code, Kebun updateRequest) {
        Kebun existing = kebunRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Kebun not found with code: " + code));

        if (!existing.getCode().equals(updateRequest.getCode())) {
            throw new IllegalArgumentException("Kebun code is immutable and cannot be changed");
        }

        return kebunRepository.save(updateRequest);
    }

    public void delete(String code) {
        if (kebunRepository.existsActiveMandorByKebunCode(code)) {
            throw new IllegalStateException("Cannot delete kebun with active mandor");
        }
        kebunRepository.deleteByCode(code);
    }
}
