package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.dto.KebunDetailResponse;
import id.ac.ui.cs.advprog.kebun.dto.MandorAssignmentRequest;
import id.ac.ui.cs.advprog.kebun.dto.MandorReassignmentRequest;
import id.ac.ui.cs.advprog.kebun.dto.SupirAssignmentRequest;
import id.ac.ui.cs.advprog.kebun.dto.SupirReassignmentRequest;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kebun")
public class KebunController {

    private final KebunService kebunService;

    public KebunController(KebunService kebunService) {
        this.kebunService = kebunService;
    }

    @PostMapping
    public ResponseEntity<Kebun> create(@RequestBody Kebun kebun) {
        Kebun created = kebunService.create(kebun);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Kebun> getByCode(@PathVariable String code) {
        return kebunService.getByCode(code)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Kebun>> getAllByName(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "code", required = false) String code) {
        List<Kebun> kebuns = kebunService.findByFilters(name == null ? "" : name, code == null ? "" : code);
        return ResponseEntity.ok(kebuns);
    }

    @GetMapping("/{code}/detail")
    public ResponseEntity<KebunDetailResponse> getDetail(@PathVariable String code) {
        return ResponseEntity.ok(kebunService.getKebunDetailByCode(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<Kebun> update(@PathVariable String code, @RequestBody Kebun kebun) {
        Kebun updated = kebunService.update(code, kebun);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        kebunService.delete(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/mandor/assign")
    public ResponseEntity<Map<String, String>> assignMandor(
            @PathVariable String code,
            @RequestBody MandorAssignmentRequest request) {
        kebunService.assignMandor(code, request.mandorId());
        return ResponseEntity.ok(Map.of("message", "Mandor assigned"));
    }

    @PostMapping("/{code}/mandor/reassign")
    public ResponseEntity<Map<String, String>> reassignMandor(
            @PathVariable String code,
            @RequestBody MandorReassignmentRequest request) {
        kebunService.reassignMandorToAnotherKebun(code, request.mandorId(), request.replacementKebunCode());
        return ResponseEntity.ok(Map.of("message", "Mandor reassigned to another kebun"));
    }

    @PostMapping("/{code}/supir/assign")
    public ResponseEntity<Map<String, String>> assignSupir(
            @PathVariable String code,
            @RequestBody SupirAssignmentRequest request) {
        kebunService.assignSupir(code, request.supirId());
        return ResponseEntity.ok(Map.of("message", "Supir assigned"));
    }

    @PostMapping("/{code}/supir/reassign")
    public ResponseEntity<Map<String, String>> reassignSupir(
            @PathVariable String code,
            @RequestBody SupirReassignmentRequest request) {
        kebunService.reassignSupirToAnotherKebun(code, request.supirId(), request.replacementKebunCode());
        return ResponseEntity.ok(Map.of("message", "Supir reassigned to another kebun"));
    }
}
