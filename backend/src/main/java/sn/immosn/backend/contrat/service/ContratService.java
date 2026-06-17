package sn.immosn.backend.contrat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import sn.immosn.backend.client.web.contrat.dto.*;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.entity.TypeContrat;
import sn.immosn.backend.visite.data.entity.DemandeVisite;

public interface ContratService {

    ContratResponseDto create(ContratCreateRequestDto request);

    /**
     * Crée automatiquement un contrat depuis une visite clôturée.
     * Pré-remplit client, annonce, lead et montant (annonce.prix) depuis la visite.
     * L'admin ne fournit que typeContrat et, si LOCATION, dureeLocationMois.
     */
    ContratResponseDto createFromVisite(DemandeVisite visite, TypeContrat typeContrat, Integer dureeLocationMois);

    Page<ContratResponseDto> getClientContrats(String clientEmail, StatutContrat statut, Pageable pageable);

    Page<ContratResponseDto> getAllContrats(StatutContrat statut, Pageable pageable);

    ContratResponseDto getById(Long id, String userEmail, boolean isAdmin);

    ContratResponseDto update(Long id, ContratUpdateRequestDto request);

    ContratResponseDto uploadDocument(Long id, MultipartFile file);

    ContratResponseDto demanderResiliation(Long id, ContratActionDto dto, String clientEmail);

    ContratResponseDto demanderProlongation(Long id, ContratActionDto dto, String clientEmail);

    ContratResponseDto accepterResiliation(Long id, ContratActionDto dto);

    ContratResponseDto refuserResiliation(Long id, ContratActionDto dto);

    ContratResponseDto accepterProlongation(Long id, ContratActionDto dto);

    ContratResponseDto refuserProlongation(Long id, ContratActionDto dto);

    // Sprint 3 — circuit pré-contrat

    /**
     * Le client valide son pré-contrat.
     * Transition : BROUILLON ou EN_ATTENTE_VALIDATION_CLIENT → EN_ATTENTE_VALIDATION_SUPER_ADMIN.
     * Vérifie que le contrat appartient bien au client connecté ; pose valideParClientAt.
     */
    ContratResponseDto validerParClient(Long id, String clientEmail);

    /**
     * Le SUPER_ADMIN active le contrat validé par le client.
     * Transition : EN_ATTENTE_VALIDATION_SUPER_ADMIN → ACTIF ; pose valideParSuperAdminAt.
     */
    ContratResponseDto activerParSuperAdmin(Long id);
}
