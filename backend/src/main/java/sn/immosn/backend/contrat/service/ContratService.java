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

    /**
     * Met à jour le contrat. Si request.statut() == ACTIF, seul un SUPER_ADMIN peut effectuer
     * cette transition (vérifié via callerEmail) — déclenche aussi la création du compte client
     * depuis le prospect si nécessaire.
     */
    ContratResponseDto update(Long id, ContratUpdateRequestDto request, String callerEmail);

    /**
     * Met à jour les informations du prospect lié au contrat (nom, prénom, email, téléphone).
     * Permet à l'admin de corriger/compléter les infos avant l'activation, qui déclenchera
     * la création automatique du compte client à partir de ces données.
     * Échoue si le contrat n'a pas de prospect (déjà lié à un client).
     */
    ContratResponseDto updateProspect(Long id, ContratProspectUpdateRequestDto request);

    ContratResponseDto uploadDocument(Long id, MultipartFile file);

    /**
     * Ajoute une ou plusieurs pièces jointes typées au contrat (pré-contrat, pièce d'identité, etc.).
     * Les fichiers sont déjà uploadés (Cloudinary côté frontend) — seules les URLs résultantes
     * sont enregistrées ici, associées à leur type et à l'admin qui les a ajoutées.
     */
    ContratResponseDto addDocuments(Long id, ContratDocumentCreateRequestDto request, String adminEmail);

    ContratResponseDto removeDocument(Long id, Long documentId);

    ContratResponseDto demanderResiliation(Long id, ContratActionDto dto, String clientEmail);

    ContratResponseDto demanderProlongation(Long id, ContratActionDto dto, String clientEmail);

    ContratResponseDto accepterResiliation(Long id, ContratActionDto dto);

    ContratResponseDto refuserResiliation(Long id, ContratActionDto dto);

    ContratResponseDto accepterProlongation(Long id, ContratActionDto dto);

    ContratResponseDto refuserProlongation(Long id, ContratActionDto dto);
}
