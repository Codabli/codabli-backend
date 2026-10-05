package com.codabli.controller;

import com.codabli.dto.RessourcePedagogiqueRequest;
import com.codabli.dto.RessourcePedagogiqueResponse;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.service.RessourcePedagogiqueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RessourcePedagogiqueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private RessourcePedagogiqueService ressourcePedagogiqueService;

    // ────────────────────────────────────────────────────────────────
    // GET (Liste & Détail)
    // ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "eleve")
    void lister_asEleve_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/ressources-pedagogiques"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "parent")
    void getById_asParent_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/ressources-pedagogiques/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void lister_asEnseignant_shouldReturn200() throws Exception {
        RessourcePedagogiqueResponse res = ressourcePedagogiqueResponse(UUID.randomUUID(), "Titre Enseignant", TypeRessource.fiche, null);

        when(ressourcePedagogiqueService.lister(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(res)));

        mockMvc.perform(get("/api/ressources-pedagogiques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titre").value("Titre Enseignant"));
    }

    // ────────────────────────────────────────────────────────────────
    // POST / PUT (Création & Modification)
    // ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "enseignant")
    void creer_asEnseignant_shouldReturn403() throws Exception {
        RessourcePedagogiqueRequest request = ressourcePedagogiqueRequest("Guide secret", null);

        mockMvc.perform(post("/api/ressources-pedagogiques")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "comite_lecture")
    void creer_asComiteLecture_shouldReturn201() throws Exception {
        RessourcePedagogiqueRequest request = ressourcePedagogiqueRequest("Guide Comité", true);

        RessourcePedagogiqueResponse response = ressourcePedagogiqueResponse(UUID.randomUUID(), "Guide Comité", TypeRessource.guide, OffsetDateTime.now());

        when(ressourcePedagogiqueService.creer(any())).thenReturn(response);

        mockMvc.perform(post("/api/ressources-pedagogiques")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Guide Comité"));
    }

    @Test
    @WithMockUser(roles = "comite_lecture")
    void modifier_asComiteLecture_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        RessourcePedagogiqueRequest request = ressourcePedagogiqueRequest("Guide Modifie", true);

        RessourcePedagogiqueResponse response = ressourcePedagogiqueResponse(id, "Guide Modifie", TypeRessource.guide, null);

        when(ressourcePedagogiqueService.modifier(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/api/ressources-pedagogiques/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Guide Modifie"));
    }

    // ────────────────────────────────────────────────────────────────
    // DELETE (Suppression)
    // ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "comite_lecture")
    void supprimer_asComiteLecture_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();

        // En Spring Security, comite_lecture est bloqué au niveau du
        // @PreAuthorize("hasRole('admin')") du controller
        mockMvc.perform(delete("/api/ressources-pedagogiques/" + id))
                .andExpect(status().isForbidden());

        verify(ressourcePedagogiqueService, never()).supprimer(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void supprimer_asAdmin_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        doNothing().when(ressourcePedagogiqueService).supprimer(eq(id), any());

        mockMvc.perform(delete("/api/ressources-pedagogiques/" + id))
                .andExpect(status().isNoContent());

        verify(ressourcePedagogiqueService).supprimer(eq(id), any());
    }

    private RessourcePedagogiqueResponse ressourcePedagogiqueResponse(UUID id, String titre, TypeRessource type, OffsetDateTime dateAjout) {
        return new RessourcePedagogiqueResponse(id, titre, null, type, null, null, null, dateAjout, true);
    }

    private RessourcePedagogiqueRequest ressourcePedagogiqueRequest(String titre, Boolean actif) {
        return new RessourcePedagogiqueRequest(titre, null, TypeRessource.guide, null, null, null, actif);
    }
}
