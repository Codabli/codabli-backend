package com.codabli.controller;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.service.ConteDanseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration pour ConteDanseController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConteDanseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private ConteDanseService conteDanseService;

    @Test
    void lister_shouldBeAccessibleToPublic() throws Exception {
        when(conteDanseService.listerPublies(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/contes-danses"))
                .andExpect(status().isOk());

        verify(conteDanseService).listerPublies(any(Pageable.class));
    }

    @Test
    void lister_withThematiqueOnly_shouldUseOptionalFilters() throws Exception {
        when(conteDanseService.filtrerPublies(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/api/contes-danses").param("thematique", "amitie"))
                .andExpect(status().isOk());

        verify(conteDanseService).filtrerPublies(eq(null), eq(null), eq("amitie"), eq(null), eq(null), any());
    }

    @Test
    void lister_withCountryOnly_shouldUseOptionalFilters() throws Exception {
        when(conteDanseService.filtrerPublies(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/api/contes-danses").param("pays", "Liban"))
                .andExpect(status().isOk());

        verify(conteDanseService).filtrerPublies(eq(null), eq("Liban"), eq(null), eq(null), eq(null), any());
    }

    @Test
    void lister_withEmptyFilters_shouldStillBeAccepted() throws Exception {
        when(conteDanseService.filtrerPublies(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/api/contes-danses").param("thematique", "").param("pays", " "))
                .andExpect(status().isOk());

        verify(conteDanseService).filtrerPublies(eq(null), eq(" "), eq(""), eq(null), eq(null), any());
    }

    @Test
    void lister_shouldExposeTranslationsField() throws Exception {
        when(conteDanseService.listerPublies(any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(conteDanseResponse())));

        mockMvc.perform(get("/api/contes-danses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].traductions").isArray());
    }

    @Test
    void creer_withoutAuth_shouldReturn401() throws Exception {
        ConteDanseRequest request = conteDanseRequest();

        mockMvc.perform(post("/api/contes-danses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "eleve")
    void creer_withAuth_shouldReturn201() throws Exception {
        ConteDanseRequest request = conteDanseRequest();

        ConteDanseResponse response = conteDanseResponse();

        when(conteDanseService.creer(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/contes-danses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Titre conte"));
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void creer_sansTitre_shouldReturn201() throws Exception {
        // Parcours "Creer mon conte danse" : le brouillon est cree avant d'avoir un titre
        ConteDanseRequest request = new ConteDanseRequest(null, null, "Le Moyen Age", null, null, "France", null, 6, 8,
                null, null, null, null, null, null, null);
        when(conteDanseService.creer(any(), any())).thenReturn(conteDanseResponse());

        mockMvc.perform(post("/api/contes-danses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private ConteDanseRequest conteDanseRequest() {
        return new ConteDanseRequest("Titre conte", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private ConteDanseResponse conteDanseResponse() {
        return new ConteDanseResponse(
                UUID.randomUUID(),
                "Titre conte",
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                List.of());
    }
}
