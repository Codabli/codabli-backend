package com.codabli.controller;

import com.codabli.dto.ConteDanseRequest;
import com.codabli.dto.ConteDanseResponse;
import com.codabli.service.ConteDanseService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration pour ConteDanseController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConteDanseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConteDanseService conteDanseService;

    @Test
    void lister_shouldBeAccessibleToPublic() throws Exception {
        mockMvc.perform(get("/api/contes-danses"))
                .andExpect(status().isOk());
    }

    @Test
    void creer_withoutAuth_shouldReturn401() throws Exception {
        ConteDanseRequest request = ConteDanseRequest.builder()
                .titre("Titre conte")
                .build();

        mockMvc.perform(post("/api/contes-danses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "eleve")
    void creer_withAuth_shouldReturn201() throws Exception {
        ConteDanseRequest request = ConteDanseRequest.builder()
                .titre("Titre conte")
                .build();

        ConteDanseResponse response = ConteDanseResponse.builder()
                .id(UUID.randomUUID())
                .titre("Titre conte")
                .build();

        when(conteDanseService.creer(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/contes-danses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Titre conte"));
    }
}
