package sn.immosn.backend.client.web.prospect.dto;

/**
 * Résultat d'une conversion prospect → client (Sprint 3).
 * Ne contient jamais le mot de passe ; celui-ci est transmis via le NotificationService.
 */
public record ConversionResultDto(
    Long prospectId,
    Long userId,
    String email,
    String nomComplet,
    boolean compteGenereAuto,
    boolean motDePasseAChanger,
    boolean dejaConverti,
    int discussionsRattachees,
    int visitesRattachees
) {}
