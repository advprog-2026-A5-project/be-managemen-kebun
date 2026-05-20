package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.dto.MandorKebunAssignmentResponse;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/mandors")
public class InternalMandorKebunController {

    private final KebunService kebunService;

    public InternalMandorKebunController(KebunService kebunService) {
        this.kebunService = kebunService;
    }

    @GetMapping("/{mandorId}/kebun")
    public ResponseEntity<MandorKebunAssignmentResponse> getMandorKebunAssignment(@PathVariable Long mandorId) {
        return ResponseEntity.ok(kebunService.getMandorKebunAssignment(mandorId));
    }
}
