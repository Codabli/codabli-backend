package com.codabli.dto;

import com.codabli.entity.enums.RoleUtilisateur;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UtilisateurResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        RoleUtilisateur role,
        LocalDate dateNaissance,
        String languePreferee,
        String statut,
        UUID ecoleId,
        String ecoleNom,
        OffsetDateTime dateCreation) {
}
