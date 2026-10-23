package com.codabli.dto;

import java.util.UUID;

import com.codabli.entity.enums.RoleMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMessageIaDto(
        @NotNull UUID conteDanseId,
        @NotNull RoleMessage role,
        @NotBlank String contenu) {
}
