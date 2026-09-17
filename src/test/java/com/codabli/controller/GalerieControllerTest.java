package com.codabli.controller;

import com.codabli.dto.GalerieItemResponse;
import com.codabli.dto.GalerieMiseEnAvantRequest;
import com.codabli.dto.GalerieMiseEnAvantResponse;
import com.codabli.entity.enums.TypeCarte;
import com.codabli.service.GalerieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
    private JsonMapper objectMapper;

    @MockitoBean
    private GalerieService galerieService;

    // ─────────────────────────────────────────────
    // Tests GalerieController (public)
    // ─────────────────────────────────────────────

    @Test
    void lister_withoutAuth_shouldReturn200() throws Exception {
        GalerieItemResponse item = new GalerieItemResponse(
                UUID.randomUUID(),
                TypeCarte.personnage,
                "https://example.com/img.jpg",
                "Test",
                null,
                "Amine",
                false);

        Page<GalerieItemResponse> page = new PageImpl<>(List.of(item));
        when(galerieService.listerGalerie(any())).thenReturn(page);

        mockMvc.perform(get("/api/galerie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].texteAssocie").value("Test"));
    }

    @Test
    void getById_withoutAuth_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        GalerieItemResponse response = new GalerieItemResponse(
                id,
                TypeCarte.personnage,
                null,
                "Le petit prince",
                null,
                "Amine",
                true);

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
        GalerieMiseEnAvantRequest request = new GalerieMiseEnAvantRequest(carteId, null, 1);

        GalerieMiseEnAvantResponse response = new GalerieMiseEnAvantResponse(
                UUID.randomUUID(),
                carteId,
                "Le petit prince",
                OffsetDateTime.now(),
                null,
                1,
                true);

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
        GalerieMiseEnAvantRequest request = new GalerieMiseEnAvantRequest(UUID.randomUUID(), null, 1);

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
