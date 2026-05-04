package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
