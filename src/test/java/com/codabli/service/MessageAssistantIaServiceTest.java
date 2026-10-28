package com.codabli.service;

import com.codabli.dto.CreateMessageIaDto;
import com.codabli.dto.MessageIaDto;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.MessageAssistantIa;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.RoleMessage;
import com.codabli.entity.enums.StatutConte;
import com.codabli.event.ConteDansePublieEvent;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import com.codabli.repository.MessageAssistantIaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageAssistantIaServiceTest {

    @Mock
    private MessageAssistantIaRepository messageRepository;

    @Mock
    private ConteDanseService conteDanseService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private PromptAnonymizerService promptAnonymizerService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private MessageAssistantIaService messageService;

    private UUID conteDanseId;
    private Utilisateur auteur;
    private ConteDanse conte;

    @BeforeEach
    void setUp() {
        conteDanseId = UUID.randomUUID();
        auteur = new Utilisateur();
        auteur.setId(UUID.randomUUID());
        conte = new ConteDanse();
        conte.setId(conteDanseId);
        conte.setCreateur(auteur);
    }

    @Test
    void ajouter_withOwner_shouldSaveAndReturnDto() {
        when(conteDanseService.verrouillerPourHistorique(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());
        when(promptAnonymizerService.anonymize("Bonjour")).thenReturn("Bonjour");
        when(messageRepository.save(any(MessageAssistantIa.class))).thenAnswer(invocation -> {
            MessageAssistantIa message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            message.setDateCreation(Instant.now());
            return message;
        });
        CreateMessageIaDto request = new CreateMessageIaDto(conteDanseId, RoleMessage.user, "Bonjour");

        MessageIaDto result = messageService.ajouter(request, jwt);

        assertEquals(conteDanseId, result.conteDanseId());
        assertEquals(RoleMessage.user, result.role());
        assertEquals("Bonjour", result.contenu());
        verify(messageRepository).save(any(MessageAssistantIa.class));
    }

    @Test
    void ajouter_withNonOwner_shouldDenyAccess() {
        Utilisateur autreUtilisateur = new Utilisateur();
        autreUtilisateur.setId(UUID.randomUUID());
        when(conteDanseService.verrouillerPourHistorique(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(autreUtilisateur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());
        CreateMessageIaDto request = new CreateMessageIaDto(conteDanseId, RoleMessage.user, "Bonjour");

        assertThrows(AccessDeniedException.class, () -> messageService.ajouter(request, jwt));

        verify(messageRepository, never()).save(any(MessageAssistantIa.class));
    }

    @Test
    void ajouterTour_shouldSaveUserAndAssistantMessagesTogether() {
        when(conteDanseService.verrouillerPourHistorique(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());
        when(promptAnonymizerService.anonymize("Question")).thenReturn("Question");
        when(promptAnonymizerService.anonymize("Réponse")).thenReturn("Réponse");

        messageService.ajouterTour(conteDanseId, "Question", "Réponse", jwt);

        ArgumentCaptor<Iterable<MessageAssistantIa>> messagesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(messageRepository).saveAll(messagesCaptor.capture());
        List<MessageAssistantIa> messages = new ArrayList<>();
        messagesCaptor.getValue().forEach(messages::add);
        assertEquals(2, messages.size());
        assertEquals(RoleMessage.user, messages.get(0).getRole());
        assertEquals("Question", messages.get(0).getContenu());
        assertEquals(RoleMessage.assistant, messages.get(1).getRole());
        assertEquals("Réponse", messages.get(1).getContenu());
        assertEquals(messages.get(0).getDateCreation().plus(1, ChronoUnit.MICROS),
                messages.get(1).getDateCreation());
        assertEquals(messages.get(0).getDateCreation().truncatedTo(ChronoUnit.MICROS),
                messages.get(0).getDateCreation());
    }

    @Test
    void ajouterTour_shouldAnonymizeBothMessagesBeforeSaving() {
        when(conteDanseService.verrouillerPourHistorique(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());
        when(promptAnonymizerService.anonymize("Mon email est enfant@example.com"))
                .thenReturn("Mon email est [EMAIL]");
        when(promptAnonymizerService.anonymize("Je vous écrirai à enfant@example.com"))
                .thenReturn("Je vous écrirai à [EMAIL]");

        messageService.ajouterTour(conteDanseId,
                "Mon email est enfant@example.com",
                "Je vous écrirai à enfant@example.com",
                jwt);

        ArgumentCaptor<Iterable<MessageAssistantIa>> messagesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(messageRepository).saveAll(messagesCaptor.capture());
        List<MessageAssistantIa> messages = new ArrayList<>();
        messagesCaptor.getValue().forEach(messages::add);
        assertEquals("Mon email est [EMAIL]", messages.get(0).getContenu());
        assertEquals("Je vous écrirai à [EMAIL]", messages.get(1).getContenu());
    }

    @Test
    void ajouterTour_withPublishedConte_shouldNotSaveMessages() {
        conte.setStatut(StatutConte.publie);
        when(conteDanseService.verrouillerPourHistorique(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> messageService.ajouterTour(conteDanseId, "Question", "Réponse", jwt));

        assertEquals(ErrorCode.CONTE_DEJA_PUBLIE, exception.getErrorCode());
        verify(messageRepository, never()).saveAll(any());
    }

    @Test
    void listerHistoriquePourConversation_withPublishedConte_shouldRejectBeforeLoadingMessages() {
        conte.setStatut(StatutConte.publie);
        when(conteDanseService.findById(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> messageService.listerHistoriquePourConversation(conteDanseId, jwt));

        assertEquals(ErrorCode.CONTE_DEJA_PUBLIE, exception.getErrorCode());
        verify(messageRepository, never()).findByConteDanseIdOrderByDateCreationAscIdAsc(conteDanseId);
    }

    @Test
    void ajouter_shouldAnonymizeMessageBeforeSaving() {
        when(conteDanseService.verrouillerPourHistorique(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());
        when(promptAnonymizerService.anonymize("Contacte-moi à enfant@example.com"))
                .thenReturn("Contacte-moi à [EMAIL]");
        when(messageRepository.save(any(MessageAssistantIa.class))).thenAnswer(invocation -> {
            MessageAssistantIa message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            message.setDateCreation(Instant.now());
            return message;
        });

        MessageIaDto result = messageService.ajouter(
                new CreateMessageIaDto(conteDanseId, RoleMessage.user, "Contacte-moi à enfant@example.com"), jwt);

        assertEquals("Contacte-moi à [EMAIL]", result.contenu());
    }

    @Test
    void listerHistorique_withNonOwner_shouldDenyAccess() {
        Utilisateur autreUtilisateur = new Utilisateur();
        autreUtilisateur.setId(UUID.randomUUID());
        when(conteDanseService.findById(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(autreUtilisateur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> messageService.listerHistorique(conteDanseId, jwt));

        verify(messageRepository, never()).findByConteDanseIdOrderByDateCreationAscIdAsc(conteDanseId);
    }

    @Test
    void listerHistorique_withOwner_shouldReturnDtosInRepositoryOrder() {
        conte.setStatut(StatutConte.publie);
        Instant dateCreation = Instant.now();
        MessageAssistantIa message = new MessageAssistantIa();
        message.setId(UUID.randomUUID());
        message.setConteDanseId(conteDanseId);
        message.setRole(RoleMessage.assistant);
        message.setContenu("Que se passe-t-il ensuite ?");
        message.setDateCreation(dateCreation);
        when(conteDanseService.findById(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());
        when(messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(conteDanseId))
                .thenReturn(List.of(message));

        List<MessageIaDto> result = messageService.listerHistorique(conteDanseId, jwt);

        assertEquals(List.of(new MessageIaDto(message.getId(), conteDanseId, RoleMessage.assistant,
                "Que se passe-t-il ensuite ?", dateCreation)), result);
    }

    @Test
    void supprimerHistoriqueLorsPublication_shouldDeleteMessagesForPublishedConte() {
        messageService.supprimerHistoriqueLorsPublication(new ConteDansePublieEvent(conteDanseId));

        verify(messageRepository).deleteByConteDanseId(conteDanseId);
    }

    @Test
    void ajouterTour_shouldAppendAfterLastMessageEvenIfClockMovesBackwards() {
        when(conteDanseService.verrouillerPourHistorique(conteDanseId)).thenReturn(conte);
        when(utilisateurService.getUtilisateurFromJwt(jwt)).thenReturn(auteur);
        when(utilisateurService.extractRoles(jwt)).thenReturn(List.of());
        when(promptAnonymizerService.anonymize("Question")).thenReturn("Question");
        when(promptAnonymizerService.anonymize("Reponse")).thenReturn("Reponse");
        MessageAssistantIa precedent = new MessageAssistantIa();
        precedent.setDateCreation(Instant.parse("2099-01-01T10:00:00Z"));
        when(messageRepository.findFirstByConteDanseIdOrderByDateCreationDescIdDesc(conteDanseId))
                .thenReturn(Optional.of(precedent));

        messageService.ajouterTour(conteDanseId, "Question", "Reponse", jwt);

        ArgumentCaptor<Iterable<MessageAssistantIa>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(messageRepository).saveAll(captor.capture());
        List<MessageAssistantIa> messages = new ArrayList<>();
        captor.getValue().forEach(messages::add);
        assertEquals(precedent.getDateCreation().plus(1, ChronoUnit.MICROS), messages.get(0).getDateCreation());
        assertEquals(precedent.getDateCreation().plus(2, ChronoUnit.MICROS), messages.get(1).getDateCreation());
    }
}
