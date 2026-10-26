package com.codabli.controller;

import com.codabli.dto.MediaUploadResponse;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.service.MediaUploadService;
import com.codabli.service.MediaLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaUploadService mediaUploadService;

    @MockitoBean
    private MediaLifecycleService mediaLifecycleService;

    @Test
    void uploadWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/media").file(pngFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void uploadWithoutMediaManagementRoleIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/media").file(pngFile()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "comite_lecture")
    void authorizedUploadReturnsMediaMetadata() throws Exception {
        UUID mediaId = UUID.randomUUID();
        when(mediaUploadService.upload(any())).thenReturn(new MediaUploadResponse(
                mediaId, "dessin.png", "image/png", 128, MediaStatus.AVAILABLE));

        mockMvc.perform(multipart("/api/media").file(pngFile()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(mediaId.toString()))
                .andExpect(jsonPath("$.originalName").value("dessin.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"));

        verify(mediaUploadService).upload(any());
    }

    @Test
    @WithMockUser(roles = "enseignant")
    void mediaDeletionRequiresAdministratorRole() throws Exception {
        mockMvc.perform(delete("/api/media/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "admin")
    void administratorQueuesMediaDeletion() throws Exception {
        UUID mediaId = UUID.randomUUID();

        mockMvc.perform(delete("/api/media/{id}", mediaId))
                .andExpect(status().isAccepted());

        verify(mediaLifecycleService).requestDeletion(mediaId);
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "dessin.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
    }
}
