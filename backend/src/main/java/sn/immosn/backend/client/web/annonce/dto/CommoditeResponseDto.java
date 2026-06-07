package sn.immosn.backend.client.web.annonce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommoditeResponseDto(
    Long id,
    String libelle,
    @JsonProperty("isArchived") boolean isArchived
) {
}
