package com.codabli.controller;

import com.codabli.dto.ParcoursConteResponse;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.service.ParcoursConteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration pour ParcoursConteController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ParcoursConteControllerTest {

    private static final UUID CONTE_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParcoursConteService parcoursConteService;

    @Test
    void lire_sansAuthentification_shouldBeRejected() throws Exception {
        // @PreAuthorize leve une AccessDeniedException, traduite en 403 par GlobalExceptionHandler
        mockMvc.perform(get("/api/contes-danses/{id}/parcours", CONTE_ID))
                .andExpect(status().isForbidden());

        verify(parcoursConteService, never()).lire(any(), any());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void lire_shouldReturnParcours() throws Exception {
        when(parcoursConteService.lire(eq(CONTE_ID), any())).thenReturn(parcours());

        mockMvc.perform(get("/api/contes-danses/{id}/parcours", CONTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.region").value("Bretagne"))
                .andExpect(jsonPath("$.univers.places[0].name").value("La forêt"))
                .andExpect(jsonPath("$.trame").isEmpty());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void enregistrerContexte_shouldReturn200() throws Exception {
        when(parcoursConteService.enregistrerContexte(eq(CONTE_ID), any(), any())).thenReturn(parcours());

        mockMvc.perform(put("/api/contes-danses/{id}/parcours/contexte", CONTE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"region": "Bretagne", "ville": "Brest", "espaceRepresentation": "gymnase",
                                 "ingredientsSecrets": ["dragons"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.espaceRepresentation").value("gymnase"));
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void enregistrerContexte_sansRegion_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/contes-danses/{id}/parcours/contexte", CONTE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"region": " ", "ingredientsSecrets": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.region").exists());

        verify(parcoursConteService, never()).enregistrerContexte(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void enregistrerEtape_shouldForwardJsonContent() throws Exception {
        when(parcoursConteService.enregistrerEtape(eq(CONTE_ID), eq("univers"), any(), any()))
                .thenReturn(parcours());

        mockMvc.perform(put("/api/contes-danses/{id}/parcours/univers", CONTE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"places": [{"id": "p1", "name": "La forêt"}]}
                                """))
                .andExpect(status().isOk());

        verify(parcoursConteService).enregistrerEtape(eq(CONTE_ID), eq("univers"),
                eq(Map.of("places", List.of(Map.of("id", "p1", "name", "La forêt")))), any());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void enregistrerEtape_etapeInconnue_shouldReturn400WithErrorCode() throws Exception {
        when(parcoursConteService.enregistrerEtape(eq(CONTE_ID), eq("inconnue"), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.ETAPE_PARCOURS_INCONNUE, "Etape inconnue"));

        mockMvc.perform(put("/api/contes-danses/{id}/parcours/inconnue", CONTE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error-code").value("error.parcours.etape-inconnue"));
    }

    private ParcoursConteResponse parcours() {
        return new ParcoursConteResponse(CONTE_ID, "Bretagne", "Brest", EspaceRepresentation.gymnase,
                List.of("dragons"), null, null, null,
                Map.of("places", List.of(Map.of("id", "p1", "name", "La forêt"))),
                null, null, null, null);
    }
}
