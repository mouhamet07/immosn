package sn.immosn.backend.visite.service;

import org.springframework.web.multipart.MultipartFile;
import sn.immosn.backend.client.web.visite.dto.RapportVisiteCreateRequestDto;
import sn.immosn.backend.client.web.visite.dto.RapportVisiteResponseDto;

public interface RapportVisiteService {

    /**
     * Crée le rapport de visite et fait passer la visite à RAPPORT_REDIGE.
     * @param adminEmail email de l'administrateur auteur (issu du token)
     */
    RapportVisiteResponseDto creerRapport(Long visiteId, RapportVisiteCreateRequestDto request, String adminEmail);

    /** Téléverse (ou remplace) le document associé au rapport de visite. */
    RapportVisiteResponseDto uploadDocument(Long visiteId, MultipartFile file);

    /** Consulte le rapport d'une visite. */
    RapportVisiteResponseDto consulterRapport(Long visiteId);
}
