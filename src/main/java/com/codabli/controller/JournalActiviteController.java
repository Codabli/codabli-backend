package com.codabli.controller;

import com.codabli.dto.JournalActiviteResponse;
import com.codabli.service.JournalActiviteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller pour le journal d'activite (CDC 8.23, SUP-03).
 * Reserve au super-administrateur.
 */
@RestController
@RequestMapping("/api/admin/journal-activite")
public class JournalActiviteController {

    private final JournalActiviteService journalActiviteService;

    public JournalActiviteController(JournalActiviteService journalActiviteService) {
        this.journalActiviteService = journalActiviteService;
    }

    @GetMapping
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<Page<JournalActiviteResponse>> lister(@PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(journalActiviteService.lister(pageable));
    }
}
