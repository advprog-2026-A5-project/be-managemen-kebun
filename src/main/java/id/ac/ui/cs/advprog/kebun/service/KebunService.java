package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.springframework.stereotype.Service;

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
}
