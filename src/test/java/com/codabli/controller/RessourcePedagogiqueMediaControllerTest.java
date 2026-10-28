package com.codabli.controller;

import com.codabli.dto.MediaDownloadResponse;
import com.codabli.service.RessourcePedagogiqueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RessourcePedagogiqueMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RessourcePedagogiqueService service;

    @Test
    @WithMockUser(roles = "eleve")
    void mediaDownloadRequiresResourceAccessRole() throws Exception {
        mockMvc.perform(get("/api/ressources-pedagogiques/{id}/media", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void mediaDownloadReturnsSignedUrlForAuthorizedResource() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        when(service.getMediaUrl(eq(resourceId), isNull())).thenReturn(new MediaDownloadResponse(
                mediaId, "guide.pdf", "https://storage.example/signed"));

        mockMvc.perform(get("/api/ressources-pedagogiques/{id}/media", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").value(mediaId.toString()))
                .andExpect(jsonPath("$.url").value("https://storage.example/signed"));

        verify(service).getMediaUrl(eq(resourceId), isNull());
    }

    @Test
    @WithMockUser(roles = "comite_lecture")
    void committeeCanAttachMedia() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        when(service.attacherMedia(resourceId, mediaId)).thenReturn(new MediaDownloadResponse(
                mediaId, "guide.pdf", "https://storage.example/signed"));

        mockMvc.perform(put("/api/ressources-pedagogiques/{id}/media/{mediaId}", resourceId, mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").value(mediaId.toString()));

        verify(service).attacherMedia(resourceId, mediaId);
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void detachingMediaRequiresManagementRole() throws Exception {
        mockMvc.perform(delete("/api/ressources-pedagogiques/{id}/media", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "comite_lecture")
    void committeeCanDetachMedia() throws Exception {
        UUID resourceId = UUID.randomUUID();

        mockMvc.perform(delete("/api/ressources-pedagogiques/{id}/media", resourceId))
                .andExpect(status().isNoContent());

        verify(service).detacherMedia(resourceId);
    }
}
