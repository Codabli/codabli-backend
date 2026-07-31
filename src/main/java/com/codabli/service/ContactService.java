package com.codabli.service;

import com.codabli.dto.ContactRequest;
import com.codabli.dto.ContactResponse;
import com.codabli.entity.DemandeContact;
import com.codabli.repository.DemandeContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service metier pour le formulaire de contact (CDC 8.21).
 *
 * Securite :
 * - Envoi (POST) : public, aucune authentification requise
 * - Consultation (GET) et traitement : reserves a l'administration via
 * @PreAuthorize dans le controller
 */
@Service
@Transactional
public class ContactService {

    private final DemandeContactRepository demandeContactRepository;

    public ContactService(DemandeContactRepository demandeContactRepository) {
        this.demandeContactRepository = demandeContactRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // ENVOYER — public (CNT-01)
    // ────────────────────────────────────────────────────────────────

    public ContactResponse envoyer(ContactRequest request) {
        DemandeContact demande = DemandeContact.builder()
                .categorie(request.getCategorie())
                .nom(request.getNom())
                .email(request.getEmail())
                .sujet(request.getSujet())
                .message(request.getMessage())
                .build();

        DemandeContact saved = demandeContactRepository.save(demande);
        return toResponse(saved, true);
    }

    // ────────────────────────────────────────────────────────────────
    // LISTER — admin/super_admin uniquement
    // ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ContactResponse> lister(Pageable pageable) {
        return demandeContactRepository.findAllByOrderByDateCreationDesc(pageable)
                .map(demande -> toResponse(demande, false));
    }

    // ────────────────────────────────────────────────────────────────
    // MARQUER TRAITEE — admin/super_admin uniquement
    // ────────────────────────────────────────────────────────────────

    public ContactResponse marquerTraitee(UUID id) {
        DemandeContact demande = demandeContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande de contact non trouvee avec l'ID: " + id));
        demande.setTraite(true);
        DemandeContact saved = demandeContactRepository.save(demande);
        return toResponse(saved, false);
    }

    // ────────────────────────────────────────────────────────────────
    // METHODES UTILITAIRES
    // ────────────────────────────────────────────────────────────────

    private ContactResponse toResponse(DemandeContact demande, boolean avecAccuseReception) {
        return ContactResponse.builder()
                .id(demande.getId())
                .categorie(demande.getCategorie())
                .nom(demande.getNom())
                .email(demande.getEmail())
                .sujet(demande.getSujet())
                .message(demande.getMessage())
                .traite(demande.isTraite())
                .dateCreation(demande.getDateCreation())
                .accuseReception(avecAccuseReception
                        ? "Votre demande a bien ete recue. Notre equipe vous repondra dans les meilleurs delais."
                        : null)
                .build();
    }
}
