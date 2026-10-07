package com.codabli.controller;

import com.codabli.dto.ProjetConteRequest;
import com.codabli.dto.ProjetConteResponse;
import com.codabli.entity.enums.EspaceRepresentation;
import com.codabli.service.ProjetConteService;
import com.codabli.service.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration pour ProjetConteController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjetConteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private ProjetConteService projetConteService;

    @Test
    void creer_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/projets-contes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteValide())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "eleve")
    void creer_withStudentRole_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/projets-contes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteValide())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetConteService);
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void creer_withTeacherRole_shouldReturn201() throws Exception {
        when(projetConteService.creer(any(ProjetConteRequest.class), any())).thenReturn(reponse());

        mockMvc.perform(post("/api/projets-contes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteValide())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trancheAge").value("9-11"))
                .andExpect(jsonPath("$.espaceRepresentation").value("gymnase"))
                .andExpect(jsonPath("$.ingredientsSecrets[0]").value("dragons"));
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void creer_withMissingRequiredFields_shouldReturn400WithFieldErrors() throws Exception {
        ProjetConteRequest request = new ProjetConteRequest(
                "7-10", "France", " ", null, "", List.of(), null);

        mockMvc.perform(post("/api/projets-contes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.trancheAge").exists())
                .andExpect(jsonPath("$.errors.region").exists())
                .andExpect(jsonPath("$.errors.theme").exists());

        verifyNoInteractions(projetConteService);
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void modifierContexte_ofAnotherTeacherProject_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetConteService.modifierContexte(eq(id), any(ProjetConteRequest.class), any()))
                .thenThrow(new AccessDeniedException("Acces interdit a ce projet de conte"));

        mockMvc.perform(put("/api/projets-contes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteValide())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void consulter_unknownProject_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetConteService.consulter(eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Projet de conte non trouve avec l'ID: " + id));

        mockMvc.perform(get("/api/projets-contes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "professionnel_education")
    void lister_withEducationProfessionalRole_shouldReturn200() throws Exception {
        when(projetConteService.listerMesProjets(any())).thenReturn(List.of(reponse()));

        mockMvc.perform(get("/api/projets-contes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].theme").value("Le Moyen Age"));
    }

    private ProjetConteRequest requeteValide() {
        return new ProjetConteRequest("9-11", "France", "Centre-Val de Loire", "Tours",
                "Le Moyen Age", List.of("dragons"), EspaceRepresentation.gymnase);
    }

    private ProjetConteResponse reponse() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ProjetConteResponse(UUID.randomUUID(), UUID.randomUUID(), "9-11", "France",
                "Centre-Val de Loire", "Tours", "Le Moyen Age", List.of("dragons"),
                EspaceRepresentation.gymnase, now, now);
    }
}
