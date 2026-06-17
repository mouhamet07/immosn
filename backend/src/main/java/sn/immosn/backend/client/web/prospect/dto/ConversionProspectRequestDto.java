package sn.immosn.backend.client.web.prospect.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Requête de conversion d'un prospect en compte CLIENT (Sprint 3).
 *
 * <p>{@code prospectId} est obligatoire. Le mot de passe est généré automatiquement côté serveur ;
 * aucun mot de passe n'est accepté dans la requête (le prospect ne passe pas par l'inscription).</p>
 */
public record ConversionProspectRequestDto(

    @NotNull(message = "L'identifiant du prospect est obligatoire")
    Long prospectId
) {}
