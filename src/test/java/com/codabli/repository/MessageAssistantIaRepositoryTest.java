package com.codabli.repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.codabli.entity.MessageAssistantIa;
import com.codabli.entity.enums.RoleMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class MessageAssistantIaRepositoryTest {

    @Autowired
    private MessageAssistantIaRepository repository;

    @Test
    void findByConteDanseIdOrderByDateCreationAscIdAsc_returnsMessagesChronologicallyWithStableTieBreak() {
        UUID conteDanseId = UUID.randomUUID();
        UUID autreConteDanseId = UUID.randomUUID();
        Instant premiereDate = Instant.parse("2026-01-01T10:00:00Z");
        Instant secondeDate = Instant.parse("2026-01-01T10:01:00Z");

        MessageAssistantIa plusRecent = creerMessage(conteDanseId, secondeDate, "message récent");
        MessageAssistantIa memeDateSecond = creerMessage(conteDanseId, premiereDate, "message à égalité 2");
        MessageAssistantIa autreConte = creerMessage(autreConteDanseId, premiereDate, "autre conte");
        MessageAssistantIa memeDateFirst = creerMessage(conteDanseId, premiereDate, "message à égalité 1");

        repository.saveAllAndFlush(List.of(plusRecent, memeDateSecond, autreConte, memeDateFirst));

        List<MessageAssistantIa> result =
                repository.findByConteDanseIdOrderByDateCreationAscIdAsc(conteDanseId);
        List<UUID> expectedIds = List.of(memeDateSecond, memeDateFirst).stream()
                .sorted(Comparator.comparing(MessageAssistantIa::getId))
                .map(MessageAssistantIa::getId)
                .toList();

        assertEquals(List.of(expectedIds.get(0), expectedIds.get(1), plusRecent.getId()),
                result.stream().map(MessageAssistantIa::getId).toList());
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
