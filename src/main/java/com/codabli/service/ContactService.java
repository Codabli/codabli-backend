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
 * </p>
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
                .categorie(request.categorie())
                .nom(request.nom())
                .email(request.email())
                .sujet(request.sujet())
                .message(request.message())
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
        return new ContactResponse(
                demande.getId(),
                demande.getCategorie(),
                demande.getNom(),
                demande.getEmail(),
                demande.getSujet(),
                demande.getMessage(),
                demande.isTraite(),
                demande.getDateCreation(),
                avecAccuseReception ? "Votre demande a bien ete recue. Notre equipe vous repondra dans les meilleurs delais." : null);
    }
}
