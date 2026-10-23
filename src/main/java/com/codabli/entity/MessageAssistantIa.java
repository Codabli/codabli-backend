package com.codabli.entity;

import java.time.Instant;
import java.util.UUID;

import com.codabli.entity.enums.RoleMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "messages_assistant_ia", indexes = {
        @Index(name = "idx_messages_assistant_ia_conte_date", columnList = "conte_danse_id, date_creation")
})
@Getter
@Setter
@NoArgsConstructor
public class MessageAssistantIa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conte_danse_id", nullable = false)
    private UUID conteDanseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoleMessage role;

    @Column(nullable = false, columnDefinition = "text")
    private String contenu;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }
}
