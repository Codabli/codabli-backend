package com.codabli.service;

import com.codabli.dto.AssistantCreationUniversRequest;
import com.codabli.dto.ConteContextData;
import com.codabli.dto.LlmRequest;
import com.codabli.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class AssistantCreationUniversServiceTest {

    private static final UUID CONTE_DANSE_ID = UUID.randomUUID();

    private final LlmClient llmClient = mock(LlmClient.class);
    private final AssistantPromptModerationService promptModerationService =
            mock(AssistantPromptModerationService.class);
    private final PromptAnonymizerService promptAnonymizerService = mock(PromptAnonymizerService.class);
    private final ConteContextService conteContextService = mock(ConteContextService.class);
    private final ConteContextFormatter conteContextFormatter = new ConteContextFormatter();
    private final Jwt jwt = mock(Jwt.class);
    private final AssistantCreationUniversService service = new AssistantCreationUniversService(
            llmClient, promptModerationService, promptAnonymizerService, new DefaultResourceLoader(),
            conteContextService, conteContextFormatter);

    @BeforeEach
    void setUp() {
        when(promptAnonymizerService.anonymize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(promptModerationService.moderer(anyString(), any(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(conteContextService.charger(eq(CONTE_DANSE_ID), eq(jwt))).thenReturn(context());
    }

    @Test
    void compose_le_socle_commun_avec_la_specialisation_et_transmet_la_description() {
        LlmResponse expectedResponse = LlmResponse.ok("Question de réflexion", "", "stop", null, null);
        when(llmClient.stream(any(), anyString(), any())).thenReturn(expectedResponse);
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

            assertThat(streamer(request, ignored -> {}))
                    .isSameAs(expectedResponse);

            verify(llmClient).stream(eq(LlmRequest.of(request.description())),
                    org.mockito.ArgumentMatchers.argThat(prompt ->
                            prompt.contains("Tu es l'assistant pédagogique")
                                    && prompt.contains(specialisation)
                                    && prompt.startsWith("<conte_context>")
                                    && prompt.contains("<ingredients_secrets>")
                                    && prompt.contains("Bretonne")), any());
        }

        verify(llmClient, times(4)).stream(any(), anyString(), any());
    }

    @Test
    void anonymise_avant_qwenGuard_et_transmet_la_description_anonymisee_au_llm() {
        String descriptionAnonymisee = "Contact [EMAIL_1] pour ce lieu";
        AssistantCreationUniversRequest request = new AssistantCreationUniversRequest(
                AssistantCreationUniversRequest.Type.Lieu, "Contact enfant@example.com pour ce lieu");
        when(promptAnonymizerService.anonymize(request.description())).thenReturn(descriptionAnonymisee);
        streamer(request, ignored -> {});

        var ordre = inOrder(promptAnonymizerService, promptModerationService, llmClient);
        ordre.verify(promptAnonymizerService).anonymize(request.description());
        ordre.verify(promptModerationService)
                .moderer(eq("assistant-creation-univers"), eq(CONTE_DANSE_ID), eq(descriptionAnonymisee), any());
        ordre.verify(llmClient).stream(eq(LlmRequest.of(descriptionAnonymisee)), anyString(), any());
    }

    @Test
    void refuse_une_description_unsafe_sans_appeler_le_llm() {
        AssistantCreationUniversRequest request = new AssistantCreationUniversRequest(
                AssistantCreationUniversRequest.Type.Personnage, "Contact enfant@example.com");
        String descriptionAnonymisee = "Contact [EMAIL_1]";
        when(promptAnonymizerService.anonymize(request.description())).thenReturn(descriptionAnonymisee);
        stubModerationResponse(descriptionAnonymisee,
                "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.", false);

        LlmResponse response = streamer(request, ignored -> {});

        assertThat(response.texte())
                .isEqualTo("Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.");
        verify(promptModerationService)
                .moderer(eq("assistant-creation-univers"), eq(CONTE_DANSE_ID), eq(descriptionAnonymisee), any());
        verifyNoInteractions(llmClient);
    }

    @Test
    void refuse_une_description_controversial_sans_appeler_le_llm() {
        AssistantCreationUniversRequest request = new AssistantCreationUniversRequest(
                AssistantCreationUniversRequest.Type.Personnage, "description anonymisée");
        stubModerationResponse(request.description(),
                "Merci de reformuler votre demande en évitant les sujets sensibles.", false);

        LlmResponse response = streamer(request, ignored -> {});

        assertThat(response.texte()).isEqualTo("Merci de reformuler votre demande en évitant les sujets sensibles.");
        verifyNoInteractions(llmClient);
    }

    @Test
    void interrompt_le_traitement_si_qwenGuard_est_indisponible() {
        AssistantCreationUniversRequest request = new AssistantCreationUniversRequest(
                AssistantCreationUniversRequest.Type.Lieu, "Contact enfant@example.com");
        String descriptionAnonymisee = "Contact [EMAIL_1]";
        when(promptAnonymizerService.anonymize(request.description())).thenReturn(descriptionAnonymisee);
        stubModerationResponse(descriptionAnonymisee,
                "Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.", true);

        LlmResponse response = streamer(request, ignored -> {});

        assertThat(response.texte())
                .isEqualTo("Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.");
        assertThat(response.degrade()).isTrue();
        verify(promptModerationService)
                .moderer(eq("assistant-creation-univers"), eq(CONTE_DANSE_ID), eq(descriptionAnonymisee), any());
        verify(llmClient, never()).stream(any(), anyString(), any());
    }

    @Test
    void interrompt_le_traitement_si_qwenGuard_ne_retourne_aucun_statut() {
        AssistantCreationUniversRequest request = new AssistantCreationUniversRequest(
                AssistantCreationUniversRequest.Type.Lieu, "description anonymisée");
        stubModerationResponse(request.description(),
                "Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.", true);

        LlmResponse response = streamer(request, ignored -> {});

        assertThat(response.texte())
                .isEqualTo("Le contrôle de sécurité est temporairement indisponible. Merci de réessayer.");
        assertThat(response.degrade()).isTrue();
        verify(llmClient, never()).stream(any(), anyString(), any());
    }

    @Test
    void transmet_le_message_de_refus_sur_le_flux_sse() {
        AssistantCreationUniversRequest request = new AssistantCreationUniversRequest(
                AssistantCreationUniversRequest.Type.Objet, "description anonymisée");
        var tokens = new java.util.ArrayList<String>();
        stubModerationResponse(request.description(),
                "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.", false);

        streamer(request, tokens::add);

        assertThat(tokens).containsExactly(
                "Votre demande contient des thématiques inadaptées. Merci de proposer un autre sujet.");
    }

    private void stubModerationResponse(String prompt, String message, boolean degrade) {
        LlmResponse response = degrade
                ? LlmResponse.standby(message)
                : LlmResponse.ok(message, "", "stop", null, null);
        when(promptModerationService.moderer(eq("assistant-creation-univers"),
                eq(CONTE_DANSE_ID), eq(prompt), any()))
                .thenAnswer(invocation -> {
                    invocation.<java.util.function.Consumer<String>>getArgument(3).accept(message);
                    return Optional.of(response);
                });
    }

    @Test
    void refuse_un_conte_inaccessible_avant_toute_communication_avec_le_llm() {
        AssistantCreationUniversRequest request = new AssistantCreationUniversRequest(
                AssistantCreationUniversRequest.Type.Lieu, "Un jardin");
        org.mockito.Mockito.doThrow(new AccessDeniedException("Accès refusé"))
                .when(conteContextService).charger(CONTE_DANSE_ID, jwt);

        org.junit.jupiter.api.Assertions.assertThrows(AccessDeniedException.class,
                () -> service.preparerCreationUnivers(CONTE_DANSE_ID, request, jwt));

        verifyNoInteractions(llmClient, promptAnonymizerService, promptModerationService);
    }

    private LlmResponse streamer(AssistantCreationUniversRequest request,
                                 java.util.function.Consumer<String> onToken) {
        return service.streamerCreationUnivers(
                service.preparerCreationUnivers(CONTE_DANSE_ID, request, jwt), onToken);
    }

    private static ConteContextData context() {
        return new ConteContextData("Le conte", "Amitié", List.of("une clé", "un secret"),
                "France", "Bretonne", 6, 10);
    }
}
