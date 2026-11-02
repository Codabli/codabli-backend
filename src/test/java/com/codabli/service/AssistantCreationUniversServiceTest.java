package com.codabli.service;

import com.codabli.dto.AssistantCreationUniversRequest;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantCreationUniversServiceTest {

    @Test
    void compose_le_socle_commun_avec_la_specialisation_et_transmet_la_description() {
        LlmClient llmClient = mock(LlmClient.class);
        LlmResponse expectedResponse = LlmResponse.ok("Question de réflexion", "", "stop", null, null);
        when(llmClient.stream(any(), anyString(), any())).thenReturn(expectedResponse);
        AssistantCreationUniversService service =
                new AssistantCreationUniversService(llmClient, new DefaultResourceLoader());
        List<AssistantCreationUniversRequest> requests = List.of(
                new AssistantCreationUniversRequest(AssistantCreationUniversRequest.Type.Lieu, "Lieu à préciser"),
                new AssistantCreationUniversRequest(AssistantCreationUniversRequest.Type.Personnage,
                        "Personnage à préciser"),
                new AssistantCreationUniversRequest(AssistantCreationUniversRequest.Type.Epoque,
                        "Époque à préciser"),
                new AssistantCreationUniversRequest(AssistantCreationUniversRequest.Type.Objet,
                        "Objet à préciser"));
        List<String> specialisations = List.of(
                "Spécialisation : aide à réfléchir à la description d'un lieu",
                "Spécialisation : aide à réfléchir à la description d'un personnage",
                "Spécialisation : aide à réfléchir à la description de l'époque",
                "Spécialisation : aide à réfléchir à la description d'un objet");

        for (int i = 0; i < requests.size(); i++) {
            AssistantCreationUniversRequest request = requests.get(i);
            String specialisation = specialisations.get(i);

            assertThat(service.streamerCreationUnivers(request, ignored -> {})).isSameAs(expectedResponse);

            verify(llmClient).stream(eq(LlmRequest.of(request.description())),
                    org.mockito.ArgumentMatchers.argThat(prompt ->
                            prompt.contains("Tu es l'assistant pédagogique")
                                    && prompt.contains(specialisation)), any());
        }

        verify(llmClient, times(4)).stream(any(), anyString(), any());
    }
}
