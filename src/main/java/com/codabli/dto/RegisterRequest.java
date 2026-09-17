package com.codabli.dto;

import com.codabli.entity.enums.RoleUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150)
        String nom,
        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 150)
        String prenom,
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être valide")
        @Size(max = 255)
        String email,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,
        @NotNull(message = "Le rôle est obligatoire")
        RoleUtilisateur role,
        LocalDate dateNaissance,
        UUID ecoleId,
        String languePreferee) {
}
