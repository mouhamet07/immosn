package sn.immosn.backend.client.web.signalement.dto;

import sn.immosn.backend.signalement.data.entity.StatutSignalement;

import java.time.LocalDateTime;

public record SignalementHistoryDto(
    Long id,
    StatutSignalement ancienStatut,
    StatutSignalement nouveauStatut,
    String auteurEmail,
    String action,
    String motif,
    LocalDateTime createdAt
) {}
