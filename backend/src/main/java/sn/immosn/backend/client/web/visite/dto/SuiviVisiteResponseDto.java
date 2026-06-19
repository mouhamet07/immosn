package sn.immosn.backend.client.web.visite.dto;

import java.util.List;

/** Réponse du suivi public d'une visite par token de prospect — aucune authentification requise. */
public record SuiviVisiteResponseDto(
    String prospectNom,
    List<DemandeVisiteResponseDto> visites
) {}
