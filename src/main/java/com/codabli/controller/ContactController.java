package com.codabli.controller;

import com.codabli.dto.ContactRequest;
import com.codabli.dto.ContactResponse;
import com.codabli.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller pour le formulaire de contact (CDC 8.21).
 *
 * Securite :
 * - POST /api/contact : public, aucune authentification requise
 * - GET / PATCH sous /api/admin/contact : reserves a l'administration
 * (couvert par le matcher /api/admin/** dans SecurityConfig)
 */
@RestController
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * POST /api/contact
     * Envoie une demande de contact. Public. Retourne un accuse de reception
     * (CNT-02).
     */
    @PostMapping("/api/contact")
    public ResponseEntity<ContactResponse> envoyer(@Valid @RequestBody ContactRequest request) {
        ContactResponse response = contactService.envoyer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/admin/contact
     * Liste paginee des demandes de contact. Reserve a l'administration.
     */
    @GetMapping("/api/admin/contact")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<Page<ContactResponse>> lister(@PageableDefault(size = 20) Pageable pageable) {
        Page<ContactResponse> demandes = contactService.lister(pageable);
        return ResponseEntity.ok(demandes);
    }

    /**
     * PATCH /api/admin/contact/{id}/traiter
     * Marque une demande comme traitee. Reserve a l'administration.
     */
    @PatchMapping("/api/admin/contact/{id}/traiter")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<ContactResponse> marquerTraitee(@PathVariable UUID id) {
        ContactResponse response = contactService.marquerTraitee(id);
        return ResponseEntity.ok(response);
    }
}
