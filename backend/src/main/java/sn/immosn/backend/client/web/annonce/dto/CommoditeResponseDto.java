package sn.immosn.backend.client.web.annonce.dto;

public record CommoditeResponseDto(
    Long id,
    String libelle,
    boolean isArchived
) {
}
