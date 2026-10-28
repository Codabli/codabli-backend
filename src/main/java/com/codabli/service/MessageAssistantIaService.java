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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MessageAssistantIaService {

    private final MessageAssistantIaRepository messageRepository;
    private final ConteDanseService conteDanseService;
    private final UtilisateurService utilisateurService;
    private final PromptAnonymizerService promptAnonymizerService;

    public MessageAssistantIaService(MessageAssistantIaRepository messageRepository,
                                     ConteDanseService conteDanseService,
                                     UtilisateurService utilisateurService,
                                     PromptAnonymizerService promptAnonymizerService) {
        this.messageRepository = messageRepository;
        this.conteDanseService = conteDanseService;
        this.utilisateurService = utilisateurService;
        this.promptAnonymizerService = promptAnonymizerService;
    }

    @Transactional(readOnly = true)
    public List<MessageIaDto> listerHistorique(UUID conteDanseId, Jwt jwt) {
        verifierAcces(conteDanseId, jwt);
        return chargerHistorique(conteDanseId);
    }

    @Transactional(readOnly = true)
    public List<MessageIaDto> listerHistoriquePourConversation(UUID conteDanseId, Jwt jwt) {
        verifierConteNonPublie(verifierAcces(conteDanseId, jwt));
        return chargerHistorique(conteDanseId);
    }

    private List<MessageIaDto> chargerHistorique(UUID conteDanseId) {
        return messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(conteDanseId).stream()
                .map(this::toDto)
                .toList();
    }

    public MessageIaDto ajouter(CreateMessageIaDto request, Jwt jwt) {
        ConteDanse conte = verifierAcces(conteDanseService.verrouillerPourHistorique(request.conteDanseId()), jwt);
        verifierConteNonPublie(conte);

        MessageAssistantIa message = new MessageAssistantIa();
        message.setConteDanseId(request.conteDanseId());
        message.setRole(request.role());
        message.setContenu(promptAnonymizerService.anonymize(request.contenu()));
        message.setDateCreation(prochaineDateCreation(request.conteDanseId()));

        return toDto(messageRepository.save(message));
    }

    public List<MessageIaDto> ajouterTour(UUID conteDanseId,
                                          String contenuUtilisateur,
                                          String contenuAssistant,
                                          Jwt jwt) {
        ConteDanse conte = verifierAcces(conteDanseService.verrouillerPourHistorique(conteDanseId), jwt);
        verifierConteNonPublie(conte);

        MessageAssistantIa messageUtilisateur = creerMessage(
                conteDanseId, RoleMessage.user, promptAnonymizerService.anonymize(contenuUtilisateur));
        MessageAssistantIa messageAssistant = creerMessage(
                conteDanseId, RoleMessage.assistant, promptAnonymizerService.anonymize(contenuAssistant));
        Instant dateUtilisateur = prochaineDateCreation(conteDanseId);
        messageUtilisateur.setDateCreation(dateUtilisateur);
        messageAssistant.setDateCreation(dateUtilisateur.plus(1, ChronoUnit.MICROS));

        return messageRepository.saveAll(List.of(messageUtilisateur, messageAssistant)).stream()
                .map(this::toDto)
                .toList();
    }

    public void supprimerHistorique(UUID conteDanseId) {
        messageRepository.deleteByConteDanseId(conteDanseId);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void supprimerHistoriqueLorsPublication(ConteDansePublieEvent event) {
        supprimerHistorique(event.conteDanseId());
    }

    private ConteDanse verifierAcces(UUID conteDanseId, Jwt jwt) {
        return verifierAcces(conteDanseService.findById(conteDanseId), jwt);
    }

    private ConteDanse verifierAcces(ConteDanse conte, Jwt jwt) {
        Utilisateur utilisateur = utilisateurService.getUtilisateurFromJwt(jwt);
        Collection<String> roles = utilisateurService.extractRoles(jwt);

        boolean estAuteur = conte.getCreateur().getId().equals(utilisateur.getId());
        boolean estAdmin = roles.contains("admin") || roles.contains("super_admin") || roles.contains("moderateur");

        if (!estAuteur && !estAdmin) {
            throw new AccessDeniedException("Vous ne pouvez acceder qu'a l'historique de vos propres contes danses");
        }
        return conte;
    }

    private void verifierConteNonPublie(ConteDanse conte) {
        if (conte.getStatut() == StatutConte.publie) {
            throw new BusinessException(ErrorCode.CONTE_DEJA_PUBLIE,
                    "L'historique d'un conte publie ne peut plus etre modifie");
        }
    }

    private MessageIaDto toDto(MessageAssistantIa message) {
        return new MessageIaDto(
                message.getId(),
                message.getConteDanseId(),
                message.getRole(),
                message.getContenu(),
                message.getDateCreation());
    }

    private MessageAssistantIa creerMessage(UUID conteDanseId, RoleMessage role, String contenu) {
        MessageAssistantIa message = new MessageAssistantIa();
        message.setConteDanseId(conteDanseId);
        message.setRole(role);
        message.setContenu(contenu);
        return message;
    }

    private Instant prochaineDateCreation(UUID conteDanseId) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return messageRepository.findFirstByConteDanseIdOrderByDateCreationDescIdDesc(conteDanseId)
                .filter(message -> !now.isAfter(message.getDateCreation()))
                .map(message -> message.getDateCreation().plus(1, ChronoUnit.MICROS))
                .orElse(now);
    }
}
