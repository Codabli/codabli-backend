package com.codabli.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUtilisateurRequest(
        @Size(max = 150)
        String nom,
        @Size(max = 150)
        String prenom,
        LocalDate dateNaissance,
        @Size(max = 10)
        String languePreferee) {
}
