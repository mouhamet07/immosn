package sn.immosn.backend.client.web.visite.dto;

import java.time.LocalDateTime;

public record RapportVisiteResponseDto(
    Long id,
    Long demandeVisiteId,
    Long auteurAdminId,
    String auteurAdminNom,
    String compteRendu,
    String documentUrl,
    boolean aboutie,
    LocalDateTime createdAt
) {}
