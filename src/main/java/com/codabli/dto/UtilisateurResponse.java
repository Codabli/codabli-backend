package com.codabli.dto;

import com.codabli.entity.enums.RoleUtilisateur;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponse {

    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private RoleUtilisateur role;
    private LocalDate dateNaissance;
    private String languePreferee;
    private String statut;
    private UUID ecoleId;
    private String ecoleNom;
    private OffsetDateTime dateCreation;
}
