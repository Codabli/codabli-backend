package com.codabli.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.codabli.dto.CreateMessageIaDto;
import com.codabli.dto.MessageIaDto;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.MessageAssistantIa;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.RoleMessage;
import com.codabli.entity.enums.StatutConte;
import com.codabli.event.ConteDansePublieEvent;
import com.codabli.repository.MessageAssistantIaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
        return messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(conteDanseId).stream()
                .map(this::toDto)
                .toList();
    }

    public MessageIaDto ajouter(CreateMessageIaDto request, Jwt jwt) {
        ConteDanse conte = verifierAcces(request.conteDanseId(), jwt);
        verifierConteNonPublie(conte);

        MessageAssistantIa message = new MessageAssistantIa();
        message.setConteDanseId(request.conteDanseId());
        message.setRole(request.role());
        message.setContenu(promptAnonymizerService.anonymize(request.contenu()));

        return toDto(messageRepository.save(message));
    }

    public List<MessageIaDto> ajouterTour(UUID conteDanseId,
                                          String contenuUtilisateur,
                                          String contenuAssistant,
                                          Jwt jwt) {
        ConteDanse conte = verifierAcces(conteDanseId, jwt);
        verifierConteNonPublie(conte);

        MessageAssistantIa messageUtilisateur = creerMessage(
                conteDanseId, RoleMessage.user, promptAnonymizerService.anonymize(contenuUtilisateur));
        MessageAssistantIa messageAssistant = creerMessage(
                conteDanseId, RoleMessage.assistant, promptAnonymizerService.anonymize(contenuAssistant));

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
        ConteDanse conte = conteDanseService.findById(conteDanseId);
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
            throw new IllegalStateException("L'historique d'un conte publie ne peut plus etre modifie");
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
}
