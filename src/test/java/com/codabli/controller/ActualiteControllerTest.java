package com.codabli.controller;

import com.codabli.dto.ActualiteRequest;
import com.codabli.dto.ActualiteResponse;
import com.codabli.service.ActualiteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration pour ActualiteController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActualiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActualiteService actualiteService;

    @Test
    void lister_shouldBeAccessibleToPublic() throws Exception {
        mockMvc.perform(get("/api/actualites"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_shouldBeAccessibleToPublic() throws Exception {
        UUID id = UUID.randomUUID();
        ActualiteResponse response = ActualiteResponse.builder()
                .id(id)
                .titre("Test Titre")
                .build();
        when(actualiteService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/actualites/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Test Titre"));
    }

    @Test
    void creer_withoutAuth_shouldReturn401() throws Exception {
        ActualiteRequest request = ActualiteRequest.builder()
                .titre("Titre")
                .contenu("Contenu")
                .build();

        mockMvc.perform(post("/api/actualites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "eleve")
    void creer_withEleveRole_shouldReturn403() throws Exception {
        ActualiteRequest request = ActualiteRequest.builder()
                .titre("Titre")
                .contenu("Contenu")
                .build();

        mockMvc.perform(post("/api/actualites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "admin")
    void creer_withAdminRole_shouldReturn201() throws Exception {
        ActualiteRequest request = ActualiteRequest.builder()
                .titre("Titre")
                .contenu("Contenu")
                .build();

        ActualiteResponse response = ActualiteResponse.builder()
                .id(UUID.randomUUID())
                .titre("Titre")
                .build();

        when(actualiteService.creer(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/actualites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Titre"));
    }
}
