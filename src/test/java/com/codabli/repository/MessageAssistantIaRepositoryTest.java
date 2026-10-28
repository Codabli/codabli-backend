package com.codabli.repository;

import com.codabli.entity.ConteDanse;
import com.codabli.entity.MessageAssistantIa;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.RoleMessage;
import com.codabli.entity.enums.RoleUtilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MessageAssistantIaRepositoryTest {

    @Autowired
    private MessageAssistantIaRepository repository;

    @Autowired
    private ConteDanseRepository conteRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Test
    void findByConteDanseIdOrderByDateCreationAscIdAsc_returnsMessagesChronologicallyWithStableTieBreak() {
        UUID conteDanseId = creerConte();
        UUID autreConteDanseId = creerConte();
        Instant premiereDate = Instant.parse("2026-01-01T10:00:00Z");
        Instant secondeDate = Instant.parse("2026-01-01T10:01:00Z");

        MessageAssistantIa plusRecent = creerMessage(conteDanseId, secondeDate, "message récent");
        MessageAssistantIa memeDateSecond = creerMessage(conteDanseId, premiereDate, "message à égalité 2");
        MessageAssistantIa autreConte = creerMessage(autreConteDanseId, premiereDate, "autre conte");
        MessageAssistantIa memeDateFirst = creerMessage(conteDanseId, premiereDate, "message à égalité 1");

        repository.saveAllAndFlush(List.of(plusRecent, memeDateSecond, autreConte, memeDateFirst));

        List<MessageAssistantIa> result =
                repository.findByConteDanseIdOrderByDateCreationAscIdAsc(conteDanseId);
        List<UUID> expectedIds = Stream.of(memeDateSecond, memeDateFirst)
                .map(MessageAssistantIa::getId)
                .sorted(Comparator.comparing(UUID::toString))
                .toList();

        assertEquals(List.of(expectedIds.get(0), expectedIds.get(1), plusRecent.getId()),
                result.stream().map(MessageAssistantIa::getId).toList());
    }

    private UUID creerConte() {
        Utilisateur auteur = utilisateurRepository.saveAndFlush(Utilisateur.builder()
                .nom("Auteur").prenom("Test").role(RoleUtilisateur.enseignant).build());
        return conteRepository.saveAndFlush(ConteDanse.builder()
                .titre("Conte de test").createur(auteur).build()).getId();
    }

    private MessageAssistantIa creerMessage(UUID conteDanseId, Instant dateCreation, String contenu) {
        MessageAssistantIa message = new MessageAssistantIa();
        message.setConteDanseId(conteDanseId);
        message.setRole(RoleMessage.user);
        message.setContenu(contenu);
        message.setDateCreation(dateCreation);
        return message;
    }
}
