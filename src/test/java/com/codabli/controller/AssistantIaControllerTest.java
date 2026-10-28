package com.codabli.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.codabli.dto.AssistantIaRequest;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import com.codabli.dto.MessageIaDto;
import com.codabli.entity.enums.RoleMessage;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.service.AssistantIaService;
import com.codabli.service.LlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

@SpringBootTest
@AutoConfigureMockMvc
class AssistantIaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private AssistantIaService assistantIaService;

    @MockitoBean
    private LlmClient llmClient;

    @Test
    @WithMockUser
    void testCompletion_shouldReturnCompleteResponse() throws Exception {
        LlmRequest request = LlmRequest.of("Bonjour");
        LlmResponse response = LlmResponse.ok("Réponse", "", "stop", null, null);
        when(llmClient.complete(request)).thenReturn(response);

        mockMvc.perform(post("/api/ia/test-completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texte").value("Réponse"));

        verify(llmClient).complete(request);
    }

    @Test
    @WithMockUser
    void testStream_shouldStartSseRoute() throws Exception {
        LlmRequest request = LlmRequest.of("Bonjour");
        when(llmClient.stream(eq(request), any()))
                .thenAnswer(invocation -> {
                    invocation.<java.util.function.Consumer<String>>getArgument(1).accept("Réponse");
                    return LlmResponse.ok("Réponse", "", "stop", null, null);
                });

        MvcResult asyncResult = mockMvc.perform(post("/api/ia/test-stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
        verify(llmClient).stream(eq(request), any());
    }

    @Test
    @WithMockUser
    void historique_shouldReturnConversationHistory() throws Exception {
        UUID conteDanseId = UUID.randomUUID();
        when(assistantIaService.listerHistorique(eq(conteDanseId), nullable(Jwt.class)))
                .thenReturn(List.of(new MessageIaDto(UUID.randomUUID(), conteDanseId, RoleMessage.user,
                        "Bonjour", Instant.parse("2026-01-01T10:00:00Z"))));

        mockMvc.perform(get("/api/ia/contes-danses/{id}/assistant-history", conteDanseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conteDanseId").value(conteDanseId.toString()))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].contenu").value("Bonjour"));
    }

    @Test
    @WithMockUser
    void assistantStream_shouldStartPersistentSseRoute() throws Exception {
        UUID conteDanseId = UUID.randomUUID();
        AssistantIaRequest request = new AssistantIaRequest("Bonjour", null);
        AssistantIaService.ConversationPreparee conversation = new AssistantIaService.ConversationPreparee(
                conteDanseId, "Bonjour", null, LlmRequest.of("Bonjour"));
        when(assistantIaService.preparerConversation(eq(conteDanseId), any(AssistantIaRequest.class),
                nullable(Jwt.class))).thenReturn(conversation);
        when(assistantIaService.streamerConversation(eq(conversation), any()))
                .thenAnswer(invocation -> {
                    invocation.<java.util.function.Consumer<String>>getArgument(1).accept("Réponse");
                    return LlmResponse.ok("Réponse", "", "stop", null, null);
                });

        MvcResult asyncResult = mockMvc.perform(post("/api/ia/contes-danses/{id}/assistant-stream", conteDanseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
        verify(assistantIaService).streamerConversation(eq(conversation), any());
    }

    @Test
    @WithMockUser
    void assistantStream_withPublishedConte_shouldReturnConflictBeforeStartingSse() throws Exception {
        UUID conteDanseId = UUID.randomUUID();
        when(assistantIaService.preparerConversation(eq(conteDanseId), any(AssistantIaRequest.class),
                nullable(Jwt.class)))
                .thenThrow(new BusinessException(ErrorCode.CONTE_DEJA_PUBLIE, "Conte deja publie"));

        mockMvc.perform(post("/api/ia/contes-danses/{id}/assistant-stream", conteDanseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantIaRequest("Bonjour", null))))
                .andExpect(status().isConflict())
                .andExpect(request().asyncNotStarted())
                .andExpect(jsonPath("$.error-code").value("error.conte-already-published"));

        verify(assistantIaService, never()).streamerConversation(any(), any());
        verifyNoInteractions(llmClient);
    }
}
