package sn.immosn.backend.client.web.contrat.dto;

import sn.immosn.backend.contrat.data.entity.StatutContrat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LOCATION : seuls dateDebut, dureeLocationMois, statut, documentUrl, notes sont modifiables.
 *            montant et dateFin sont recalculés automatiquement.
 * VENTE    : dateDebut, dateFin, montant, statut, documentUrl, notes sont modifiables.
 *            dureeLocationMois est bloqué (non applicable).
 */
public record ContratUpdateRequestDto(
    LocalDate dateDebut,
    LocalDate dateFin,
    BigDecimal montant,
    Integer dureeLocationMois,
    StatutContrat statut,
    String documentUrl,
    String notes,
    // Sprint 4 — paramètres d'éligibilité des signalements
    LocalDate dateFinGarantie,
    String clausesContractuelles
) {}
