package com.codabli.dto;

import com.codabli.entity.enums.CategorieContact;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO d'entree pour l'envoi d'une demande de contact (CNT-01).
 * Le formulaire est public : nom/email sont saisis directement, meme pour un
 * utilisateur non connecte.
 */
public record ContactRequest(
        @NotNull(message = "La categorie est obligatoire")
        CategorieContact categorie,
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150)
        String nom,
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit etre valide")
        @Size(max = 255)
        String email,
        @NotBlank(message = "Le sujet est obligatoire")
        @Size(max = 255)
        String sujet,
        @NotBlank(message = "Le message est obligatoire")
        String message) {
}
