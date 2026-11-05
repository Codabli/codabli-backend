package com.codabli.entity;

import com.codabli.service.QwenGuardClient.SafetyStatus;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refus_moderation_qwen_guard", indexes = {
        @Index(name = "idx_refus_qwen_guard_conte_date", columnList = "conte_danse_id, date_creation"),
        @Index(name = "idx_refus_qwen_guard_statut_date", columnList = "statut, date_creation")
})
@Getter
@Setter
@NoArgsConstructor
public class RefusModerationQwenGuard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conte_danse_id", nullable = false)
    private UUID conteDanseId;

    @Column(nullable = false, length = 32)
    private String assistant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SafetyStatus statut;

    @Column(name = "prompt_initial", nullable = false, columnDefinition = "text")
    private String promptInitial;

    @Column(name = "retour_qwen_guard", nullable = false, columnDefinition = "text")
    private String retourQwenGuard;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }
}
