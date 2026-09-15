package com.codabli.controller;

import com.codabli.dto.ChangerStatutCommandeRequest;
import com.codabli.dto.CommandeResponse;
import com.codabli.dto.CreerCommandeRequest;
import com.codabli.entity.enums.StatutCommande;
import com.codabli.service.CommandeService;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommandeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private CommandeService commandeService;

    @Test
    void creerCommande_withoutAuth_shouldReturn401() throws Exception {
        CreerCommandeRequest request = CreerCommandeRequest.builder()
                .adresseLivraisonId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/commandes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "eleve")
    void creerCommande_withUserRole_shouldReturn201() throws Exception {
        CreerCommandeRequest request = CreerCommandeRequest.builder()
                .adresseLivraisonId(UUID.randomUUID())
                .build();

        CommandeResponse response = CommandeResponse.builder()
                .id(UUID.randomUUID())
                .statut(StatutCommande.en_attente)
                .total(new BigDecimal("25.00"))
                .build();

        when(commandeService.creerCommande(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/commandes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("en_attente"))
                .andExpect(jsonPath("$.total").value(25.00));
    }

    @Test
    @WithMockUser(roles = "eleve")
    void getCommandeById_forAnotherUser_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();

        when(commandeService.getCommandeById(eq(id), any()))
                .thenThrow(new AccessDeniedException("Vous ne pouvez pas acceder a cette commande"));

        mockMvc.perform(get("/api/commandes/" + id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "eleve")
    void changerStatut_withUserRole_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();
        ChangerStatutCommandeRequest request = ChangerStatutCommandeRequest.builder()
                .statut(StatutCommande.payee)
                .build();

        mockMvc.perform(patch("/api/admin/commandes/" + id + "/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "admin")
    void changerStatut_withAdminRole_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        ChangerStatutCommandeRequest request = ChangerStatutCommandeRequest.builder()
                .statut(StatutCommande.payee)
                .build();

        CommandeResponse response = CommandeResponse.builder()
                .id(id)
                .statut(StatutCommande.payee)
                .build();

        when(commandeService.changerStatut(eq(id), eq(StatutCommande.payee))).thenReturn(response);

        mockMvc.perform(patch("/api/admin/commandes/" + id + "/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("payee"));
    }
}
