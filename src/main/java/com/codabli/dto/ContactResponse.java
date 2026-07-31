package com.codabli.dto;

import com.codabli.entity.enums.CategorieContact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de sortie pour une demande de contact.
 * Le champ "accuseReception" porte le message de confirmation d'envoi
 * (CNT-02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {

    private UUID id;

    private CategorieContact categorie;

    private String nom;

    private String email;

    private String sujet;

    private String message;

    private boolean traite;

    private OffsetDateTime dateCreation;

    private String accuseReception;
}
