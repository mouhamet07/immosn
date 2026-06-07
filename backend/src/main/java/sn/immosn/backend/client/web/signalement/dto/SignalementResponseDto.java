package sn.immosn.backend.client.web.signalement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;

import java.time.LocalDateTime;

public record SignalementResponseDto(
    Long id,
    Long contratId,
    String annonceLibelle,
    Long clientId,
    String clientNom,
    String contenu,
    StatutSignalement statut,
    @JsonProperty("isRead") boolean isRead,
    String reponseAdmin,
    LocalDateTime createdAt
) {}
