package com.codabli.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUtilisateurRequest {

    @Size(max = 150)
    private String nom;

    @Size(max = 150)
    private String prenom;

    private LocalDate dateNaissance;

    @Size(max = 10)
    private String languePreferee;
}
