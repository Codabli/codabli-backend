package com.codabli.controller;

import com.codabli.dto.GalerieItemResponse;
import com.codabli.dto.GalerieMiseEnAvantRequest;
import com.codabli.dto.GalerieMiseEnAvantResponse;
import com.codabli.entity.enums.TypeCarte;
import com.codabli.service.GalerieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration MockMvc pour GalerieController et GalerieAdminController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GalerieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GalerieService galerieService;

    // ─────────────────────────────────────────────
    // Tests GalerieController (public)
    // ─────────────────────────────────────────────

    @Test
    void lister_withoutAuth_shouldReturn200() throws Exception {
        GalerieItemResponse item = GalerieItemResponse.builder()
                .id(UUID.randomUUID())
                .type(TypeCarte.personnage)
                .imageUrl("https://example.com/img.jpg")
                .texteAssocie("Test")
                .createurPrenom("Amine")
                .miseEnAvant(false)
                .build();

        Page<GalerieItemResponse> page = new PageImpl<>(List.of(item));
        when(galerieService.listerGalerie(any())).thenReturn(page);

        mockMvc.perform(get("/api/galerie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].texteAssocie").value("Test"));
    }

    @Test
    void getById_withoutAuth_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        GalerieItemResponse response = GalerieItemResponse.builder()
                .id(id)
                .type(TypeCarte.personnage)
                .texteAssocie("Le petit prince")
                .createurPrenom("Amine")
                .miseEnAvant(true)
                .build();

        when(galerieService.getCarteGalerie(id)).thenReturn(response);

        mockMvc.perform(get("/api/galerie/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texteAssocie").value("Le petit prince"));
    }

    // ─────────────────────────────────────────────
    // Tests GalerieAdminController (admin)
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "admin")
    void postMiseEnAvant_asAdmin_shouldReturn201() throws Exception {
        UUID carteId = UUID.randomUUID();
        GalerieMiseEnAvantRequest request = GalerieMiseEnAvantRequest.builder()
                .carteAConteId(carteId)
                .ordreAffichage(1)
                .build();

        GalerieMiseEnAvantResponse response = GalerieMiseEnAvantResponse.builder()
                .id(UUID.randomUUID())
                .carteAConteId(carteId)
                .titreCarte("Le petit prince")
                .dateDebut(OffsetDateTime.now())
                .ordreAffichage(1)
                .actif(true)
                .build();

        when(galerieService.ajouterMiseEnAvant(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/galerie/mise-en-avant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titreCarte").value("Le petit prince"));
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void postMiseEnAvant_asEnseignant_shouldReturn403() throws Exception {
        GalerieMiseEnAvantRequest request = GalerieMiseEnAvantRequest.builder()
                .carteAConteId(UUID.randomUUID())
                .ordreAffichage(1)
                .build();

        mockMvc.perform(post("/api/admin/galerie/mise-en-avant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "admin")
    void deleteMiseEnAvant_asAdmin_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(galerieService).supprimerMiseEnAvant(id);

        mockMvc.perform(delete("/api/admin/galerie/mise-en-avant/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMiseEnAvant_withoutAuth_shouldReturn401() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/galerie/mise-en-avant/" + id))
                .andExpect(status().isUnauthorized());
    }
}
