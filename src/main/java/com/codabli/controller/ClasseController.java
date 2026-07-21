package com.codabli.controller;

import com.codabli.dto.ClasseRequest;
import com.codabli.dto.ClasseResponse;
import com.codabli.service.ClasseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints pour gerer les classes.
 */
@RestController
@RequestMapping("/api/classes")
public class ClasseController {

    private final ClasseService classeService;

    public ClasseController(ClasseService classeService) {
        this.classeService = classeService;
    }

    @PostMapping
    public ResponseEntity<ClasseResponse> creer(@Valid @RequestBody ClasseRequest request) {
        ClasseResponse response = classeService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClasseResponse>> lister() {
        return ResponseEntity.ok(classeService.listerTout());
    }
}
