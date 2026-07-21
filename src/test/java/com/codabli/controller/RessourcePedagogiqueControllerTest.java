package com.codabli.controller;

import com.codabli.dto.RessourcePedagogiqueRequest;
import com.codabli.dto.RessourcePedagogiqueResponse;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.service.RessourcePedagogiqueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RessourcePedagogiqueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
        RessourcePedagogiqueResponse res = RessourcePedagogiqueResponse.builder()
                .id(UUID.randomUUID())
                .titre("Titre Enseignant")
                .type(TypeRessource.fiche)
                .actif(true)
                .build();

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
        RessourcePedagogiqueRequest request = RessourcePedagogiqueRequest.builder()
                .titre("Guide secret")
                .type(TypeRessource.guide)
                .build();

        mockMvc.perform(post("/api/ressources-pedagogiques")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "comite_lecture")
    void creer_asComiteLecture_shouldReturn201() throws Exception {
        RessourcePedagogiqueRequest request = RessourcePedagogiqueRequest.builder()
                .titre("Guide Comité")
                .type(TypeRessource.guide)
                .actif(true)
                .build();

        RessourcePedagogiqueResponse response = RessourcePedagogiqueResponse.builder()
                .id(UUID.randomUUID())
                .titre("Guide Comité")
                .type(TypeRessource.guide)
                .actif(true)
                .dateAjout(OffsetDateTime.now())
                .build();

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
        RessourcePedagogiqueRequest request = RessourcePedagogiqueRequest.builder()
                .titre("Guide Modifie")
                .type(TypeRessource.guide)
                .actif(true)
                .build();

        RessourcePedagogiqueResponse response = RessourcePedagogiqueResponse.builder()
                .id(id)
                .titre("Guide Modifie")
                .type(TypeRessource.guide)
                .actif(true)
                .build();

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
}
