package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.dto.SupirKebunAssignmentResponse;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/supirs")
public class InternalSupirKebunController {

    private final KebunService kebunService;

    public InternalSupirKebunController(KebunService kebunService) {
        this.kebunService = kebunService;
    }

    @GetMapping("/{supirId}/kebun")
    public ResponseEntity<SupirKebunAssignmentResponse> getSupirKebunAssignment(@PathVariable Long supirId) {
        return ResponseEntity.ok(kebunService.getSupirKebunAssignment(supirId));
    }
}
