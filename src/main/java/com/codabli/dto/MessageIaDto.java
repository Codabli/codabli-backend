package com.codabli.dto;

import java.time.Instant;
import java.util.UUID;

import com.codabli.entity.enums.RoleMessage;

public record MessageIaDto(
        UUID id,
        UUID conteDanseId,
        RoleMessage role,
        String contenu,
        Instant dateCreation) {
}
