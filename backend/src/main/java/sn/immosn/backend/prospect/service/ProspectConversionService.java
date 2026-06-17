package sn.immosn.backend.prospect.service;

import sn.immosn.backend.client.web.prospect.dto.ConversionResultDto;

public interface ProspectConversionService {

    /**
     * Convertit un prospect en compte CLIENT (Sprint 3).
     *
     * <p>Crée un User CLIENT à partir des informations du prospect, génère un mot de passe
     * temporaire sécurisé (encodé BCrypt), positionne {@code compteGenereAuto} et
     * {@code motDePasseAChanger}, rattache discussions et visites du prospect au nouveau compte,
     * puis transmet les identifiants via le NotificationService.</p>
     *
     * <p>Idempotent : si le prospect est déjà converti, renvoie le compte existant sans rien recréer.</p>
     */
    ConversionResultDto convertirProspect(Long prospectId);
}
