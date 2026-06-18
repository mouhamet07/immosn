package sn.immosn.backend.visite.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.annonce.data.entity.TypeTransaction;
import sn.immosn.backend.client.web.contrat.dto.ContratResponseDto;
import sn.immosn.backend.client.web.visite.dto.*;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

public interface DemandeVisiteService {

    DemandeVisiteResponseDto create(DemandeVisiteCreateRequestDto request, String clientEmail);

    /**
     * Crée une demande de visite pour un visiteur non authentifié.
     * Réutilise ou crée un Prospect (par email) et renvoie le token de suivi.
     */
    VisiteInviteResponseDto createInvite(VisiteInviteCreateRequestDto request);

    /**
     * Suivi public d'une visite par token de prospect — aucune authentification requise.
     * Retourne toutes les demandes de visite rattachées au prospect identifié par ce token.
     */
    SuiviVisiteResponseDto suivreParToken(String token);

    DemandeVisiteResponseDto getById(Long id, String userEmail, boolean isAdmin);

    Page<DemandeVisiteResponseDto> getClientVisites(String clientEmail, StatutDemandeVisite statut, Pageable pageable);

    Page<DemandeVisiteResponseDto> getAllVisites(StatutDemandeVisite statut, TypeTransaction typeTransaction, Pageable pageable);

    DemandeVisiteResponseDto updateStatut(Long id, UpdateStatutVisiteDto dto, String userEmail, boolean isAdmin);

    DemandeVisiteResponseDto updateDate(Long id, UpdateDateVisiteDto dto);

    DemandeVisiteResponseDto modifierParClient(Long id, UpdateDateVisiteDto dto, String clientEmail);

    // Sprint 2 — processus commercial

    /** Affecte un administrateur responsable à une visite ACCEPTEE → AFFECTEE. */
    DemandeVisiteResponseDto affecterAdmin(Long id, AffecterAdminDto dto);

    /** Demande une replanification de la date (ACCEPTEE/AFFECTEE → REPLANIFICATION_DEMANDEE). */
    DemandeVisiteResponseDto demanderReplanification(Long id, ReplanificationDto dto);

    /** Accepte la replanification : applique la date proposée et revient à l'état précédent (ACCEPTEE/AFFECTEE). */
    DemandeVisiteResponseDto accepterReplanification(Long id);

    void annuler(Long id, String clientEmail);

    /**
     * Clôture une visite ACCEPTEE.
     * SANS_SUITE : visite → CLOTUREE_SANS_SUITE, lead → ABANDONNE.
     * AVEC_CONTRAT : visite → CLOTUREE_AVEC_CONTRAT, contrat créé automatiquement, lead → CONVERTI.
     */
    ContratResponseDto cloturerVisite(Long id, CloturerVisiteDto dto);
}
